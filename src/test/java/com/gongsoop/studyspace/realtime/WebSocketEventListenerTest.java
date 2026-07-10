package com.gongsoop.studyspace.realtime;

import com.gongsoop.studyspace.service.PresenceService;
import com.gongsoop.studyspace.service.StudySessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import static org.mockito.Mockito.*;

/**
 * WS 종료 이벤트 처리의 단일 연결 정책 검증: 종료된 연결이 세션의 현재 연결일 때만 끊김
 * 처리하고, 재접속/새 탭으로 대체된 이전 연결의 종료는 무시한다.
 */
class WebSocketEventListenerTest {

    private PresenceService presenceService;
    private StudySessionService studySessionService;
    private WebSocketEventListener listener;

    @BeforeEach
    void setUp() {
        presenceService = mock(PresenceService.class);
        studySessionService = mock(StudySessionService.class);
        listener = new WebSocketEventListener(presenceService, studySessionService);
    }

    private SessionDisconnectEvent disconnectEvent(String wsSessionId) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.DISCONNECT);
        accessor.setSessionId(wsSessionId);
        Message<byte[]> message =
                MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        return new SessionDisconnectEvent(this, message, wsSessionId, CloseStatus.NORMAL);
    }

    @Test
    void disconnectOfCurrentConnectionTriggersHandleDisconnect() {
        when(presenceService.resolveWsSession("ws-1")).thenReturn(30L);
        when(presenceService.isCurrentWs(30L, "ws-1")).thenReturn(true);

        listener.onDisconnect(disconnectEvent("ws-1"));

        verify(studySessionService).handleDisconnect(30L);
        verify(presenceService).unlink("ws-1");
    }

    @Test
    void disconnectOfStaleConnectionIsIgnoredButUnlinked() {
        when(presenceService.resolveWsSession("ws-old")).thenReturn(30L);
        when(presenceService.isCurrentWs(30L, "ws-old")).thenReturn(false); // 이미 새 연결로 대체됨

        listener.onDisconnect(disconnectEvent("ws-old"));

        verify(studySessionService, never()).handleDisconnect(anyLong()); // 살아있는 세션 보호
        verify(presenceService).unlink("ws-old");
    }

    @Test
    void disconnectWithoutMappingIsNoOp() {
        when(presenceService.resolveWsSession("ws-x")).thenReturn(null);

        listener.onDisconnect(disconnectEvent("ws-x"));

        verify(studySessionService, never()).handleDisconnect(anyLong());
        verify(presenceService, never()).unlink(anyString());
    }
}
