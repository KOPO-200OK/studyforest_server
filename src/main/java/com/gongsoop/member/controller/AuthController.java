package com.gongsoop.member.controller;

import com.gongsoop.global.response.ApiResponse;
import com.gongsoop.member.dto.request.FindEmailRequest;
import com.gongsoop.member.dto.request.LoginRequest;
import com.gongsoop.member.dto.request.ResetPasswordRequest;
import com.gongsoop.member.dto.request.SignupRequest;
import com.gongsoop.member.dto.response.LoginResponse;
import com.gongsoop.member.service.MemberService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final MemberService memberService;

    public AuthController(MemberService memberService) {
        this.memberService = memberService;
    }

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Void>> signup(@Valid @RequestBody SignupRequest request) {
        memberService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("회원가입이 완료되었습니다"));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = memberService.login(request);
        return ResponseEntity.ok(ApiResponse.success("로그인이 완료되었습니다", response));
    }

    @PostMapping("/find-email")
    public ResponseEntity<ApiResponse<String>> findEmail(@Valid @RequestBody FindEmailRequest request) {
        String email = memberService.findEmail(request);
        return ResponseEntity.ok(ApiResponse.success("이메일 조회가 완료되었습니다", email));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        memberService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success("비밀번호가 재설정되었습니다"));
    }
}
