package com.gongsoop.question.controller;

import com.gongsoop.global.response.ApiResponse;
import com.gongsoop.question.dto.request.SolveQuestionRequest;
import com.gongsoop.question.dto.response.PageResponse;
import com.gongsoop.question.dto.response.SolveResultResponse;
import com.gongsoop.question.dto.response.WrongAnswerSummaryResponse;
import com.gongsoop.question.service.QuestionBankService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/wrong-answers")
public class WrongAnswerController {

    private final QuestionBankService questionBankService;

    public WrongAnswerController(
            QuestionBankService questionBankService
    ) {
        this.questionBankService = questionBankService;
    }

    /**
     * 로그인한 사용자의 오답노트 목록을 조회합니다.
     *
     * resolved:
     * - 값을 보내지 않음: 전체 조회
     * - true: 해결 완료 문제만 조회
     * - false: 미해결 문제만 조회
     */
    @GetMapping
    public ApiResponse<PageResponse<WrongAnswerSummaryResponse>> getWrongAnswers(
            @AuthenticationPrincipal String email,
            @RequestParam(required = false) Boolean resolved,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.success(
                "오답노트를 조회했습니다",
                questionBankService.getWrongAnswers(
                        email,
                        resolved,
                        page,
                        size
                )
        );
    }

    /**
     * 로그인한 사용자의 오답노트 한 건을 조회합니다.
     *
     * 오답노트 상세 주소로 직접 접근하거나
     * 새로고침했을 때 사용합니다.
     */
    @GetMapping("/{wrongAnswerId}")
    public ApiResponse<WrongAnswerSummaryResponse> getWrongAnswer(
            @PathVariable Long wrongAnswerId,
            @AuthenticationPrincipal String email
    ) {
        return ApiResponse.success(
                "오답노트를 조회했습니다",
                questionBankService.getWrongAnswer(
                        email,
                        wrongAnswerId
                )
        );
    }

    /**
     * 오답노트 문제를 다시 채점합니다.
     */
    @PostMapping("/{wrongAnswerId}/retry")
    public ApiResponse<SolveResultResponse> retryWrongAnswer(
            @PathVariable Long wrongAnswerId,
            @Valid @RequestBody SolveQuestionRequest request,
            @AuthenticationPrincipal String email
    ) {
        return ApiResponse.success(
                "오답 재풀이 채점이 완료되었습니다",
                questionBankService.retryWrongAnswer(
                        wrongAnswerId,
                        request,
                        email
                )
        );
    }
}