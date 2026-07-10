package com.gongsoop.studyspace.realtime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gongsoop.global.config.RedisConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 좌석 변화 이벤트를 커밋 이후에만 Redis 채널로 발행한다.
 * {@code AFTER_COMMIT}이므로 트랜잭션이 롤백되면 방송이 나가지 않는다.
 * 담긴 payload는 이미 완성된 DTO라 LAZY 엔티티를 만지지 않는다.
 */
@Component
public class SeatEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(SeatEventPublisher.class);

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public SeatEventPublisher(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSeatChanged(SeatChangedEvent event) {
        publish(event.message());
    }

    /** 트랜잭션 밖(스케줄러 개별 커밋 후 등)에서 직접 방송할 때 사용. */
    public void publish(SeatEventMessage message) {
        try {
            redis.convertAndSend(RedisConfig.SEAT_EVENTS_CHANNEL, objectMapper.writeValueAsString(message));
        } catch (JsonProcessingException e) {
            log.warn("좌석 이벤트 직렬화 실패: channelId={}, seatId={}",
                    message.channelId(), message.seatId(), e);
        }
    }
}
