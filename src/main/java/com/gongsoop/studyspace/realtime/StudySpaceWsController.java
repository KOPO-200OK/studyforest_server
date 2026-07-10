package com.gongsoop.studyspace.realtime;

import com.gongsoop.studyspace.dto.response.SessionTickResponse;
import com.gongsoop.studyspace.service.PresenceService;
import com.gongsoop.studyspace.service.StudySessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * 스터디 세션 실시간 입력 처리(STOMP). 클라이언트는 구독 직후 join으로 매핑을 확정하고,
 * 이후 주기적으로 heartbeat를 보낸다. 응답(ack)은 사용자 전용 큐로 돌려준다.
 */
@Controller
public class StudySpaceWsController {

    private static final Logger log = LoggerFactory.getLogger(StudySpaceWsController.class);

    private final StudySessionService studySessionService;
    private final PresenceService presenceService;
    private final SimpMessagingTemplate messagingTemplate;

    public StudySpaceWsController(
            StudySessionService studySessionService,
            PresenceService presenceService,
            SimpMessagingTemplate messagingTemplate
    ) {
        this.studySessionService = studySessionService;
        this.presenceService = presenceService;
        this.messagingTemplate = messagingTemplate;
    }

    /** 입장. 소유권 확인 시에만 ws↔세션 매핑을 만들어, 첫 heartbeat 이전 끊김도 역추적한다. */
    @MessageMapping("/sessions/{sessionId}/join")
    public void join(
            @DestinationVariable Long sessionId,
            Principal principal,
            SimpMessageHeaderAccessor accessor
    ) {
        String email = principal == null ? null : principal.getName();
        if (!studySessionService.isSessionOwner(sessionId, email)) {
            log.warn("소유자가 아닌 세션 join 시도 차단: sessionId={}, user={}", sessionId, email);
            return;
        }
        presenceService.linkWsSession(accessor.getSessionId(), sessionId);
    }

    @MessageMapping("/sessions/{sessionId}/heartbeat")
    public void heartbeat(
            @DestinationVariable Long sessionId,
            Principal principal,
            SimpMessageHeaderAccessor accessor
    ) {
        String email = principal == null ? null : principal.getName();
        try {
            SessionTickResponse tick = studySessionService.heartbeat(sessionId, email);
            // 매핑 보정(첫 heartbeat가 join보다 먼저 도착한 경우 등)
            presenceService.linkWsSession(accessor.getSessionId(), sessionId);
            if (email != null) {
                messagingTemplate.convertAndSendToUser(email, "/queue/session", tick);
            }
        } catch (RuntimeException e) {
            log.debug("heartbeat 처리 실패: sessionId={}, user={}, msg={}",
                    sessionId, email, e.getMessage());
        }
    }
}
