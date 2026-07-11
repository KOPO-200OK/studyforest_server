package com.gongsoop.global.security;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
class TokenServiceRedisTest {

    private static final String EMAIL = "token-test@example.com";
    private static final String REFRESH_TOKEN = "refresh-token-value";
    private static final String ACCESS_TOKEN = "access-token-value";

    @Container
    private static final GenericContainer<?> REDIS = new GenericContainer<>(
            DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    private static LettuceConnectionFactory connectionFactory;
    private static StringRedisTemplate redis;
    private static TokenService tokenService;

    @BeforeAll
    static void setUp() {
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration(
                REDIS.getHost(), REDIS.getMappedPort(6379));
        connectionFactory = new LettuceConnectionFactory(configuration);
        connectionFactory.afterPropertiesSet();
        redis = new StringRedisTemplate(connectionFactory);
        redis.afterPropertiesSet();
        tokenService = new TokenService(redis);
    }

    @AfterEach
    void cleanRedis() {
        Set<String> keys = redis.keys("auth:*");
        if (keys != null && !keys.isEmpty()) {
            redis.delete(keys);
        }
    }

    @AfterAll
    static void tearDown() {
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
    }

    @Test
    void savesRefreshTokenAsHashWithTtlAndMatchesRawToken() {
        tokenService.saveRefreshToken(EMAIL, REFRESH_TOKEN, 5_000);

        String key = "auth:refresh:" + EMAIL;
        assertThat(redis.opsForValue().get(key)).isEqualTo(sha256(REFRESH_TOKEN));
        assertThat(redis.opsForValue().get(key)).isNotEqualTo(REFRESH_TOKEN);
        assertThat(redis.getExpire(key, TimeUnit.MILLISECONDS)).isBetween(1L, 5_000L);
        assertThat(tokenService.matchesRefreshToken(EMAIL, REFRESH_TOKEN)).isTrue();
        assertThat(tokenService.matchesRefreshToken(EMAIL, "another-token")).isFalse();
    }

    @Test
    void missingOrNullRefreshTokenDoesNotMatch() {
        assertThat(tokenService.matchesRefreshToken(EMAIL, REFRESH_TOKEN)).isFalse();
        assertThat(tokenService.matchesRefreshToken(EMAIL, null)).isFalse();
    }

    @Test
    void deletesRefreshToken() {
        tokenService.saveRefreshToken(EMAIL, REFRESH_TOKEN, 5_000);

        tokenService.deleteRefreshToken(EMAIL);

        assertThat(tokenService.matchesRefreshToken(EMAIL, REFRESH_TOKEN)).isFalse();
    }

    @Test
    void blacklistsHashedAccessTokenWithTtl() {
        tokenService.blacklistAccessToken(ACCESS_TOKEN, 5_000);

        String hashedKey = "auth:blacklist:" + sha256(ACCESS_TOKEN);
        assertThat(redis.opsForValue().get(hashedKey)).isEqualTo("1");
        assertThat(redis.hasKey("auth:blacklist:" + ACCESS_TOKEN)).isFalse();
        assertThat(redis.getExpire(hashedKey, TimeUnit.MILLISECONDS)).isBetween(1L, 5_000L);
        assertThat(tokenService.isBlacklisted(ACCESS_TOKEN)).isTrue();
        assertThat(tokenService.isBlacklisted("another-token")).isFalse();
        assertThat(tokenService.isBlacklisted(null)).isFalse();
    }

    @Test
    void nonPositiveTtlDoesNotBlacklistAccessToken() {
        tokenService.blacklistAccessToken(ACCESS_TOKEN, 0);
        tokenService.blacklistAccessToken("negative-ttl-token", -1);

        assertThat(tokenService.isBlacklisted(ACCESS_TOKEN)).isFalse();
        assertThat(tokenService.isBlacklisted("negative-ttl-token")).isFalse();
    }

    @Test
    void refreshTokenExpiresWithRedisTtl() throws InterruptedException {
        tokenService.saveRefreshToken(EMAIL, REFRESH_TOKEN, 150);
        assertThat(tokenService.matchesRefreshToken(EMAIL, REFRESH_TOKEN)).isTrue();

        long deadline = System.nanoTime() + Duration.ofSeconds(3).toNanos();
        while (tokenService.matchesRefreshToken(EMAIL, REFRESH_TOKEN)
                && System.nanoTime() < deadline) {
            Thread.sleep(25);
        }

        assertThat(tokenService.matchesRefreshToken(EMAIL, REFRESH_TOKEN)).isFalse();
    }

    private static String sha256(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
