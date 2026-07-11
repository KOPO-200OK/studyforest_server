package com.gongsoop.global.security;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class TokenService {

    private static final String REFRESH_PREFIX = "refresh:token:";
    private static final String BLACKLIST_PREFIX = "blacklist:access:";

    private final StringRedisTemplate redisTemplate;

    public TokenService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void saveRefreshToken(String email, String refreshToken, long ttlMs) {
        redisTemplate.opsForValue().set(REFRESH_PREFIX + email, refreshToken, ttlMs, TimeUnit.MILLISECONDS);
    }

    public String getRefreshToken(String email) {
        return redisTemplate.opsForValue().get(REFRESH_PREFIX + email);
    }

    public void deleteRefreshToken(String email) {
        redisTemplate.delete(REFRESH_PREFIX + email);
    }

    public void blacklistAccessToken(String accessToken, long ttlMs) {
        if (ttlMs > 0) {
            redisTemplate.opsForValue().set(BLACKLIST_PREFIX + accessToken, "1", ttlMs, TimeUnit.MILLISECONDS);
        }
    }

    public boolean isBlacklisted(String accessToken) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + accessToken));
    }
}
