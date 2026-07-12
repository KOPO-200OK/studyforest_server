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

    /**
     * 문제 목록을 조회합니다.
     *
     * 지원 조건:
     * - examRound: 시험 회차
     * - periodCode: 프론트에서 사용하는 시대 코드
     * - era: DB에 저장된 실제 시대 문자열
     * - category: 문제 분류
     * - page: 페이지 번호, 0부터 시작
     * - size: 페이지 크기
     */
    @GetMapping
    public ApiResponse<PageResponse<QuestionSummaryResponse>> getQuestions(
            @RequestParam(required = false) Integer examRound,
            @RequestParam(required = false) String periodCode,
            @RequestParam(required = false) String era,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.success(
                "문제 목록을 조회했습니다",
                questionBankService.getQuestions(
                        examRound,
                        periodCode,
                        era,
                        category,
                        page,
                        size
                )
        );
    }

    /**
     * 단일 문제 상세 정보를 조회합니다.
     */
    @GetMapping("/{questionId}")
    public ApiResponse<QuestionDetailResponse> getQuestion(
            @PathVariable Long questionId
    ) {
        return ApiResponse.success(
                "문제를 조회했습니다",
                questionBankService.getQuestion(questionId)
        );
    }

    /**
     * 문제를 채점하고 풀이 기록 및 오답노트를 저장합니다.
     */
    @PostMapping("/{questionId}/solve")
    public ApiResponse<SolveResultResponse> solveQuestion(
            @PathVariable Long questionId,
            @Valid @RequestBody SolveQuestionRequest request,
            @AuthenticationPrincipal String email
    ) {
        return ApiResponse.success(
                "채점이 완료되었습니다",
                questionBankService.solveQuestion(
                        questionId,
                        request,
                        email
                )
        );
    }

    /**
     * 조건에 맞는 문제를 무작위로 조회합니다.
     */
    @GetMapping("/random")
    public ApiResponse<List<QuestionDetailResponse>> getRandomQuestions(
            @RequestParam(defaultValue = "5") int count,
            @RequestParam(required = false) Integer examRound,
            @RequestParam(required = false) String periodCode,
            @RequestParam(required = false) String era,
            @RequestParam(required = false) String category
    ) {
        return ApiResponse.success(
                "랜덤 문제를 조회했습니다",
                questionBankService.getRandomQuestions(
                        count,
                        examRound,
                        periodCode,
                        era,
                        category
                )
        );
    }
}