package com.gongsoop.question.controller;

import com.gongsoop.global.response.ApiResponse;
import com.gongsoop.question.dto.request.SolveQuestionRequest;
import com.gongsoop.question.dto.response.PageResponse;
import com.gongsoop.question.dto.response.QuestionDetailResponse;
import com.gongsoop.question.dto.response.QuestionSummaryResponse;
import com.gongsoop.question.dto.response.SolveResultResponse;
import com.gongsoop.question.service.QuestionBankService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/questions")
public class QuestionController {

    private final QuestionBankService questionBankService;

    public QuestionController(QuestionBankService questionBankService) {
        this.questionBankService = questionBankService;
    }

    @GetMapping
    public ApiResponse<PageResponse<QuestionSummaryResponse>> getQuestions(
            @RequestParam(required = false) Integer examRound,
            @RequestParam(required = false) String era,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.success(
                "문제 목록을 조회했습니다",
                questionBankService.getQuestions(examRound, era, category, page, size)
        );
    }

    @GetMapping("/{questionId}")
    public ApiResponse<QuestionDetailResponse> getQuestion(
            @PathVariable Long questionId
    ) {
        return ApiResponse.success(
                "문제를 조회했습니다",
                questionBankService.getQuestion(questionId)
        );
    }

    @PostMapping("/{questionId}/solve")
    public ApiResponse<SolveResultResponse> solveQuestion(
            @PathVariable Long questionId,
            @Valid @RequestBody SolveQuestionRequest request,
            @AuthenticationPrincipal String email
    ) {
        return ApiResponse.success(
                "채점이 완료되었습니다",
                questionBankService.solveQuestion(questionId, request, email)
        );
    }

    @GetMapping("/random")
    public ApiResponse<List<QuestionDetailResponse>> getRandomQuestions(
            @RequestParam(defaultValue = "5") int count,
            @RequestParam(required = false) Integer examRound,
            @RequestParam(required = false) String era,
            @RequestParam(required = false) String category
    ) {
        return ApiResponse.success(
                "랜덤 문제를 조회했습니다",
                questionBankService.getRandomQuestions(count, examRound, era, category)
        );
    }
}