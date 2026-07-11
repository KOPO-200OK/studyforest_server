package com.gongsoop.global.security;

import io.jsonwebtoken.Claims;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * REST와 STOMP 인증 경로에서 동일한 액세스 토큰 정책을 적용한다.
 */
@Component
public class AccessTokenValidator {

    private final JwtProvider jwtProvider;
    private final TokenService tokenService;

    public AccessTokenValidator(JwtProvider jwtProvider, TokenService tokenService) {
        this.jwtProvider = jwtProvider;
        this.tokenService = tokenService;
    }

    public Optional<Claims> getUsableClaims(String token) {
        if (token == null || !jwtProvider.validateToken(token)) {
            return Optional.empty();
        }

        Claims claims = jwtProvider.parseToken(token);
        if (!"access".equals(claims.get("type", String.class))
                || tokenService.isBlacklisted(token)) {
            return Optional.empty();
        }

        return Optional.of(claims);
    }
}
