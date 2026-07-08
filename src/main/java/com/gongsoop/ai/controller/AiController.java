package com.gongsoop.ai.controller;

import com.gongsoop.ai.dto.request.SolveAiGeneratedQuestionRequest;
import com.gongsoop.ai.dto.response.AiGeneratedQuestionSolveRecordResponse;
import com.gongsoop.ai.dto.response.SolveAiGeneratedQuestionResponse;
import com.gongsoop.ai.dto.response.AiGeneratedQuestionSetDetailResponse;
import com.gongsoop.ai.dto.response.AiGeneratedQuestionSetSummaryResponse;
import com.gongsoop.question.dto.response.PageResponse;
import com.gongsoop.ai.dto.request.UpdateChatSessionTitleRequest;
import com.gongsoop.ai.dto.request.ChatMessageRequest;
import com.gongsoop.ai.dto.request.GenerateAiQuestionRequest;
import com.gongsoop.ai.dto.request.QuestionExplanationRequest;
import com.gongsoop.ai.dto.response.ChatAnswerResponse;
import com.gongsoop.ai.dto.response.GenerateAiQuestionResponse;
import com.gongsoop.ai.service.AiService;
import com.gongsoop.global.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import com.gongsoop.ai.dto.request.CreateChatSessionRequest;
import com.gongsoop.ai.dto.response.AiChatSessionResponse;
import com.gongsoop.ai.dto.response.ChatMessageResponse;
import com.gongsoop.ai.dto.response.ChatSessionSummaryResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ai")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/questions/generate")
    public ApiResponse<GenerateAiQuestionResponse> generateQuestions(
            @AuthenticationPrincipal String email,
            @Valid @RequestBody GenerateAiQuestionRequest request
    ) {
        return ApiResponse.success(
                "AI 문제가 생성되었습니다",
                aiService.generateQuestions(email, request)
        );
    }

    @PostMapping("/chat")
    public ApiResponse<ChatAnswerResponse> chat(
            @Valid @RequestBody ChatMessageRequest request
    ) {
        return ApiResponse.success(
                "AI 답변을 생성했습니다",
                aiService.chat(request)
        );
    }

    @PostMapping("/motivation")
    public ApiResponse<ChatAnswerResponse> motivation(
            @Valid @RequestBody ChatMessageRequest request
    ) {
        return ApiResponse.success(
                "AI 동기부여 메시지를 생성했습니다",
                aiService.motivation(request)
        );
    }

    @PostMapping("/questions/{questionId}/explanation")
    public ApiResponse<ChatAnswerResponse> explainQuestion(
            @PathVariable Long questionId,
            @Valid @RequestBody QuestionExplanationRequest request
    ) {
        return ApiResponse.success(
                "AI 문제 해설을 생성했습니다",
                aiService.explainQuestion(questionId, request)
        );
    }

    @PostMapping("/chat-sessions")
    public ApiResponse<AiChatSessionResponse> createChatSession(
            @AuthenticationPrincipal String email,
            @RequestBody CreateChatSessionRequest request
    ) {
        return ApiResponse.success(
                "AI 채팅 세션을 생성했습니다",
                aiService.createChatSession(email, request)
        );
    }

    @GetMapping("/chat-sessions")
    public ApiResponse<List<ChatSessionSummaryResponse>> getChatSessions(
            @AuthenticationPrincipal String email
    ) {
        return ApiResponse.success(
                "AI 채팅 세션 목록을 조회했습니다",
                aiService.getChatSessions(email)
        );
    }

    @GetMapping("/chat-sessions/{sessionId}/messages")
    public ApiResponse<List<ChatMessageResponse>> getChatMessages(
            @AuthenticationPrincipal String email,
            @PathVariable Long sessionId
    ) {
        return ApiResponse.success(
                "AI 채팅 메시지 목록을 조회했습니다",
                aiService.getChatMessages(email, sessionId)
        );
    }

    @PostMapping("/chat-sessions/{sessionId}/messages")
    public ApiResponse<ChatAnswerResponse> sendChatMessage(
            @AuthenticationPrincipal String email,
            @PathVariable Long sessionId,
            @Valid @RequestBody ChatMessageRequest request
    ) {
        return ApiResponse.success(
                "AI 답변을 생성했습니다",
                aiService.sendChatMessage(email, sessionId, request)
        );
    }

    @PatchMapping("/chat-sessions/{sessionId}")
    public ApiResponse<AiChatSessionResponse> updateChatSessionTitle(
            @AuthenticationPrincipal String email,
            @PathVariable Long sessionId,
            @Valid @RequestBody UpdateChatSessionTitleRequest request
    ) {
        return ApiResponse.success(
                "AI 채팅 세션 제목을 수정했습니다",
                aiService.updateChatSessionTitle(email, sessionId, request)
        );
    }

    @DeleteMapping("/chat-sessions/{sessionId}")
    public ApiResponse<Void> deleteChatSession(
            @AuthenticationPrincipal String email,
            @PathVariable Long sessionId
    ) {
        aiService.deleteChatSession(email, sessionId);

        return ApiResponse.success(
                "AI 채팅 세션을 삭제했습니다",
                null
        );
    }

    @GetMapping("/generated-question-sets")
    public ApiResponse<PageResponse<AiGeneratedQuestionSetSummaryResponse>> getGeneratedQuestionSets(
            @AuthenticationPrincipal String email,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.success(
                "AI 생성 문제 기록 목록을 조회했습니다",
                aiService.getGeneratedQuestionSets(email, page, size)
        );
    }

    @GetMapping("/generated-question-sets/{setId}")
    public ApiResponse<AiGeneratedQuestionSetDetailResponse> getGeneratedQuestionSetDetail(
            @AuthenticationPrincipal String email,
            @PathVariable Long setId
    ) {
        return ApiResponse.success(
                "AI 생성 문제 기록 상세를 조회했습니다",
                aiService.getGeneratedQuestionSetDetail(email, setId)
        );
    }

    @DeleteMapping("/generated-question-sets/{setId}")
    public ApiResponse<Void> deleteGeneratedQuestionSet(
            @AuthenticationPrincipal String email,
            @PathVariable Long setId
    ) {
        aiService.deleteGeneratedQuestionSet(email, setId);

        return ApiResponse.success(
                "AI 생성 문제 기록을 삭제했습니다",
                null
        );
    }

    @PostMapping("/generated-questions/{generatedQuestionId}/solve")
    public ApiResponse<SolveAiGeneratedQuestionResponse> solveGeneratedQuestion(
            @AuthenticationPrincipal String email,
            @PathVariable Long generatedQuestionId,
            @Valid @RequestBody SolveAiGeneratedQuestionRequest request
    ) {
        return ApiResponse.success(
                "AI 생성 문제를 채점했습니다",
                aiService.solveGeneratedQuestion(email, generatedQuestionId, request)
        );
    }

    @GetMapping("/generated-question-solve-records")
    public ApiResponse<PageResponse<AiGeneratedQuestionSolveRecordResponse>> getGeneratedQuestionSolveRecords(
            @AuthenticationPrincipal String email,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.success(
                "AI 생성 문제 풀이 기록을 조회했습니다",
                aiService.getGeneratedQuestionSolveRecords(email, page, size)
        );
    }
}