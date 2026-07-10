package com.gongsoop.studyspace.service;

import com.gongsoop.studyspace.config.StudySpaceRealtimeProperties;
import com.gongsoop.studyspace.entity.StudySession;
import com.gongsoop.studyspace.entity.StudySessionStatus;
import com.gongsoop.studyspace.repository.SeatOccupancyRepository;
import com.gongsoop.studyspace.repository.StudySessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 서버 재기동 시 유령 데이터 정리. 점유 세션엔 startupGrace 유예 창만 심어(무방송, DB 유지)
 * 첫 heartbeat로 복구되게 하고, 점유 없는 활성 세션은 즉시 종결한다.
 */
@Component
public class StudySpaceStartupCleaner {

    private static final Logger log = LoggerFactory.getLogger(StudySpaceStartupCleaner.class);

    private static final List<StudySessionStatus> ACTIVE_STATUSES = List.of(
            StudySessionStatus.RUNNING,
            StudySessionStatus.PAUSED,
            StudySessionStatus.DISCONNECTED
    );

    private final SeatOccupancyRepository seatOccupancyRepository;
    private final StudySessionRepository studySessionRepository;
    private final StudySessionService studySessionService;
    private final PresenceService presenceService;
    private final StudySpaceRealtimeProperties properties;

    public StudySpaceStartupCleaner(
            SeatOccupancyRepository seatOccupancyRepository,
            StudySessionRepository studySessionRepository,
            StudySessionService studySessionService,
            PresenceService presenceService,
            StudySpaceRealtimeProperties properties
    ) {
        this.seatOccupancyRepository = seatOccupancyRepository;
        this.studySessionRepository = studySessionRepository;
        this.studySessionService = studySessionService;
        this.presenceService = presenceService;
        this.properties = properties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void cleanUp() {
        Set<Long> occupiedSessionIds = new HashSet<>(seatOccupancyRepository.findAllSessionIds());

        int seeded = 0;
        int orphans = 0;
        for (StudySession session : studySessionRepository.findAllByStatusIn(ACTIVE_STATUSES)) {
            Long sessionId = session.getId();

            // 점유 없는 유령 활성 세션은 즉시 종결(복구 대상 아님).
            if (!occupiedSessionIds.contains(sessionId)) {
                try {
                    studySessionService.terminateOrphanSession(sessionId);
                    orphans++;
                } catch (RuntimeException e) {
                    log.debug("유령 세션 종결 실패: sessionId={}, msg={}", sessionId, e.getMessage());
                }
                continue;
            }

            // 이미 끊김이면 reconnect 스윕이 대상으로 삼으므로 재접속 창을, 그 외엔 presence를
            // grace만큼 심는다. 두 스윕이 각각 이 창을 보고 유예 동안 건너뛴다.
            if (session.getStatus() == StudySessionStatus.DISCONNECTED) {
                if (!presenceService.isReconnectWindowOpen(sessionId)) {
                    presenceService.startReconnectWindow(sessionId, properties.getStartupGrace());
                    seeded++;
                }
            } else if (!presenceService.isAlive(sessionId)) {
                presenceService.refresh(sessionId, properties.getStartupGrace());
                seeded++;
            }
        }

        log.info("스터디 공간 재기동 정리 완료: 유예 seeding={}, 유령 세션 종결={}", seeded, orphans);
    }
}
