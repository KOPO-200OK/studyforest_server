package com.gongsoop.ai.service;

import com.gongsoop.ai.client.AiServerClient;
import com.gongsoop.ai.dto.request.ChatMessageRequest;
import com.gongsoop.ai.dto.request.CreateChatSessionRequest;
import com.gongsoop.ai.dto.request.GenerateAiQuestionRequest;
import com.gongsoop.ai.dto.response.AiChatSessionResponse;
import com.gongsoop.ai.dto.response.ChatAnswerResponse;
import com.gongsoop.ai.dto.response.GenerateAiQuestionResponse;
import com.gongsoop.global.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class AiService {

    private final AiServerClient aiServerClient;

    private final AtomicLong sessionSequence = new AtomicLong(1);
    private final Map<Long, AiChatSessionResponse> sessions = new ConcurrentHashMap<>();

    public AiService(AiServerClient aiServerClient) {
        this.aiServerClient = aiServerClient;
    }

    public GenerateAiQuestionResponse generateQuestions(GenerateAiQuestionRequest request) {
        return aiServerClient.generateQuestions(request);
    }

    public AiChatSessionResponse createSession(CreateChatSessionRequest request) {
        Long sessionId = sessionSequence.getAndIncrement();

        String title = request.questionId() == null
                ? "AI 한국사 질의응답"
                : "문제 " + request.questionId() + "번 질문";

        AiChatSessionResponse response = new AiChatSessionResponse(sessionId, title);
        sessions.put(sessionId, response);

        return response;
    }

    public ChatAnswerResponse sendExamMessage(Long sessionId, ChatMessageRequest request) {
        if (!sessions.containsKey(sessionId)) {
            throw new BusinessException(
                    "CHAT_SESSION_NOT_FOUND",
                    "채팅 세션을 찾을 수 없습니다",
                    HttpStatus.NOT_FOUND
            );
        }

        return aiServerClient.askExam(request.message());
    }

    public ChatAnswerResponse askExam(ChatMessageRequest request) {
        return aiServerClient.askExam(request.message());
    }

    public ChatAnswerResponse askMotivation(ChatMessageRequest request) {
        return aiServerClient.askMotivation(request.message());
    }
}