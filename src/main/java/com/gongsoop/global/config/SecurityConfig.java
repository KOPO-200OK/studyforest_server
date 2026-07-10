package com.gongsoop.global.config;

import org.springframework.http.HttpMethod;
import com.gongsoop.global.security.CustomAccessDeniedHandler;
import com.gongsoop.global.security.CustomAuthenticationEntryPoint;
import com.gongsoop.global.security.JwtAuthenticationFilter;
import com.gongsoop.global.security.JwtProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    private final JwtProvider jwtProvider;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    private final CustomAccessDeniedHandler accessDeniedHandler;

    public SecurityConfig(
            JwtProvider jwtProvider,
            CustomAuthenticationEntryPoint authenticationEntryPoint,
            CustomAccessDeniedHandler accessDeniedHandler
    ) {
        this.jwtProvider = jwtProvider;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/api/v1/health").permitAll()

                        // WebSocket 핸드셰이크는 허용, 실제 인증은 STOMP CONNECT에서 검사
                        .requestMatchers("/ws-studyspace/**").permitAll()

                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/v3/api-docs.yaml"
                        ).permitAll()

                        // 공지사항: 사용자는 조회만 가능
                        .requestMatchers(HttpMethod.GET, "/api/v1/notices/**").permitAll()

                        // 장원급제: 승인된 목록은 공개 조회 가능, 신청/내역은 로그인 필요
                        .requestMatchers(HttpMethod.GET, "/api/v1/jangwon/applications/me").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/jangwon/applications").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/jangwon/**").permitAll()

                        // 관리자 API는 관리자만 접근 가능
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

                        // 문의하기: 공개 목록은 누구나, 나머지는 로그인 필요
                        .requestMatchers(HttpMethod.GET, "/api/v1/inquiries/public").permitAll()
                        .requestMatchers("/api/v1/inquiries/**").authenticated()

                        // 로그인 필요 API
                        .requestMatchers("/api/v1/mock-exams/**").authenticated()
                        .requestMatchers("/api/v1/study/**").authenticated()
                        .requestMatchers("/api/v1/ai/**").authenticated()
                        .requestMatchers("/api/v1/dashboard/**").authenticated()
                        .requestMatchers("/api/v1/todos/**").authenticated()
                        .requestMatchers("/api/v1/questions/**").authenticated()
                        .requestMatchers("/api/v1/members/**").authenticated()

                        .anyRequest().authenticated()
                )
                .addFilterBefore(new JwtAuthenticationFilter(jwtProvider), UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(List.of(
                "http://localhost:5173",
                "http://127.0.0.1:5173",
                "http://localhost:3000",
                "http://127.0.0.1:3000"
        ));

        configuration.setAllowedMethods(List.of(
                "GET",
                "POST",
                "PUT",
                "PATCH",
                "DELETE",
                "OPTIONS"
        ));

        configuration.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type"
        ));

        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
