package com.gongsoop.studyspace.realtime;

import com.gongsoop.studyspace.service.PresenceService;
import com.gongsoop.studyspace.service.StudySessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.security.Principal;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class StudySpaceWsControllerTest {

    private StudySessionService studySessionService;
    private PresenceService presenceService;
    private StudySpaceWsController controller;
    private Principal principal;

    @BeforeEach
    void setUp() {
        studySessionService = mock(StudySessionService.class);
        presenceService = mock(PresenceService.class);
        controller = new StudySpaceWsController(
                studySessionService,
                presenceService,
                mock(SimpMessagingTemplate.class)
        );
        principal = () -> "member@example.com";
    }

    @Test
    void joinWithoutStompSessionIdIsIgnored() {
        controller.join(10L, principal, SimpMessageHeaderAccessor.create());

        verifyNoInteractions(studySessionService, presenceService);
    }

    @Test
    void heartbeatWithoutStompSessionIdIsIgnored() {
        controller.heartbeat(10L, principal, SimpMessageHeaderAccessor.create());

        verify(studySessionService, never()).heartbeat(10L, "member@example.com");
        verifyNoInteractions(presenceService);
    }
}
