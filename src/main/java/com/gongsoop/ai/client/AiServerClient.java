package com.gongsoop.ai.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gongsoop.ai.dto.request.GenerateAiQuestionRequest;
import com.gongsoop.ai.dto.response.ChatAnswerResponse;
import com.gongsoop.ai.dto.response.GenerateAiQuestionResponse;
import com.gongsoop.global.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;

@Component
public class AiServerClient {

    private final RestClient restClient;

    public AiServerClient(
            @Value("${ai.server.base-url}") String baseUrl,
            @Value("${ai.server.connect-timeout-ms}") int connectTimeoutMs,
            @Value("${ai.server.read-timeout-ms}") int readTimeoutMs
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    public GenerateAiQuestionResponse generateQuestions(GenerateAiQuestionRequest request) {
        try {
            return restClient.post()
                    .uri("/api/v1/questions/generate")
                    .body(request)
                    .retrieve()
                    .body(GenerateAiQuestionResponse.class);
        } catch (RestClientException e) {
            throw new BusinessException(
                    "AI_SERVER_ERROR",
                    "AI 문제 생성 서버와 통신하지 못했습니다",
                    HttpStatus.BAD_GATEWAY
            );
        }
    }

    public ChatAnswerResponse askExam(String message) {
        return ask("/api/v1/chat/exam", message);
    }

    public ChatAnswerResponse askMotivation(String message) {
        return ask("/api/v1/chat/motivation", message);
    }

    private ChatAnswerResponse ask(String path, String message) {
        try {
            return restClient.post()
                    .uri(path)
                    .body(new AiChatPayload(message))
                    .retrieve()
                    .body(ChatAnswerResponse.class);
        } catch (RestClientException e) {
            throw new BusinessException(
                    "AI_SERVER_ERROR",
                    "AI 채팅 서버와 통신하지 못했습니다",
                    HttpStatus.BAD_GATEWAY
            );
        }
    }

    private record AiChatPayload(
            @JsonProperty("message")
            String message
    ) {
    }
}