package com.gongsoop.global.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AccessTokenAuthenticationTest {

    private static final String TOKEN = "test-token";

    private JwtProvider jwtProvider;
    private TokenService tokenService;
    private JwtAuthenticationFilter filter;
    private StompAuthChannelInterceptor interceptor;

    @BeforeEach
    void setUp() {
        jwtProvider = mock(JwtProvider.class);
        tokenService = mock(TokenService.class);
        AccessTokenValidator validator = new AccessTokenValidator(jwtProvider, tokenService);
        filter = new JwtAuthenticationFilter(validator);
        interceptor = new StompAuthChannelInterceptor(validator);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void restRejectsRefreshToken() throws Exception {
        stubClaims("refresh");

        filter.doFilter(requestWithBearer(), new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(tokenService, never()).isBlacklisted(TOKEN);
    }

    @Test
    void restRejectsBlacklistedAccessToken() throws Exception {
        stubClaims("access");
        when(tokenService.isBlacklisted(TOKEN)).thenReturn(true);

        filter.doFilter(requestWithBearer(), new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void stompRejectsRefreshToken() {
        stubClaims("refresh");

        assertThatThrownBy(() -> interceptor.preSend(connectMessage(), mock(MessageChannel.class)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("STOMP 연결 인증에 실패했습니다");
        verify(tokenService, never()).isBlacklisted(TOKEN);
    }

    @Test
    void stompRejectsBlacklistedAccessToken() {
        stubClaims("access");
        when(tokenService.isBlacklisted(TOKEN)).thenReturn(true);

        assertThatThrownBy(() -> interceptor.preSend(connectMessage(), mock(MessageChannel.class)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("STOMP 연결 인증에 실패했습니다");
    }

    @Test
    void restAcceptsUsableAccessToken() throws Exception {
        Claims claims = stubClaims("access");
        when(claims.getSubject()).thenReturn("user@example.com");
        when(claims.get("role", String.class)).thenReturn("USER");

        filter.doFilter(requestWithBearer(), new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName())
                .isEqualTo("user@example.com");
    }

    private Claims stubClaims(String type) {
        Claims claims = mock(Claims.class);
        when(jwtProvider.validateToken(TOKEN)).thenReturn(true);
        when(jwtProvider.parseToken(TOKEN)).thenReturn(claims);
        when(claims.get("type", String.class)).thenReturn(type);
        return claims;
    }

    private MockHttpServletRequest requestWithBearer() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + TOKEN);
        return request;
    }

    private Message<byte[]> connectMessage() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "Bearer " + TOKEN);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
