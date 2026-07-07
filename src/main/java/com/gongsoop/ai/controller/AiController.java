package com.gongsoop.ai.controller;

import com.gongsoop.ai.dto.request.ChatMessageRequest;
import com.gongsoop.ai.dto.request.CreateChatSessionRequest;
import com.gongsoop.ai.dto.request.GenerateAiQuestionRequest;
import com.gongsoop.ai.dto.response.AiChatSessionResponse;
import com.gongsoop.ai.dto.response.ChatAnswerResponse;
import com.gongsoop.ai.dto.response.GenerateAiQuestionResponse;
import com.gongsoop.ai.service.AiService;
import com.gongsoop.global.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ai")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/questions/generate")
    public ApiResponse<GenerateAiQuestionResponse> generateQuestions(
            @Valid @RequestBody GenerateAiQuestionRequest request
    ) {
        return ApiResponse.success(
                "AI 문제가 생성되었습니다",
                aiService.generateQuestions(request)
        );
    }

    @PostMapping("/chat-sessions")
    public ApiResponse<AiChatSessionResponse> createChatSession(
            @RequestBody(required = false) CreateChatSessionRequest request
    ) {
        CreateChatSessionRequest safeRequest = request == null
                ? new CreateChatSessionRequest(null)
                : request;

        return ApiResponse.success(
                "AI 채팅 세션이 생성되었습니다",
                aiService.createSession(safeRequest)
        );
    }

    @PostMapping("/chat-sessions/{chatSessionId}/messages")
    public ApiResponse<ChatAnswerResponse> sendChatMessage(
            @PathVariable Long chatSessionId,
            @Valid @RequestBody ChatMessageRequest request
    ) {
        return ApiResponse.success(
                "AI 답변이 생성되었습니다",
                aiService.sendExamMessage(chatSessionId, request)
        );
    }

    @PostMapping("/chat/exam")
    public ApiResponse<ChatAnswerResponse> askExam(
            @Valid @RequestBody ChatMessageRequest request
    ) {
        return ApiResponse.success(
                "AI 답변이 생성되었습니다",
                aiService.askExam(request)
        );
    }

    @PostMapping("/chat/motivation")
    public ApiResponse<ChatAnswerResponse> askMotivation(
            @Valid @RequestBody ChatMessageRequest request
    ) {
        return ApiResponse.success(
                "AI 답변이 생성되었습니다",
                aiService.askMotivation(request)
        );
    }
}