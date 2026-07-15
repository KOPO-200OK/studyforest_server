package com.gongsoop.studyspace.service;

import com.gongsoop.studyspace.config.StudySpaceRealtimeProperties;
import com.gongsoop.studyspace.entity.StudySession;
import com.gongsoop.studyspace.entity.StudySessionStatus;
import com.gongsoop.studyspace.repository.SeatOccupancyRepository;
import com.gongsoop.studyspace.repository.StudySessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 유지보수 스케줄러. 재접속·일시정지 제한 초과 좌석을 자동 정리하고, WS 종료 이벤트가
 * 유실된 끊김을 늦게라도 감지한다. 각 항목은 개별 트랜잭션으로 처리하며 한 항목의 예외가
 * 전체 스윕을 막지 않도록 격리한다.
 */
@Service
public class SeatMaintenanceService {

    private static final Logger log = LoggerFactory.getLogger(SeatMaintenanceService.class);

    private final SeatOccupancyRepository seatOccupancyRepository;
    private final StudySessionRepository studySessionRepository;
    private final StudySessionService studySessionService;
    private final PresenceService presenceService;
    private final StudySpaceRealtimeProperties properties;
    private final Clock clock;

    @Autowired
    public SeatMaintenanceService(
            SeatOccupancyRepository seatOccupancyRepository,
            StudySessionRepository studySessionRepository,
            StudySessionService studySessionService,
            PresenceService presenceService,
            StudySpaceRealtimeProperties properties
    ) {
        this(seatOccupancyRepository, studySessionRepository, studySessionService,
                presenceService, properties, Clock.systemDefaultZone());
    }

    SeatMaintenanceService(
            SeatOccupancyRepository seatOccupancyRepository,
            StudySessionRepository studySessionRepository,
            StudySessionService studySessionService,
            PresenceService presenceService,
            StudySpaceRealtimeProperties properties,
            Clock clock
    ) {
        this.seatOccupancyRepository = seatOccupancyRepository;
        this.studySessionRepository = studySessionRepository;
        this.studySessionService = studySessionService;
        this.presenceService = presenceService;
        this.properties = properties;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${studyspace.realtime.sweep-interval:30s}")
    public void sweep() {
        reconnectTimeoutSweep();
        pauseTimeoutSweep();
        presenceSweep();
    }

    /** 재접속 제한 초과: Redis 대기창이 만료됐고 DB 기한도 지난 점유를 자동 퇴실. */
    void reconnectTimeoutSweep() {
        LocalDateTime now = LocalDateTime.now(clock);
        List<Long> candidates =
                seatOccupancyRepository.findSessionIdsWithReconnectDeadlineBefore(now);
        for (Long sessionId : candidates) {
            if (presenceService.isReconnectWindowOpen(sessionId)) {
                continue; // Redis 1차 판정: 아직 대기창 유효 → 보류
            }
            runSafely(() -> studySessionService.autoTerminateReconnectTimeout(sessionId));
        }
    }

    /** 일시정지 제한 초과: PAUSED이면서 pause 기한이 지난 세션을 자동 퇴실. */
    void pauseTimeoutSweep() {
        LocalDateTime now = LocalDateTime.now(clock);
        List<StudySession> candidates = studySessionRepository
                .findAllByStatusAndPauseDeadlineAtBefore(StudySessionStatus.PAUSED, now);
        for (StudySession session : candidates) {
            Long sessionId = session.getId();
            runSafely(() -> studySessionService.autoTerminatePauseTimeout(sessionId));
        }
    }

    /** WS 종료 이벤트 유실 대비: heartbeat가 끊긴 지 오래고 presence도 죽은 점유를 끊김 처리. */
    void presenceSweep() {
        LocalDateTime threshold =
                LocalDateTime.now(clock).minus(properties.getPresenceStaleThreshold());
        List<Long> candidates = seatOccupancyRepository.findStaleSessionIds(threshold);
        for (Long sessionId : candidates) {
            if (presenceService.isAlive(sessionId)) {
                continue; // 아직 살아 있으면 끊김 아님
            }
            runSafely(() -> studySessionService.handleStalePresence(sessionId));
        }
    }

    private void runSafely(Runnable action) {
        try {
            action.run();
        } catch (RuntimeException e) {
            // 낙관락 경합 등 다른 처리가 선점한 경우 포함 — 다음 주기에 다시 시도된다.
            log.debug("스케줄 처리 건너뜀");
        }
    }
}
