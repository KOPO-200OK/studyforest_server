package com.gongsoop.studyspace.service;

import com.gongsoop.global.exception.BusinessException;
import com.gongsoop.member.entity.Member;
import com.gongsoop.member.repository.MemberRepository;
import com.gongsoop.studyspace.config.StudySpaceRealtimeProperties;
import com.gongsoop.studyspace.dto.request.OccupySeatRequest;
import com.gongsoop.studyspace.dto.response.SessionTickResponse;
import com.gongsoop.studyspace.dto.response.ActiveStudySessionResponse;
import com.gongsoop.studyspace.dto.response.StudySessionResponse;
import com.gongsoop.studyspace.dto.response.StudyTimeSummaryResponse;
import com.gongsoop.studyspace.entity.*;
import com.gongsoop.studyspace.realtime.SeatChangedEvent;
import com.gongsoop.studyspace.realtime.SeatEventMessage;
import com.gongsoop.studyspace.repository.SeatOccupancyRepository;
import com.gongsoop.studyspace.repository.SeatRepository;
import com.gongsoop.studyspace.repository.StudyChannelRepository;
import com.gongsoop.studyspace.repository.StudySessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Optional;

@Service
public class StudySessionService {

    private static final String DEFAULT_SUBJECT = "한국사";

    private final MemberRepository memberRepository;
    private final StudyChannelRepository studyChannelRepository;
    private final SeatRepository seatRepository;
    private final StudySessionRepository studySessionRepository;
    private final SeatOccupancyRepository seatOccupancyRepository;
    private final SeatHoldService seatHoldService;
    private final PresenceService presenceService;
    private final ApplicationEventPublisher eventPublisher;
    private final StudySpaceRealtimeProperties properties;
    private final Clock clock;

    @Autowired
    public StudySessionService(
            MemberRepository memberRepository,
            StudyChannelRepository studyChannelRepository,
            SeatRepository seatRepository,
            StudySessionRepository studySessionRepository,
            SeatOccupancyRepository seatOccupancyRepository,
            SeatHoldService seatHoldService,
            PresenceService presenceService,
            ApplicationEventPublisher eventPublisher,
            StudySpaceRealtimeProperties properties
    ) {
        this(memberRepository, studyChannelRepository, seatRepository, studySessionRepository,
                seatOccupancyRepository, seatHoldService, presenceService, eventPublisher,
                properties, Clock.systemDefaultZone());
    }

    StudySessionService(
            MemberRepository memberRepository,
            StudyChannelRepository studyChannelRepository,
            SeatRepository seatRepository,
            StudySessionRepository studySessionRepository,
            SeatOccupancyRepository seatOccupancyRepository,
            SeatHoldService seatHoldService,
            PresenceService presenceService,
            ApplicationEventPublisher eventPublisher,
            StudySpaceRealtimeProperties properties,
            Clock clock
    ) {
        this.memberRepository = memberRepository;
        this.studyChannelRepository = studyChannelRepository;
        this.seatRepository = seatRepository;
        this.studySessionRepository = studySessionRepository;
        this.seatOccupancyRepository = seatOccupancyRepository;
        this.seatHoldService = seatHoldService;
        this.presenceService = presenceService;
        this.eventPublisher = eventPublisher;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public StudySessionResponse occupySeat(
            Long studyChannelId,
            Long seatId,
            OccupySeatRequest request,
            String email
    ) {
        Member member = getCurrentMember(email);
        StudyChannel channel = getActiveChannel(studyChannelId);

        // 1차 방어선: Redis 선점(빠른 실패). 최종 확정은 아래 DB 비관락이 담당.
        Optional<String> hold = seatHoldService.tryHold(studyChannelId, seatId, member.getId());
        if (hold.isEmpty()) {
            throw new BusinessException(
                    "SEAT_ALREADY_OCCUPIED", "이미 사용 중인 좌석입니다", HttpStatus.CONFLICT);
        }

        try {
            Seat seat = seatRepository.findByIdForUpdate(seatId)
                    .filter(Seat::isActive)
                    .orElseThrow(() -> new BusinessException(
                            "SEAT_NOT_FOUND", "이용 가능한 좌석을 찾을 수 없습니다", HttpStatus.NOT_FOUND));

            if (!channel.getStudyRoom().getId().equals(seat.getStudyRoom().getId())) {
                throw new BusinessException(
                        "SEAT_NOT_IN_CHANNEL_ROOM", "선택한 채널에 속하지 않은 좌석입니다", HttpStatus.BAD_REQUEST);
            }
            if (seatOccupancyRepository.existsByMember_Id(member.getId())) {
                throw new BusinessException(
                        "MEMBER_ALREADY_SEATED", "이미 이용 중인 좌석이 있습니다", HttpStatus.CONFLICT);
            }
            if (seatOccupancyRepository.existsByStudyChannel_IdAndSeat_Id(studyChannelId, seatId)) {
                throw new BusinessException(
                        "SEAT_ALREADY_OCCUPIED", "이미 사용 중인 좌석입니다", HttpStatus.CONFLICT);
            }

            LocalDateTime now = LocalDateTime.now(clock);
            String subject = normalizeSubject(request == null ? null : request.subject());
            StudySession session = studySessionRepository.save(
                    StudySession.start(member, channel, seat, subject, now));
            seatOccupancyRepository.save(SeatOccupancy.occupy(channel, seat, member, session, now));

            presenceService.refresh(session.getId());
            publish(SeatEventMessage.Type.OCCUPIED, channel.getId(), seat, member, session, now);

            return StudySessionResponse.from(session);
        } finally {
            // 커밋 성공/실패와 무관하게 내 토큰만 해제(CAD). 실패해도 TTL로 자연 소멸.
            seatHoldService.release(studyChannelId, seatId, hold.get());
        }
    }

    @Transactional(readOnly = true)
    public Optional<ActiveStudySessionResponse> getActiveSession(String email) {
        Member member = getCurrentMember(email);
        LocalDateTime now = LocalDateTime.now(clock);
        return seatOccupancyRepository.findByMember_Id(member.getId())
                .map(occupancy -> ActiveStudySessionResponse.from(occupancy, now));
    }

    @Transactional
    public StudySessionResponse leaveSeat(Long studySessionId, String email) {
        Member member = getCurrentMember(email);
        SeatOccupancy occupancy = seatOccupancyRepository
                .findOwnedByStudySessionIdForUpdate(studySessionId, member.getId())
                .orElseThrow(() -> new BusinessException(
                        "ACTIVE_OCCUPANCY_NOT_FOUND", "현재 이용 중인 좌석을 찾을 수 없습니다", HttpStatus.NOT_FOUND));

        StudySession session = occupancy.getStudySession();
        LocalDateTime now = LocalDateTime.now(clock);
        session.completeByUser(now);
        seatOccupancyRepository.delete(occupancy);

        presenceService.clearPresence(studySessionId);
        presenceService.clearReconnect(studySessionId);
        publish(SeatEventMessage.Type.VACATED, occupancy.getStudyChannel().getId(),
                occupancy.getSeat(), member, session, now);

        return StudySessionResponse.from(session);
    }

    /** 소유권 검증 후 presence 갱신. DISCONNECTED면 재접속 복구, 정상이면 DB 쓰기는 스로틀. */
    @Transactional
    public SessionTickResponse heartbeat(Long studySessionId, String email) {
        Member member = getCurrentMember(email);
        SeatOccupancy occupancy = seatOccupancyRepository
                .findOwnedByStudySessionIdForUpdate(studySessionId, member.getId())
                .orElseThrow(() -> new BusinessException(
                        "ACTIVE_OCCUPANCY_NOT_FOUND", "현재 이용 중인 좌석을 찾을 수 없습니다", HttpStatus.NOT_FOUND));

        StudySession session = occupancy.getStudySession();
        LocalDateTime now = LocalDateTime.now(clock);
        presenceService.refresh(studySessionId);

        if (session.getStatus() == StudySessionStatus.DISCONNECTED) {
            session.reconnect(now);
            occupancy.touch(now);
            presenceService.clearReconnect(studySessionId);
            publish(SeatEventMessage.Type.RECONNECTED, occupancy.getStudyChannel().getId(),
                    occupancy.getSeat(), member, session, now);
        } else if (presenceService.tryMarkDbTouched(studySessionId)) {
            occupancy.touch(now); // 스로틀 통과 시에만 DB 반영
        }

        return SessionTickResponse.of(session, now);
    }

    @Transactional
    public SessionTickResponse pause(Long studySessionId, String email) {
        SeatOccupancy occupancy = requireOwnedOccupancy(studySessionId, email);
        StudySession session = occupancy.getStudySession();
        LocalDateTime now = LocalDateTime.now(clock);

        StudySessionStatus before = session.getStatus();
        session.pause(now, now.plus(properties.getPauseTimeout()));
        // 실제 전이일 때만 방송·갱신(이미 PAUSED면 no-op이므로 중복 이벤트 방지).
        if (session.getStatus() != before) {
            onExplicitTransition(occupancy, session, now, SeatEventMessage.Type.PAUSED);
        }
        return SessionTickResponse.of(session, now);
    }

    @Transactional
    public SessionTickResponse resume(Long studySessionId, String email) {
        SeatOccupancy occupancy = requireOwnedOccupancy(studySessionId, email);
        StudySession session = occupancy.getStudySession();
        LocalDateTime now = LocalDateTime.now(clock);

        StudySessionStatus before = session.getStatus();
        session.resume(now);
        if (session.getStatus() != before) {
            onExplicitTransition(occupancy, session, now, SeatEventMessage.Type.RESUMED);
        }
        return SessionTickResponse.of(session, now);
    }

    /** pause/resume 전이 후처리: last_seen 반영 + presence 갱신 + 방송. */
    private void onExplicitTransition(
            SeatOccupancy occupancy,
            StudySession session,
            LocalDateTime now,
            SeatEventMessage.Type type
    ) {
        occupancy.touch(now);
        presenceService.refresh(session.getId());
        publish(type, occupancy.getStudyChannel().getId(),
                occupancy.getSeat(), occupancy.getMember(), session, now);
    }

    /** WS 종료 이벤트가 즉시 호출하는 경로. 이미 DISCONNECTED/종료면 no-op. */
    @Transactional
    public void handleDisconnect(Long studySessionId) {
        disconnect(studySessionId, false);
    }

    /**
     * heartbeat 유실을 스케줄러가 뒤늦게 감지한 경로. DB lastSeen은 쓰기 스로틀만큼
     * 실제 heartbeat보다 늦을 수 있으므로 그 상한을 보정하되, 감지 시점부터 재접속
     * 10분을 새로 부여하지는 않는다.
     */
    @Transactional
    public void handleStalePresence(Long studySessionId) {
        disconnect(studySessionId, true);
    }

    private void disconnect(Long studySessionId, boolean stalePresenceDetected) {
        SeatOccupancy occupancy = seatOccupancyRepository.findByStudySessionIdForUpdate(studySessionId)
                .orElse(null);
        if (occupancy == null) {
            return;
        }
        StudySession session = occupancy.getStudySession();
        if (session.isTerminal() || session.getStatus() == StudySessionStatus.DISCONNECTED) {
            return;
        }

        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime lastSeen = occupancy.getLastSeenAt();
        LocalDateTime disconnectedAt = now;
        if (stalePresenceDetected && lastSeen != null) {
            LocalDateTime latestPossibleHeartbeat =
                    lastSeen.plus(properties.getDbLastSeenThrottle());
            disconnectedAt = latestPossibleHeartbeat.isAfter(now)
                    ? now
                    : latestPossibleHeartbeat;
        }
        LocalDateTime reconnectDeadline =
                disconnectedAt.plus(properties.getReconnectWindow());

        session.disconnect(disconnectedAt);
        occupancy.markDisconnected(disconnectedAt, reconnectDeadline);

        if (stalePresenceDetected) {
            Duration remaining = Duration.between(now, reconnectDeadline);
            if (remaining.isNegative() || remaining.isZero()) {
                presenceService.clearReconnect(studySessionId);
            } else {
                presenceService.startReconnectWindow(studySessionId, remaining);
            }
        } else {
            presenceService.startReconnectWindow(studySessionId);
        }
        presenceService.clearPresence(studySessionId);
        publish(SeatEventMessage.Type.DISCONNECTED, occupancy.getStudyChannel().getId(),
                occupancy.getSeat(), occupancy.getMember(), session, now);
    }

    /**
     * 회원의 오늘/이번 주(월요일 시작) 누적 학습 시간(초). "이번 주" 통계는 매주 월요일
     * 초기화되므로 주 시작을 항상 월요일로 고정한다. RUNNING 세션은 마지막 재개 이후
     * 아직 DB에 반영되지 않은 진행분을 더해 화면에 실시간에 가깝게 보여준다.
     */
    @Transactional(readOnly = true)
    public StudyTimeSummaryResponse getStudyTimeSummary(Long memberId) {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate today = now.toLocalDate();
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        long todaySeconds = studySessionRepository
                .sumAccumulatedSecondsByMemberAndDateRange(memberId, today, today);
        long weeklySeconds = studySessionRepository
                .sumAccumulatedSecondsByMemberAndDateRange(memberId, weekStart, today);

        long liveExtraSeconds = studySessionRepository
                .findByMember_IdAndStatus(memberId, StudySessionStatus.RUNNING)
                .map(session -> {
                    long accumulated = session.getAccumulatedSeconds() == null ? 0L : session.getAccumulatedSeconds();
                    return Math.max(0L, session.elapsedSeconds(now) - accumulated);
                })
                .orElse(0L);

        return new StudyTimeSummaryResponse(
                todaySeconds + liveExtraSeconds,
                weeklySeconds + liveExtraSeconds
        );
    }

    /** WS join 매핑 전에 해당 세션의 소유자인지 확인한다(타인 세션 매핑·오염 방지). */
    @Transactional(readOnly = true)
    public boolean isSessionOwner(Long studySessionId, String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        return memberRepository.findByEmail(email)
                .flatMap(member -> seatOccupancyRepository
                        .findByStudySession_IdAndMember_Id(studySessionId, member.getId()))
                .isPresent();
    }

    /** 재접속 제한 초과 자동 퇴실(스케줄러). */
    @Transactional
    public void autoTerminateReconnectTimeout(Long studySessionId) {
        autoTerminate(studySessionId, StudySessionEndReason.RECONNECT_TIMEOUT);
    }

    /** 일시정지 제한 초과 자동 퇴실(스케줄러). */
    @Transactional
    public void autoTerminatePauseTimeout(Long studySessionId) {
        autoTerminate(studySessionId, StudySessionEndReason.PAUSE_TIMEOUT);
    }

    private void autoTerminate(Long studySessionId, StudySessionEndReason reason) {
        SeatOccupancy occupancy = seatOccupancyRepository.findByStudySession_Id(studySessionId)
                .orElse(null);
        if (occupancy == null) {
            return;
        }
        StudySession session = occupancy.getStudySession();
        LocalDateTime now = LocalDateTime.now(clock);
        if (reason == StudySessionEndReason.PAUSE_TIMEOUT) {
            session.terminateByPauseTimeout(now);
        } else {
            session.terminateByReconnectTimeout(now);
        }
        seatOccupancyRepository.delete(occupancy);
        presenceService.clearPresence(studySessionId);
        presenceService.clearReconnect(studySessionId);
        publish(SeatEventMessage.Type.VACATED, occupancy.getStudyChannel().getId(),
                occupancy.getSeat(), occupancy.getMember(), session, now);
    }

    /** 점유 없는 유령 활성 세션 종결(재기동 정리 전용). 좌석 표시가 없어 방송하지 않는다. */
    @Transactional
    public void terminateOrphanSession(Long studySessionId) {
        StudySession session = studySessionRepository.findById(studySessionId).orElse(null);
        if (session == null || session.isTerminal()) {
            return;
        }
        if (seatOccupancyRepository.findByStudySession_Id(studySessionId).isPresent()) {
            return; // 점유가 살아 있으면 유령이 아님
        }
        session.terminateByReconnectTimeout(LocalDateTime.now(clock));
    }

    private SeatOccupancy requireOwnedOccupancy(Long studySessionId, String email) {
        Member member = getCurrentMember(email);
        return seatOccupancyRepository
                .findOwnedByStudySessionIdForUpdate(studySessionId, member.getId())
                .orElseThrow(() -> new BusinessException(
                        "ACTIVE_OCCUPANCY_NOT_FOUND", "현재 이용 중인 좌석을 찾을 수 없습니다", HttpStatus.NOT_FOUND));
    }

    private void publish(
            SeatEventMessage.Type type,
            Long channelId,
            Seat seat,
            Member member,
            StudySession session,
            LocalDateTime at
    ) {
        SeatEventMessage message = new SeatEventMessage(
                type,
                channelId,
                seat.getId(),
                seat.getSeatNo(),
                type == SeatEventMessage.Type.VACATED ? null : member.getCharacterId()
        );
        eventPublisher.publishEvent(new SeatChangedEvent(message));
    }

    private StudyChannel getActiveChannel(Long studyChannelId) {
        StudyChannel channel = studyChannelRepository.findById(studyChannelId)
                .filter(StudyChannel::isActive)
                .orElseThrow(() -> new BusinessException(
                        "STUDY_CHANNEL_NOT_FOUND", "이용 가능한 스터디 채널을 찾을 수 없습니다", HttpStatus.NOT_FOUND));
        if (!channel.getStudyRoom().isActive()) {
            throw new BusinessException(
                    "STUDY_ROOM_INACTIVE", "현재 이용할 수 없는 스터디 공간입니다", HttpStatus.CONFLICT);
        }
        return channel;
    }

    private Member getCurrentMember(String email) {
        if (email == null || email.isBlank()) {
            throw new BusinessException("UNAUTHORIZED", "로그인이 필요합니다", HttpStatus.UNAUTHORIZED);
        }
        return memberRepository.findByEmail(email)
                .filter(member -> !member.isDeleted())
                .orElseThrow(() -> new BusinessException(
                        "MEMBER_NOT_FOUND", "회원 정보를 찾을 수 없습니다", HttpStatus.NOT_FOUND));
    }

    private String normalizeSubject(String subject) {
        return subject == null || subject.isBlank() ? DEFAULT_SUBJECT : subject.trim();
    }
}
