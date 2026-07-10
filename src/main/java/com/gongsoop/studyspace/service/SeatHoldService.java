package com.gongsoop.studyspace.service;

import com.gongsoop.studyspace.config.StudySpaceRealtimeProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 좌석 선점(soft hold) 락. SETNX로 짧게 잡아 동시 진입을 빠르게 실패시키는 1차 방어선이며,
 * 최종 확정은 DB 비관적 락이 담당한다. 해제는 토큰 compare-and-delete로만 하여, TTL 만료 후
 * 다른 요청이 잡은 락을 이전 요청이 지우는 사고를 막는다.
 */
@Service
public class SeatHoldService {

    private static final RedisScript<Long> COMPARE_AND_DELETE = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then "
                    + "return redis.call('del', KEYS[1]) "
                    + "else return 0 end",
            Long.class
    );

    private final StringRedisTemplate redis;
    private final StudySpaceRealtimeProperties properties;

    public SeatHoldService(StringRedisTemplate redis, StudySpaceRealtimeProperties properties) {
        this.redis = redis;
        this.properties = properties;
    }

    /**
     * 좌석 선점 시도. 성공하면 해제에 사용할 토큰을 반환하고, 이미 잡혀 있으면 비어 있음을 반환한다.
     */
    public Optional<String> tryHold(Long channelId, Long seatId, Long userId) {
        String token = UUID.randomUUID() + ":" + userId;
        Boolean acquired = redis.opsForValue()
                .setIfAbsent(key(channelId, seatId), token, properties.getHoldTtl());
        return Boolean.TRUE.equals(acquired) ? Optional.of(token) : Optional.empty();
    }

    /** 내 토큰일 때만 락을 해제한다. token이 null이면 아무 것도 하지 않는다. */
    public void release(Long channelId, Long seatId, String token) {
        if (token == null) {
            return;
        }
        redis.execute(COMPARE_AND_DELETE, List.of(key(channelId, seatId)), token);
    }

    private String key(Long channelId, Long seatId) {
        return "hold:seat:" + channelId + ":" + seatId;
    }
}
