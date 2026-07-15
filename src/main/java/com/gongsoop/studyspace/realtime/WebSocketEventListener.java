package com.gongsoop.studyspace.realtime;

import com.gongsoop.studyspace.service.PresenceService;
import com.gongsoop.studyspace.service.StudySessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/**
 * WebSocket 연결 종료(브라우저 종료·네트워크 끊김)를 감지해 해당 학습 세션을 끊김 처리한다.
 * ws 세션 ID로 학습 세션을 역추적하며, 처리는 idempotent 하므로 중복 이벤트도 안전하다.
 */
@Component
public class WebSocketEventListener {

    private static final Logger log = LoggerFactory.getLogger(WebSocketEventListener.class);

    private final PresenceService presenceService;
    private final StudySessionService studySessionService;

    public WebSocketEventListener(
            PresenceService presenceService,
            StudySessionService studySessionService
    ) {
        this.presenceService = presenceService;
        this.studySessionService = studySessionService;
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        String wsSessionId = StompHeaderAccessor.wrap(event.getMessage()).getSessionId();
        if (wsSessionId == null) {
            return;
        }
        Long studySessionId = presenceService.resolveWsSession(wsSessionId);
        if (studySessionId == null) {
            return;
        }
        try {
            // 단일 연결 정책: 이미 새 연결(재접속/새 탭)이 현재 연결이면 이 종료는 무시한다.
            // 살아 있는 세션이 이전 연결의 종료로 끊기는 것을 방지.
            if (presenceService.isCurrentWs(studySessionId, wsSessionId)) {
                studySessionService.handleDisconnect(studySessionId);
            }
        } catch (RuntimeException e) {
            // 낙관락 경합 등은 다른 처리가 선점한 것으로 보고 무해하게 넘긴다.
            log.debug("WS 끊김 처리 경합/실패: sessionId={}", studySessionId);
        } finally {
            presenceService.unlink(wsSessionId);
        }
    }
}
