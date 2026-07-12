package com.gongsoop.studyspace.service;

import com.gongsoop.global.exception.BusinessException;
import com.gongsoop.member.entity.Member;
import com.gongsoop.member.repository.MemberRepository;
import com.gongsoop.studyspace.config.StudySpaceRealtimeProperties;
import com.gongsoop.studyspace.dto.response.SessionTickResponse;
import com.gongsoop.studyspace.entity.*;
import com.gongsoop.studyspace.realtime.SeatChangedEvent;
import com.gongsoop.studyspace.realtime.SeatEventMessage;
import com.gongsoop.studyspace.repository.SeatOccupancyRepository;
import com.gongsoop.studyspace.repository.SeatRepository;
import com.gongsoop.studyspace.repository.StudyChannelRepository;
import com.gongsoop.studyspace.repository.StudySessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * heartbeat·재접속·끊김·자동퇴실 오케스트레이션을 mock 리포지토리 + 실제 도메인 로직으로 검증.
 * DB·Redis 없이 서비스가 상태 전이·스로틀·방송·idempotency를 올바르게 조율하는지 본다.
 */
class StudySessionServiceRealtimeTest {

    // 고정 클럭: 2026-07-08T06:05:00Z == 2026-07-08T15:05:00 KST
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 8, 15, 5, 0);

    private MemberRepository memberRepository;
    private SeatOccupancyRepository occupancyRepository;
    private StudySessionRepository sessionRepository;
    private SeatHoldService seatHoldService;
    private PresenceService presenceService;
    private ApplicationEventPublisher eventPublisher;
    private StudySpaceRealtimeProperties properties;
    private StudySessionService service;

    private Member member;
    private StudyChannel channel;
    private Seat seat;

    @BeforeEach
    void setUp() {
        memberRepository = mock(MemberRepository.class);
        StudyChannelRepository channelRepository = mock(StudyChannelRepository.class);
        SeatRepository seatRepository = mock(SeatRepository.class);
        sessionRepository = mock(StudySessionRepository.class);
        occupancyRepository = mock(SeatOccupancyRepository.class);
        seatHoldService = mock(SeatHoldService.class);
        presenceService = mock(PresenceService.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        properties = new StudySpaceRealtimeProperties();
        Clock clock = Clock.fixed(Instant.parse("2026-07-08T06:05:00Z"), ZoneId.of("Asia/Seoul"));

        service = new StudySessionService(
                memberRepository, channelRepository, seatRepository, sessionRepository,
                occupancyRepository, seatHoldService, presenceService, eventPublisher,
                properties, clock);

        member = mock(Member.class);
        channel = mock(StudyChannel.class);
        seat = mock(Seat.class);
        when(member.getId()).thenReturn(7L);
        when(member.getName()).thenReturn("희주");
        when(member.isDeleted()).thenReturn(false);
        when(channel.getId()).thenReturn(3L);
        when(seat.getId()).thenReturn(10L);
        when(seat.getSeatNo()).thenReturn(1);
        when(memberRepository.findByEmail("user@test.com")).thenReturn(Optional.of(member));
    }

    private StudySession session(StudySessionStatus statusAfterStart) {
        StudySession session = StudySession.start(member, channel, seat, "한국사", NOW.minusMinutes(30));
        ReflectionTestUtils.setField(session, "id", 30L);
        return session;
    }

    private SeatOccupancy occupancyFor(StudySession session, LocalDateTime lastSeen) {
        return SeatOccupancy.occupy(channel, seat, member, session, lastSeen);
    }

    private SeatEventMessage.Type capturePublishedType() {
        ArgumentCaptor<SeatChangedEvent> captor = ArgumentCaptor.forClass(SeatChangedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        return captor.getValue().message().type();
    }

    // --- heartbeat ---

    @Test
    void heartbeatWithinThrottleWindowSkipsDbTouchButRefreshesPresence() {
        StudySession session = session(StudySessionStatus.RUNNING);
        SeatOccupancy occupancy = occupancyFor(session, NOW.minusMinutes(1));
        when(occupancyRepository.findOwnedByStudySessionIdForUpdate(30L, 7L))
                .thenReturn(Optional.of(occupancy));
        when(presenceService.tryMarkDbTouched(30L)).thenReturn(false); // 스로틀 미통과

        SessionTickResponse tick = service.heartbeat(30L, "user@test.com");

        verify(presenceService).refresh(30L);                 // presence는 항상 갱신
        assertThat(occupancy.getLastSeenAt()).isEqualTo(NOW.minusMinutes(1)); // DB touch 안 됨
        verify(eventPublisher, never()).publishEvent(any());
        assertThat(tick.status()).isEqualTo(StudySessionStatus.RUNNING);
    }

    @Test
    void heartbeatPastThrottleWindowTouchesDb() {
        StudySession session = session(StudySessionStatus.RUNNING);
        SeatOccupancy occupancy = occupancyFor(session, NOW.minusMinutes(1));
        when(occupancyRepository.findOwnedByStudySessionIdForUpdate(30L, 7L))
                .thenReturn(Optional.of(occupancy));
        when(presenceService.tryMarkDbTouched(30L)).thenReturn(true); // 스로틀 통과

        service.heartbeat(30L, "user@test.com");

        assertThat(occupancy.getLastSeenAt()).isEqualTo(NOW);  // DB touch 반영
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void heartbeatOnDisconnectedSessionReconnectsAndBroadcasts() {
        StudySession session = session(StudySessionStatus.RUNNING);
        session.disconnect(NOW.minusMinutes(5));               // DISCONNECTED로 만든다
        SeatOccupancy occupancy = occupancyFor(session, NOW.minusMinutes(5));
        occupancy.markDisconnected(NOW.minusMinutes(5), NOW.plusMinutes(5));
        when(occupancyRepository.findOwnedByStudySessionIdForUpdate(30L, 7L))
                .thenReturn(Optional.of(occupancy));

        service.heartbeat(30L, "user@test.com");

        assertThat(session.getStatus()).isEqualTo(StudySessionStatus.RUNNING); // 복구
        assertThat(occupancy.getReconnectDeadlineAt()).isNull();               // touch가 정리
        verify(presenceService).clearReconnect(30L);
        assertThat(capturePublishedType()).isEqualTo(SeatEventMessage.Type.RECONNECTED);
    }

    @Test
    void heartbeatRejectsNonOwner() {
        when(occupancyRepository.findOwnedByStudySessionIdForUpdate(30L, 7L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.heartbeat(30L, "user@test.com"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("현재 이용 중인 좌석을 찾을 수 없습니다");
    }

    // --- handleDisconnect ---

    @Test
    void handleDisconnectMarksDisconnectedAndOpensReconnectWindow() {
        StudySession session = session(StudySessionStatus.RUNNING);
        SeatOccupancy occupancy = occupancyFor(session, NOW.minusSeconds(30));
        when(occupancyRepository.findByStudySessionIdForUpdate(30L)).thenReturn(Optional.of(occupancy));

        service.handleDisconnect(30L);

        assertThat(session.getStatus()).isEqualTo(StudySessionStatus.DISCONNECTED);
        assertThat(occupancy.getDisconnectedAt()).isEqualTo(NOW);
        assertThat(occupancy.getReconnectDeadlineAt())
                .isEqualTo(NOW.plus(properties.getReconnectWindow()));
        verify(presenceService).startReconnectWindow(30L);
        verify(presenceService).clearPresence(30L);
        assertThat(capturePublishedType()).isEqualTo(SeatEventMessage.Type.DISCONNECTED);
    }

    @Test
    void handleDisconnectOnAlreadyDisconnectedIsNoOp() {
        StudySession session = session(StudySessionStatus.RUNNING);
        session.disconnect(NOW.minusMinutes(2));
        SeatOccupancy occupancy = occupancyFor(session, NOW.minusMinutes(2));
        when(occupancyRepository.findByStudySessionIdForUpdate(30L)).thenReturn(Optional.of(occupancy));

        service.handleDisconnect(30L);

        verify(eventPublisher, never()).publishEvent(any());
        verify(presenceService, never()).startReconnectWindow(anyLong());
    }

    @Test
    void handleDisconnectWithoutOccupancyIsNoOp() {
        when(occupancyRepository.findByStudySessionIdForUpdate(30L)).thenReturn(Optional.empty());

        service.handleDisconnect(30L);

        verify(eventPublisher, never()).publishEvent(any());
    }

    // --- 자동 퇴실 ---

    @Test
    void autoTerminateReconnectTimeoutReleasesSeatAndBroadcastsVacated() {
        StudySession session = session(StudySessionStatus.RUNNING);
        session.disconnect(NOW.minusMinutes(11));
        SeatOccupancy occupancy = occupancyFor(session, NOW.minusMinutes(11));
        when(occupancyRepository.findByStudySession_Id(30L)).thenReturn(Optional.of(occupancy));

        service.autoTerminateReconnectTimeout(30L);

        assertThat(session.getStatus()).isEqualTo(StudySessionStatus.AUTO_TERMINATED);
        assertThat(session.getEndReason()).isEqualTo(StudySessionEndReason.RECONNECT_TIMEOUT);
        verify(occupancyRepository).delete(occupancy);
        verify(presenceService).clearPresence(30L);
        verify(presenceService).clearReconnect(30L);
        assertThat(capturePublishedType()).isEqualTo(SeatEventMessage.Type.VACATED);
    }

    @Test
    void autoTerminatePauseTimeoutReleasesSeat() {
        StudySession session = session(StudySessionStatus.RUNNING);
        session.pause(NOW.minusHours(2), NOW.minusHours(1));
        SeatOccupancy occupancy = occupancyFor(session, NOW.minusHours(2));
        when(occupancyRepository.findByStudySession_Id(30L)).thenReturn(Optional.of(occupancy));

        service.autoTerminatePauseTimeout(30L);

        assertThat(session.getStatus()).isEqualTo(StudySessionStatus.AUTO_TERMINATED);
        assertThat(session.getEndReason()).isEqualTo(StudySessionEndReason.PAUSE_TIMEOUT);
        verify(occupancyRepository).delete(occupancy);
        assertThat(capturePublishedType()).isEqualTo(SeatEventMessage.Type.VACATED);
    }

    @Test
    void terminateOrphanSessionEndsSessionWithoutBroadcast() {
        StudySession session = session(StudySessionStatus.RUNNING);
        when(sessionRepository.findById(30L)).thenReturn(Optional.of(session));
        when(occupancyRepository.findByStudySession_Id(30L)).thenReturn(Optional.empty());

        service.terminateOrphanSession(30L);

        assertThat(session.isTerminal()).isTrue();
        verify(eventPublisher, never()).publishEvent(any()); // 좌석 표시 없음 → 무방송
    }

    // --- pause / resume ---

    @Test
    void pauseSetsDeadlineRefreshesPresenceTouchesAndBroadcasts() {
        StudySession session = session(StudySessionStatus.RUNNING);
        SeatOccupancy occupancy = occupancyFor(session, NOW.minusMinutes(2));
        when(occupancyRepository.findOwnedByStudySessionIdForUpdate(30L, 7L))
                .thenReturn(Optional.of(occupancy));

        SessionTickResponse tick = service.pause(30L, "user@test.com");

        assertThat(session.getStatus()).isEqualTo(StudySessionStatus.PAUSED);
        assertThat(session.getPauseDeadlineAt()).isEqualTo(NOW.plus(properties.getPauseTimeout()));
        assertThat(tick.status()).isEqualTo(StudySessionStatus.PAUSED);
        // 명시적 전이 → last_seen 반영 + presence 갱신 + 방송
        assertThat(occupancy.getLastSeenAt()).isEqualTo(NOW);
        verify(presenceService).refresh(30L);
        assertThat(capturePublishedType()).isEqualTo(SeatEventMessage.Type.PAUSED);
    }

    @Test
    void resumeReturnsToRunningAndBroadcasts() {
        StudySession session = session(StudySessionStatus.RUNNING);
        session.pause(NOW.minusMinutes(3), NOW.plusHours(1));
        SeatOccupancy occupancy = occupancyFor(session, NOW.minusMinutes(2));
        when(occupancyRepository.findOwnedByStudySessionIdForUpdate(30L, 7L))
                .thenReturn(Optional.of(occupancy));

        service.resume(30L, "user@test.com");

        assertThat(session.getStatus()).isEqualTo(StudySessionStatus.RUNNING);
        assertThat(occupancy.getLastSeenAt()).isEqualTo(NOW);
        verify(presenceService).refresh(30L);
        assertThat(capturePublishedType()).isEqualTo(SeatEventMessage.Type.RESUMED);
    }

    @Test
    void pauseOnAlreadyPausedIsNoOpAndDoesNotBroadcast() {
        StudySession session = session(StudySessionStatus.RUNNING);
        session.pause(NOW.minusMinutes(5), NOW.plusHours(1)); // 이미 PAUSED
        SeatOccupancy occupancy = occupancyFor(session, NOW.minusMinutes(5));
        when(occupancyRepository.findOwnedByStudySessionIdForUpdate(30L, 7L))
                .thenReturn(Optional.of(occupancy));

        service.pause(30L, "user@test.com");

        verify(eventPublisher, never()).publishEvent(any());   // 중복 방송 없음
        verify(presenceService, never()).refresh(anyLong());
        assertThat(occupancy.getLastSeenAt()).isEqualTo(NOW.minusMinutes(5)); // touch 안 함
    }

    @Test
    void resumeOnRunningIsNoOpAndDoesNotBroadcast() {
        StudySession session = session(StudySessionStatus.RUNNING); // 이미 RUNNING
        SeatOccupancy occupancy = occupancyFor(session, NOW.minusMinutes(5));
        when(occupancyRepository.findOwnedByStudySessionIdForUpdate(30L, 7L))
                .thenReturn(Optional.of(occupancy));

        service.resume(30L, "user@test.com");

        verify(eventPublisher, never()).publishEvent(any());
        verify(presenceService, never()).refresh(anyLong());
    }

    // --- 선점 락 ---

    @Test
    void occupyFailsFastWhenHoldNotAcquiredAndNeverTouchesDb() {
        StudyChannelRepository channelRepository =
                (StudyChannelRepository) ReflectionTestUtils.getField(service, "studyChannelRepository");
        when(channelRepository.findById(3L)).thenReturn(Optional.of(channel));
        when(channel.isActive()).thenReturn(true);
        StudyRoom room = mock(StudyRoom.class);
        when(room.isActive()).thenReturn(true);
        when(channel.getStudyRoom()).thenReturn(room);
        when(seatHoldService.tryHold(3L, 10L, 7L)).thenReturn(Optional.empty()); // 선점 실패

        assertThatThrownBy(() -> service.occupySeat(3L, 10L, null, "user@test.com"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("이미 사용 중인 좌석입니다");

        SeatRepository seatRepository =
                (SeatRepository) ReflectionTestUtils.getField(service, "seatRepository");
        verify(seatRepository, never()).findByIdForUpdate(anyLong()); // DB 진입 차단
    }
}
