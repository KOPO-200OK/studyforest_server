package com.gongsoop.studyspace.service;

import com.gongsoop.studyspace.config.StudySpaceRealtimeProperties;
import com.gongsoop.studyspace.entity.StudySession;
import com.gongsoop.studyspace.entity.StudySessionStatus;
import com.gongsoop.studyspace.repository.SeatOccupancyRepository;
import com.gongsoop.studyspace.repository.StudySessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 유지보수 스윕이 만료 대상만 골라 자동 퇴실/끊김 처리하고, 개별 항목의 예외가 전체 스윕을
 * 막지 않는지 검증한다.
 */
class SeatMaintenanceServiceTest {

    private SeatOccupancyRepository occupancyRepository;
    private StudySessionRepository sessionRepository;
    private StudySessionService studySessionService;
    private PresenceService presenceService;
    private SeatMaintenanceService maintenance;

    @BeforeEach
    void setUp() {
        occupancyRepository = mock(SeatOccupancyRepository.class);
        sessionRepository = mock(StudySessionRepository.class);
        studySessionService = mock(StudySessionService.class);
        presenceService = mock(PresenceService.class);
        StudySpaceRealtimeProperties properties = new StudySpaceRealtimeProperties();
        Clock clock = Clock.fixed(Instant.parse("2026-07-08T06:05:00Z"), ZoneId.of("Asia/Seoul"));

        maintenance = new SeatMaintenanceService(
                occupancyRepository, sessionRepository, studySessionService,
                presenceService, properties, clock);
    }

    @Test
    void reconnectSweepSkipsSessionsWhoseWindowIsStillOpen() {
        when(occupancyRepository.findSessionIdsWithReconnectDeadlineBefore(any()))
                .thenReturn(List.of(1L, 2L));
        when(presenceService.isReconnectWindowOpen(1L)).thenReturn(true);  // 아직 대기 중 → 보류
        when(presenceService.isReconnectWindowOpen(2L)).thenReturn(false); // 만료 → 퇴실

        maintenance.reconnectTimeoutSweep();

        verify(studySessionService, never()).autoTerminateReconnectTimeout(1L);
        verify(studySessionService).autoTerminateReconnectTimeout(2L);
    }

    @Test
    void reconnectSweepIsolatesPerItemFailure() {
        when(occupancyRepository.findSessionIdsWithReconnectDeadlineBefore(any()))
                .thenReturn(List.of(1L, 2L));
        when(presenceService.isReconnectWindowOpen(anyLong())).thenReturn(false);
        doThrow(new RuntimeException("낙관락 경합")).when(studySessionService)
                .autoTerminateReconnectTimeout(1L);

        maintenance.reconnectTimeoutSweep();

        // 1L이 터져도 2L은 처리돼야 한다.
        verify(studySessionService).autoTerminateReconnectTimeout(2L);
    }

    @Test
    void pauseSweepTerminatesEachExpiredPausedSession() {
        when(sessionRepository.findAllByStatusAndPauseDeadlineAtBefore(eq(StudySessionStatus.PAUSED), any()))
                .thenReturn(List.of(sessionWithId(5L), sessionWithId(6L)));

        maintenance.pauseTimeoutSweep();

        verify(studySessionService).autoTerminatePauseTimeout(5L);
        verify(studySessionService).autoTerminatePauseTimeout(6L);
    }

    @Test
    void presenceSweepDisconnectsOnlyDeadSessions() {
        when(occupancyRepository.findStaleSessionIds(any())).thenReturn(List.of(3L, 4L));
        when(presenceService.isAlive(3L)).thenReturn(true);   // 아직 살아 있음 → 유지
        when(presenceService.isAlive(4L)).thenReturn(false);  // 죽음 → 끊김 처리

        maintenance.presenceSweep();

        verify(studySessionService, never()).handleStalePresence(3L);
        verify(studySessionService).handleStalePresence(4L);
    }

    private StudySession sessionWithId(Long id) {
        StudySession session = StudySession.start(
                null, null, null, "한국사", LocalDateTime.of(2026, 7, 8, 13, 0));
        ReflectionTestUtils.setField(session, "id", id);
        return session;
    }
}
