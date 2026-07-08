package com.gongsoop.ai.controller;

import com.gongsoop.ai.dto.request.ChatMessageRequest;
import com.gongsoop.ai.dto.request.GenerateAiQuestionRequest;
import com.gongsoop.ai.dto.request.QuestionExplanationRequest;
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
}