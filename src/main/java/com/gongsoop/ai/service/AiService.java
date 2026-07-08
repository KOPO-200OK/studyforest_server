package com.gongsoop.ai.service;


import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

import org.springframework.http.MediaType;
import java.util.LinkedHashMap;
import java.util.Map;
import com.gongsoop.ai.dto.request.ChatMessageRequest;
import com.gongsoop.ai.dto.request.GenerateAiQuestionRequest;
import com.gongsoop.ai.dto.request.QuestionExplanationRequest;
import com.gongsoop.ai.dto.response.ChatAnswerResponse;
import com.gongsoop.ai.dto.response.GenerateAiQuestionResponse;
import com.gongsoop.global.exception.BusinessException;
import com.gongsoop.question.entity.HistExamQuestion;
import com.gongsoop.question.entity.HistExamQuestionId;
import com.gongsoop.question.repository.HistExamQuestionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Service
public class AiService {

    private final RestClient aiRestClient;
    private final HistExamQuestionRepository questionRepository;

    public AiService(
            RestClient aiRestClient,
            HistExamQuestionRepository questionRepository
    ) {
        this.aiRestClient = aiRestClient;
        this.questionRepository = questionRepository;
    }

    public GenerateAiQuestionResponse generateQuestions(GenerateAiQuestionRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();

        body.put("topic", request.topic());
        body.put("difficulty", request.difficulty());
        body.put("question_type", request.questionType());
        body.put("count", request.count());
        body.put("include_explanation", request.includeExplanation());

        return postToAiServer(
                "/api/v1/questions/generate",
                body,
                GenerateAiQuestionResponse.class
        );
    }

    public ChatAnswerResponse chat(ChatMessageRequest request) {
        return postToAiServer(
                "/api/v1/chat/exam",
                request,
                ChatAnswerResponse.class
        );
    }

    public ChatAnswerResponse motivation(ChatMessageRequest request) {
        return postToAiServer(
                "/api/v1/chat/motivation",
                request,
                ChatAnswerResponse.class
        );
    }

    public ChatAnswerResponse explainQuestion(
            Long questionId,
            QuestionExplanationRequest request
    ) {
        HistExamQuestion question = findQuestionBySyntheticId(questionId);

        FastApiQuestionExplanationRequest aiRequest =
                new FastApiQuestionExplanationRequest(
                        question.getQuestionText(),
                        question.getPassage(),
                        question.getEra(),
                        question.getCategory(),
                        List.of(
                                new FastApiQuestionOption(1, question.getChoice1()),
                                new FastApiQuestionOption(2, question.getChoice2()),
                                new FastApiQuestionOption(3, question.getChoice3()),
                                new FastApiQuestionOption(4, question.getChoice4()),
                                new FastApiQuestionOption(5, question.getChoice5())
                        ),
                        question.getAnswer(),
                        request.selectedOptionId()
                );

        return postToAiServer(
                "/api/v1/questions/explanation",
                aiRequest,
                ChatAnswerResponse.class
        );
    }

    private <T> T postToAiServer(String uri, Object body, Class<T> responseType) {
        try {
            T response = aiRestClient.post()
                    .uri(uri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(responseType);

            if (response == null) {
                throw new BusinessException(
                        "AI_EMPTY_RESPONSE",
                        "AI 서버 응답이 비어 있습니다",
                        HttpStatus.BAD_GATEWAY
                );
            }

            return response;

        } catch (org.springframework.web.client.RestClientResponseException e) {
            throw new BusinessException(
                    "AI_SERVER_RESPONSE_ERROR",
                    "AI 서버 응답 오류: status=" + e.getStatusCode() + ", body=" + e.getResponseBodyAsString(),
                    HttpStatus.BAD_GATEWAY
            );

        } catch (org.springframework.web.client.ResourceAccessException e) {
            throw new BusinessException(
                    "AI_SERVER_CONNECTION_ERROR",
                    "AI 서버에 연결할 수 없습니다. FastAPI 서버가 127.0.0.1:8000에서 실행 중인지 확인해주세요.",
                    HttpStatus.BAD_GATEWAY
            );

        } catch (Exception e) {
            throw new BusinessException(
                    "AI_SERVER_ERROR",
                    "AI 서버 호출 중 오류가 발생했습니다: " + e.getMessage(),
                    HttpStatus.BAD_GATEWAY
            );
        }
    }

    private HistExamQuestion findQuestionBySyntheticId(Long questionId) {
        int examRound = (int) (questionId / 1000);
        int qNo = (int) (questionId % 1000);

        return questionRepository.findById(new HistExamQuestionId(examRound, qNo))
                .orElseThrow(() -> new BusinessException(
                        "QUESTION_NOT_FOUND",
                        "문제를 찾을 수 없습니다",
                        HttpStatus.NOT_FOUND
                ));
    }

    private record FastApiQuestionExplanationRequest(
            String questionContent,
            String passage,
            String era,
            String category,
            List<FastApiQuestionOption> options,
            Integer correctOptionId,
            Integer selectedOptionId
    ) {
    }

    private record FastApiQuestionOption(
            Integer optionNo,
            String optionContent
    ) {
    }
}