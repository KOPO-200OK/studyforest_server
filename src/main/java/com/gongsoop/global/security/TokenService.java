package com.gongsoop.global.security;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Service
public class TokenService {

    private static final String REFRESH_PREFIX = "auth:refresh:";
    private static final String BLACKLIST_PREFIX = "auth:blacklist:";

    private final StringRedisTemplate redisTemplate;

    public TokenService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void saveRefreshToken(String email, String refreshToken, long ttlMs) {
        redisTemplate.opsForValue().set(
                REFRESH_PREFIX + email,
                hashToken(refreshToken),
                ttlMs,
                TimeUnit.MILLISECONDS
        );
    }

    public boolean matchesRefreshToken(String email, String refreshToken) {
        if (refreshToken == null) {
            return false;
        }

        String storedHash = redisTemplate.opsForValue().get(REFRESH_PREFIX + email);
        if (storedHash == null) {
            return false;
        }

        byte[] storedBytes = storedHash.getBytes(StandardCharsets.UTF_8);
        byte[] presentedBytes = hashToken(refreshToken).getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(storedBytes, presentedBytes);
    }

    public void deleteRefreshToken(String email) {
        redisTemplate.delete(REFRESH_PREFIX + email);
    }

    public void blacklistAccessToken(String accessToken, long ttlMs) {
        if (ttlMs > 0) {
            redisTemplate.opsForValue().set(
                    BLACKLIST_PREFIX + hashToken(accessToken),
                    "1",
                    ttlMs,
                    TimeUnit.MILLISECONDS
            );
        }
    }

    public boolean isBlacklisted(String accessToken) {
        return accessToken != null
                && Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + hashToken(accessToken)));
    }

    private String hashToken(String token) {
        Objects.requireNonNull(token, "token must not be null");
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is not available", e);
        }
    }
}
