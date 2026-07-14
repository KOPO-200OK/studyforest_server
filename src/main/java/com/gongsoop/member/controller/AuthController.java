package com.gongsoop.member.controller;

import com.gongsoop.global.response.ApiResponse;
import com.gongsoop.member.dto.request.FindEmailRequest;
import com.gongsoop.member.dto.request.LoginRequest;
import com.gongsoop.member.dto.request.RefreshTokenRequest;
import com.gongsoop.member.dto.request.ResetPasswordRequest;
import com.gongsoop.member.dto.request.SignupRequest;
import com.gongsoop.member.dto.request.SignupValidationRequest;
import com.gongsoop.member.dto.response.LoginResponse;
import com.gongsoop.member.service.MemberService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final MemberService memberService;

    public AuthController(
            MemberService memberService
    ) {
        this.memberService = memberService;
    }

    /**
     * 회원가입 첫 번째 화면의 입력값을 검증합니다.
     *
     * 회원 데이터는 저장하지 않고 다음 항목만 확인합니다.
     * - 이름
     * - 생년월일
     * - 이메일 형식
     * - 이메일 중복
     * - 비밀번호 규칙
     */
    @PostMapping("/signup/validate")
    public ResponseEntity<ApiResponse<Void>> validateSignup(
            @Valid
            @RequestBody
            SignupValidationRequest request
    ) {
        memberService.validateSignup(
                request
        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "회원가입 기본 정보 검증이 완료되었습니다"
                )
        );
    }

    /**
     * 닉네임과 캐릭터를 포함한 최종 회원가입입니다.
     */
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Void>> signup(
            @Valid
            @RequestBody
            SignupRequest request
    ) {
        memberService.signup(
                request
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "회원가입이 완료되었습니다"
                        )
                );
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid
            @RequestBody
            LoginRequest request
    ) {
        LoginResponse response =
                memberService.login(
                        request
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "로그인이 완료되었습니다",
                        response
                )
        );
    }

    @PostMapping("/find-email")
    public ResponseEntity<ApiResponse<String>> findEmail(
            @Valid
            @RequestBody
            FindEmailRequest request
    ) {
        String email =
                memberService.findEmail(
                        request
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "이메일 조회가 완료되었습니다",
                        email
                )
        );
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid
            @RequestBody
            ResetPasswordRequest request
    ) {
        memberService.resetPassword(
                request
        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "비밀번호가 재설정되었습니다"
                )
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal
            String email,

            HttpServletRequest request
    ) {
        String token =
                resolveToken(
                        request
                );

        memberService.logout(
                email,
                token
        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "로그아웃이 완료되었습니다"
                )
        );
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<String>> refresh(
            @Valid
            @RequestBody
            RefreshTokenRequest request
    ) {
        String newAccessToken =
                memberService.refresh(
                        request.refreshToken()
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "액세스 토큰이 갱신되었습니다",
                        newAccessToken
                )
        );
    }

    private String resolveToken(
            HttpServletRequest request
    ) {
        String bearer =
                request.getHeader(
                        "Authorization"
                );

        if (
                bearer != null
                        && bearer.startsWith(
                        "Bearer "
                )
        ) {
            return bearer.substring(
                    7
            );
        }

        return null;
    }
}