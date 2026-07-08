package com.gongsoop.admin.controller;


import com.gongsoop.admin.dto.request.AdminQuestionCreateRequest;
import com.gongsoop.admin.dto.request.AdminQuestionUpdateRequest;
import com.gongsoop.admin.dto.response.AdminQuestionDetailResponse;
import com.gongsoop.admin.dto.request.UpdateMemberDeleteStatusRequest;
import com.gongsoop.admin.dto.request.UpdateMemberRoleRequest;
import com.gongsoop.admin.dto.response.AdminDashboardResponse;
import com.gongsoop.admin.dto.response.AdminMemberDetailResponse;
import com.gongsoop.admin.dto.response.AdminMemberSummaryResponse;
import com.gongsoop.admin.dto.response.AdminQuestionSummaryResponse;
import com.gongsoop.admin.service.AdminService;
import com.gongsoop.global.response.ApiResponse;
import com.gongsoop.question.dto.response.PageResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/dashboard")
    public ApiResponse<AdminDashboardResponse> getDashboard(
            @AuthenticationPrincipal String email
    ) {
        return ApiResponse.success(
                "관리자 대시보드 정보를 조회했습니다",
                adminService.getDashboard(email)
        );
    }

    @GetMapping("/members")
    public ApiResponse<PageResponse<AdminMemberSummaryResponse>> getMembers(
            @AuthenticationPrincipal String email,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String userRole,
            @RequestParam(required = false) Boolean isDeleted
    ) {
        return ApiResponse.success(
                "회원 목록을 조회했습니다",
                adminService.getMembers(email, page, size, keyword, userRole, isDeleted)
        );
    }

    @GetMapping("/members/{memberId}")
    public ApiResponse<AdminMemberDetailResponse> getMemberDetail(
            @AuthenticationPrincipal String email,
            @PathVariable Long memberId
    ) {
        return ApiResponse.success(
                "회원 상세 정보를 조회했습니다",
                adminService.getMemberDetail(email, memberId)
        );
    }

    @PatchMapping("/members/{memberId}/role")
    public ApiResponse<AdminMemberSummaryResponse> updateMemberRole(
            @AuthenticationPrincipal String email,
            @PathVariable Long memberId,
            @Valid @RequestBody UpdateMemberRoleRequest request
    ) {
        return ApiResponse.success(
                "회원 권한을 수정했습니다",
                adminService.updateMemberRole(email, memberId, request)
        );
    }

    @PatchMapping("/members/{memberId}/delete-status")
    public ApiResponse<AdminMemberSummaryResponse> updateMemberDeleteStatus(
            @AuthenticationPrincipal String email,
            @PathVariable Long memberId,
            @Valid @RequestBody UpdateMemberDeleteStatusRequest request
    ) {
        return ApiResponse.success(
                "회원 삭제 상태를 수정했습니다",
                adminService.updateMemberDeleteStatus(email, memberId, request)
        );
    }

    @GetMapping("/questions")
    public ApiResponse<PageResponse<AdminQuestionSummaryResponse>> getQuestions(
            @AuthenticationPrincipal String email,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Integer examRound,
            @RequestParam(required = false) String era,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean isDeleted
    ) {
        return ApiResponse.success(
                "관리자 문제 목록을 조회했습니다",
                adminService.getQuestions(email, page, size, examRound, era, category, keyword, isDeleted)
        );
    }

    @GetMapping("/questions/{questionId}")
    public ApiResponse<AdminQuestionDetailResponse> getQuestionDetail(
            @AuthenticationPrincipal String email,
            @PathVariable Long questionId
    ) {
        return ApiResponse.success(
                "관리자 문제 상세를 조회했습니다",
                adminService.getQuestionDetail(email, questionId)
        );
    }

    @PostMapping("/questions")
    public ApiResponse<AdminQuestionDetailResponse> createQuestion(
            @AuthenticationPrincipal String email,
            @Valid @RequestBody AdminQuestionCreateRequest request
    ) {
        return ApiResponse.success(
                "문제를 생성했습니다",
                adminService.createQuestion(email, request)
        );
    }

    @PutMapping("/questions/{questionId}")
    public ApiResponse<AdminQuestionDetailResponse> updateQuestion(
            @AuthenticationPrincipal String email,
            @PathVariable Long questionId,
            @Valid @RequestBody AdminQuestionUpdateRequest request
    ) {
        return ApiResponse.success(
                "문제를 수정했습니다",
                adminService.updateQuestion(email, questionId, request)
        );
    }

    @DeleteMapping("/questions/{questionId}")
    public ApiResponse<Void> deleteQuestion(
            @AuthenticationPrincipal String email,
            @PathVariable Long questionId
    ) {
        adminService.deleteQuestion(email, questionId);

        return ApiResponse.success(
                "문제를 삭제했습니다",
                null
        );
    }
}