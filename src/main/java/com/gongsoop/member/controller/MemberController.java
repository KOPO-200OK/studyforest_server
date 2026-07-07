package com.gongsoop.member.controller;

import com.gongsoop.global.response.ApiResponse;
import com.gongsoop.member.service.MemberService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/members")
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> withdraw(@AuthenticationPrincipal String email) {
        memberService.withdraw(email);
        return ResponseEntity.ok(ApiResponse.success("회원탈퇴가 완료되었습니다"));
    }
}
