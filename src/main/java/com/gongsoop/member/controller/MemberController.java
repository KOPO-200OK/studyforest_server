package com.gongsoop.member.controller;

import com.gongsoop.global.response.ApiResponse;
import com.gongsoop.member.dto.request.UpdateCharacterRequest;
import com.gongsoop.member.dto.request.UpdateProfileRequest;
import com.gongsoop.member.dto.response.CharacterResponse;
import com.gongsoop.member.dto.response.MemberResponse;
import com.gongsoop.member.service.MemberService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/members")
public class MemberController {

    private final MemberService memberService;

    public MemberController(
            MemberService memberService
    ) {
        this.memberService = memberService;
    }

    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> withdraw(
            @AuthenticationPrincipal String email
    ) {
        memberService.withdraw(email);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "회원탈퇴가 완료되었습니다"
                )
        );
    }

    @PatchMapping("/me/character")
    public ApiResponse<CharacterResponse> updateCharacter(
            @Valid
            @RequestBody
            UpdateCharacterRequest request,

            @AuthenticationPrincipal
            String email
    ) {
        return ApiResponse.success(
                "캐릭터를 변경했습니다",
                memberService.updateCharacter(
                        email,
                        request
                )
        );
    }

    /**
     * 닉네임과 캐릭터를 함께 수정합니다.
     */
    @PatchMapping("/me/profile")
    public ApiResponse<MemberResponse> updateProfile(
            @Valid
            @RequestBody
            UpdateProfileRequest request,

            @AuthenticationPrincipal
            String email
    ) {
        return ApiResponse.success(
                "프로필을 변경했습니다",
                memberService.updateProfile(
                        email,
                        request
                )
        );
    }
}