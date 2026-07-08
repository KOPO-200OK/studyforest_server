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

    public WrongAnswerController(QuestionBankService questionBankService) {
        this.questionBankService = questionBankService;
    }

    @GetMapping
    public ApiResponse<PageResponse<WrongAnswerSummaryResponse>> getWrongAnswers(
            @AuthenticationPrincipal String email,
            @RequestParam(required = false) Boolean resolved,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.success(
                "오답노트를 조회했습니다",
                questionBankService.getWrongAnswers(email, resolved, page, size)
        );
    }

    @PostMapping("/{wrongAnswerId}/retry")
    public ApiResponse<SolveResultResponse> retryWrongAnswer(
            @PathVariable Long wrongAnswerId,
            @Valid @RequestBody SolveQuestionRequest request,
            @AuthenticationPrincipal String email
    ) {
        return ApiResponse.success(
                "오답 재풀이 채점이 완료되었습니다",
                questionBankService.retryWrongAnswer(wrongAnswerId, request, email)
        );
    }
}