package com.gongsoop.studyspace.service;

import com.gongsoop.studyspace.config.StudySpaceRealtimeProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * 좌석 선점 락을 실제 Redis에 붙여 검증한다(Oracle 불필요). 로컬/CI에 Redis가 없으면
 * 테스트를 건너뛴다. 핵심은 토큰 compare-and-delete 안전장치다.
 */
class SeatHoldServiceRedisTest {

    private LettuceConnectionFactory connectionFactory;
    private StringRedisTemplate redis;
    private SeatHoldService seatHoldService;

    private static final Long CHANNEL = 999L;
    private static final Long SEAT = 8888L;

    @BeforeEach
    void setUp() {
        connectionFactory = new LettuceConnectionFactory(
                new RedisStandaloneConfiguration("localhost", 6379));
        connectionFactory.afterPropertiesSet();
        assumeTrue(redisReachable(), "로컬 Redis가 없어 SeatHoldService 통합 테스트를 건너뜁니다");

        redis = new StringRedisTemplate(connectionFactory);
        StudySpaceRealtimeProperties properties = new StudySpaceRealtimeProperties();
        seatHoldService = new SeatHoldService(redis, properties);
        redis.delete(key());
    }

    @AfterEach
    void tearDown() {
        if (redis != null) {
            redis.delete(key());
        }
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
    }

    @Test
    void firstHolderWinsAndSecondFailsFast() {
        Optional<String> first = seatHoldService.tryHold(CHANNEL, SEAT, 1L);
        Optional<String> second = seatHoldService.tryHold(CHANNEL, SEAT, 2L);

        assertThat(first).isPresent();
        assertThat(second).isEmpty();
    }

    @Test
    void releaseWithOwnTokenFreesTheSeat() {
        String token = seatHoldService.tryHold(CHANNEL, SEAT, 1L).orElseThrow();
        seatHoldService.release(CHANNEL, SEAT, token);

        assertThat(seatHoldService.tryHold(CHANNEL, SEAT, 2L)).isPresent();
    }

    /** 선점 계층의 핵심 안전장치: 만료 후 남이 잡은 락을 이전 요청이 지우면 안 된다. */
    @Test
    void releaseOldTokenMustNotDeleteNewToken() {
        String oldToken = seatHoldService.tryHold(CHANNEL, SEAT, 1L).orElseThrow();

        // TTL 만료를 시뮬레이션: 키를 강제로 없앤다.
        redis.delete(key());

        // 다른 요청이 같은 좌석을 재선점.
        String newToken = seatHoldService.tryHold(CHANNEL, SEAT, 2L).orElseThrow();
        assertThat(newToken).isNotEqualTo(oldToken);

        // 이전 요청이 뒤늦게 release → 남의 락을 지워선 안 된다.
        seatHoldService.release(CHANNEL, SEAT, oldToken);

        // 새 락은 여전히 살아 있어야 하므로 제3자는 선점 실패.
        assertThat(seatHoldService.tryHold(CHANNEL, SEAT, 3L)).isEmpty();

        // 주인이 자기 토큰으로 풀면 정상 해제.
        seatHoldService.release(CHANNEL, SEAT, newToken);
        assertThat(seatHoldService.tryHold(CHANNEL, SEAT, 3L)).isPresent();
    }

    @Test
    void nullTokenReleaseIsNoOp() {
        String token = seatHoldService.tryHold(CHANNEL, SEAT, 1L).orElseThrow();
        seatHoldService.release(CHANNEL, SEAT, null); // 아무 것도 하지 않아야 함

        assertThat(seatHoldService.tryHold(CHANNEL, SEAT, 2L)).isEmpty(); // 여전히 점유 중
        seatHoldService.release(CHANNEL, SEAT, token);
    }

    private boolean redisReachable() {
        try {
            connectionFactory.getConnection().ping();
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private String key() {
        return "hold:seat:" + CHANNEL + ":" + SEAT;
    }
}
