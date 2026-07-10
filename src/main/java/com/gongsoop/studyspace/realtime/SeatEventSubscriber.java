package com.gongsoop.studyspace.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.lang.NonNull;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Redis {@code seat-events} 채널을 구독해 STOMP 구독자에게 relay 한다.
 * 채널별 topic으로 좌석 현황 변화를 push 한다: {@code /topic/channels/{channelId}/seats}.
 */
@Component
public class SeatEventSubscriber implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(SeatEventSubscriber.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    public SeatEventSubscriber(SimpMessagingTemplate messagingTemplate, ObjectMapper objectMapper) {
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onMessage(@NonNull Message message, byte[] pattern) {
        try {
            String body = new String(message.getBody(), StandardCharsets.UTF_8);
            SeatEventMessage event = objectMapper.readValue(body, SeatEventMessage.class);
            messagingTemplate.convertAndSend(
                    "/topic/channels/" + event.channelId() + "/seats", event);
        } catch (Exception e) {
            log.warn("좌석 이벤트 relay 실패", e);
        }
    }
}
