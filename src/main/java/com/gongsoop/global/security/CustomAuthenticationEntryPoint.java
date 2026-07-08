package com.gongsoop.global.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gongsoop.global.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 토큰이 없거나 유효하지 않은 요청이 인증이 필요한 API에 접근했을 때 호출된다.
 * 스프링 시큐리티 기본 응답(빈 본문 403 등) 대신 프로젝트 공통 ApiResponse 형식으로 응답한다.
 */
@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public CustomAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        response.getWriter().write(objectMapper.writeValueAsString(
                ApiResponse.failure("UNAUTHORIZED", "로그인이 필요합니다")
        ));
    }
}
