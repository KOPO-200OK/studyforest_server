package com.gongsoop.studyspace.service;

import com.gongsoop.studyspace.config.StudySpaceRealtimeProperties;
import com.gongsoop.studyspace.entity.StudySession;
import com.gongsoop.studyspace.repository.SeatOccupancyRepository;
import com.gongsoop.studyspace.repository.StudySessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * 재기동 정리의 grace 우선·무방송 정책 검증. 특히 이미 DISCONNECTED인 점유 세션에도 유예가
 * 적용돼(재접속 창 seed), 첫 스윕에서 즉시 자동퇴실되지 않아야 한다.
 */
class StudySpaceStartupCleanerTest {

    private SeatOccupancyRepository occupancyRepository;
    private StudySessionRepository sessionRepository;
    private StudySessionService studySessionService;
    private PresenceService presenceService;
    private StudySpaceRealtimeProperties properties;
    private StudySpaceStartupCleaner cleaner;

    @BeforeEach
    void setUp() {
        occupancyRepository = mock(SeatOccupancyRepository.class);
        sessionRepository = mock(StudySessionRepository.class);
        studySessionService = mock(StudySessionService.class);
        presenceService = mock(PresenceService.class);
        properties = new StudySpaceRealtimeProperties();
        cleaner = new StudySpaceStartupCleaner(
                occupancyRepository, sessionRepository, studySessionService, presenceService, properties);
    }

    private StudySession sessionWithId(Long id) {
        StudySession session = StudySession.start(
                null, null, null, "한국사", LocalDateTime.of(2026, 7, 8, 13, 0));
        ReflectionTestUtils.setField(session, "id", id);
        return session;
    }

    @Test
    void seedsReconnectWindowForDisconnectedOccupiedSession() {
        StudySession disconnected = sessionWithId(1L);
        disconnected.disconnect(LocalDateTime.of(2026, 7, 8, 13, 5)); // DISCONNECTED
        when(occupancyRepository.findAllSessionIds()).thenReturn(List.of(1L));
        when(sessionRepository.findAllByStatusIn(any())).thenReturn(List.of(disconnected));
        when(presenceService.isReconnectWindowOpen(1L)).thenReturn(false);

        cleaner.cleanUp();

        verify(presenceService).startReconnectWindow(1L, properties.getStartupGrace());
        verify(studySessionService, never()).terminateOrphanSession(anyLong());
    }

    @Test
    void seedsGracePresenceForRunningOccupiedSessionWhenPresenceDead() {
        StudySession running = sessionWithId(2L); // RUNNING
        when(occupancyRepository.findAllSessionIds()).thenReturn(List.of(2L));
        when(sessionRepository.findAllByStatusIn(any())).thenReturn(List.of(running));
        when(presenceService.isAlive(2L)).thenReturn(false);

        cleaner.cleanUp();

        verify(presenceService).refresh(2L, properties.getStartupGrace());
        verify(presenceService, never()).startReconnectWindow(anyLong(), any());
    }

    @Test
    void terminatesOrphanActiveSessionWithoutOccupancy() {
        StudySession orphan = sessionWithId(3L);
        when(occupancyRepository.findAllSessionIds()).thenReturn(List.of()); // 점유 없음
        when(sessionRepository.findAllByStatusIn(any())).thenReturn(List.of(orphan));

        cleaner.cleanUp();

        verify(studySessionService).terminateOrphanSession(3L);
    }
}
