package com.gongsoop.ai.service;

import com.gongsoop.ai.dto.request.SolveAiGeneratedQuestionRequest;
import com.gongsoop.ai.dto.response.AiGeneratedQuestionSolveRecordResponse;
import com.gongsoop.ai.dto.response.SolveAiGeneratedQuestionResponse;
import com.gongsoop.ai.entity.AiGeneratedQuestionSolveRecord;
import com.gongsoop.ai.repository.AiGeneratedQuestionSolveRecordRepository;
import com.gongsoop.ai.dto.request.UpdateChatSessionTitleRequest;
import com.gongsoop.ai.dto.request.CreateChatSessionRequest;
import com.gongsoop.ai.dto.response.AiChatSessionResponse;
import com.gongsoop.ai.dto.response.ChatMessageResponse;
import com.gongsoop.ai.dto.response.ChatSessionSummaryResponse;
import com.gongsoop.ai.entity.AiChatMessage;
import com.gongsoop.ai.entity.AiChatSession;
import com.gongsoop.ai.repository.AiChatMessageRepository;
import com.gongsoop.ai.repository.AiChatSessionRepository;
import com.gongsoop.member.entity.Member;
import com.gongsoop.member.repository.MemberRepository;
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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gongsoop.ai.dto.response.AiGeneratedQuestionDetailResponse;
import com.gongsoop.ai.dto.response.AiGeneratedQuestionSetDetailResponse;
import com.gongsoop.ai.dto.response.AiGeneratedQuestionSetSummaryResponse;
import com.gongsoop.ai.dto.response.GeneratedQuestionResponse;
import com.gongsoop.ai.entity.AiGeneratedQuestion;
import com.gongsoop.ai.entity.AiGeneratedQuestionSet;
import com.gongsoop.ai.repository.AiGeneratedQuestionRepository;
import com.gongsoop.ai.repository.AiGeneratedQuestionSetRepository;
import com.gongsoop.question.dto.response.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AiService {

    private final AiGeneratedQuestionSolveRecordRepository aiGeneratedQuestionSolveRecordRepository;
    private final MemberRepository memberRepository;
    private final AiChatSessionRepository aiChatSessionRepository;
    private final AiChatMessageRepository aiChatMessageRepository;
    private final RestClient aiRestClient;
    private final HistExamQuestionRepository questionRepository;
    private final ObjectMapper objectMapper;
    private final AiGeneratedQuestionSetRepository aiGeneratedQuestionSetRepository;
    private final AiGeneratedQuestionRepository aiGeneratedQuestionRepository;

    public AiService(
            RestClient aiRestClient,
            HistExamQuestionRepository questionRepository,
            MemberRepository memberRepository,
            AiChatSessionRepository aiChatSessionRepository,
            AiChatMessageRepository aiChatMessageRepository,
            ObjectMapper objectMapper,
            AiGeneratedQuestionSetRepository aiGeneratedQuestionSetRepository,
            AiGeneratedQuestionRepository aiGeneratedQuestionRepository,
            AiGeneratedQuestionSolveRecordRepository aiGeneratedQuestionSolveRecordRepository
    ) {
        this.aiRestClient = aiRestClient;
        this.questionRepository = questionRepository;
        this.memberRepository = memberRepository;
        this.aiChatSessionRepository = aiChatSessionRepository;
        this.aiChatMessageRepository = aiChatMessageRepository;
        this.objectMapper = objectMapper;
        this.aiGeneratedQuestionSetRepository = aiGeneratedQuestionSetRepository;
        this.aiGeneratedQuestionRepository = aiGeneratedQuestionRepository;
        this.aiGeneratedQuestionSolveRecordRepository = aiGeneratedQuestionSolveRecordRepository;
    }

    @Transactional
    public GenerateAiQuestionResponse generateQuestions(String email, GenerateAiQuestionRequest request) {
        Long memberId = getCurrentMemberId(email);

        Map<String, Object> body = new LinkedHashMap<>();

        body.put("topic", request.topic());
        body.put("difficulty", request.difficulty());
        body.put("question_type", request.questionType());
        body.put("count", request.count());
        body.put("include_explanation", request.includeExplanation());

        GenerateAiQuestionResponse response = postToAiServer(
                "/api/v1/questions/generate",
                body,
                GenerateAiQuestionResponse.class
        );

        saveGeneratedQuestions(memberId, request, response);

        return response;
    }

    @Transactional(readOnly = true)
    public PageResponse<AiGeneratedQuestionSetSummaryResponse> getGeneratedQuestionSets(
            String email,
            int page,
            int size
    ) {
        Long memberId = getCurrentMemberId(email);

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);

        Pageable pageable = PageRequest.of(safePage, safeSize);

        Page<AiGeneratedQuestionSet> result =
                aiGeneratedQuestionSetRepository.findByMemberIdOrderByCreatedAtDesc(memberId, pageable);

        List<AiGeneratedQuestionSetSummaryResponse> content = result.getContent()
                .stream()
                .map(this::toGeneratedQuestionSetSummaryResponse)
                .toList();

        return new PageResponse<>(
                content,
                result.getTotalElements(),
                result.getTotalPages(),
                result.getNumber(),
                result.getSize()
        );
    }

    @Transactional(readOnly = true)
    public AiGeneratedQuestionSetDetailResponse getGeneratedQuestionSetDetail(
            String email,
            Long setId
    ) {
        Long memberId = getCurrentMemberId(email);

        AiGeneratedQuestionSet questionSet = findGeneratedQuestionSet(setId, memberId);

        List<AiGeneratedQuestionDetailResponse> questions =
                aiGeneratedQuestionRepository.findByAiGeneratedQuestionSetIdOrderByQuestionOrderAsc(setId)
                        .stream()
                        .map(this::toGeneratedQuestionDetailResponse)
                        .toList();

        return new AiGeneratedQuestionSetDetailResponse(
                questionSet.getAiGeneratedQuestionSetId(),
                questionSet.getTopic(),
                questionSet.getDifficulty(),
                questionSet.getQuestionType(),
                questionSet.getQuestionCount(),
                questionSet.getIncludeExplanation(),
                questionSet.getCreatedAt(),
                questions
        );
    }

    @Transactional
    public void deleteGeneratedQuestionSet(String email, Long setId) {
        Long memberId = getCurrentMemberId(email);

        AiGeneratedQuestionSet questionSet = findGeneratedQuestionSet(setId, memberId);

        aiGeneratedQuestionSetRepository.delete(questionSet);
    }

    private void saveGeneratedQuestions(
            Long memberId,
            GenerateAiQuestionRequest request,
            GenerateAiQuestionResponse response
    ) {
        if (response.questions() == null || response.questions().isEmpty()) {
            return;
        }

        AiGeneratedQuestionSet questionSet = AiGeneratedQuestionSet.create(
                memberId,
                request.topic(),
                request.difficulty(),
                request.questionType(),
                response.questions().size(),
                request.includeExplanation()
        );

        AiGeneratedQuestionSet savedSet = aiGeneratedQuestionSetRepository.save(questionSet);

        int order = 1;

        for (GeneratedQuestionResponse generatedQuestion : response.questions()) {
            AiGeneratedQuestion question = AiGeneratedQuestion.create(
                    savedSet.getAiGeneratedQuestionSetId(),
                    order,
                    generatedQuestion.question(),
                    writeChoicesJson(generatedQuestion.choices()),
                    generatedQuestion.answer(),
                    generatedQuestion.explanation(),
                    generatedQuestion.era(),
                    generatedQuestion.topic(),
                    generatedQuestion.difficulty(),
                    generatedQuestion.examTip()
            );

            aiGeneratedQuestionRepository.save(question);
            order++;
        }
    }

    private AiGeneratedQuestionSet findGeneratedQuestionSet(Long setId, Long memberId) {
        return aiGeneratedQuestionSetRepository.findByAiGeneratedQuestionSetIdAndMemberId(setId, memberId)
                .orElseThrow(() -> new BusinessException(
                        "AI_GENERATED_QUESTION_SET_NOT_FOUND",
                        "AI 생성 문제 기록을 찾을 수 없습니다",
                        HttpStatus.NOT_FOUND
                ));
    }

    private AiGeneratedQuestionSetSummaryResponse toGeneratedQuestionSetSummaryResponse(
            AiGeneratedQuestionSet questionSet
    ) {
        return new AiGeneratedQuestionSetSummaryResponse(
                questionSet.getAiGeneratedQuestionSetId(),
                questionSet.getTopic(),
                questionSet.getDifficulty(),
                questionSet.getQuestionType(),
                questionSet.getQuestionCount(),
                questionSet.getIncludeExplanation(),
                questionSet.getCreatedAt()
        );
    }

    private AiGeneratedQuestionDetailResponse toGeneratedQuestionDetailResponse(
            AiGeneratedQuestion question
    ) {
        return new AiGeneratedQuestionDetailResponse(
                question.getAiGeneratedQuestionId(),
                question.getQuestionOrder(),
                question.getQuestionText(),
                readChoicesJson(question.getChoicesJson()),
                question.getAnswerText(),
                question.getExplanation(),
                question.getEra(),
                question.getTopic(),
                question.getDifficulty(),
                question.getExamTip()
        );
    }

    private String writeChoicesJson(List<String> choices) {
        try {
            List<String> safeChoices = choices == null ? List.of() : choices;
            return objectMapper.writeValueAsString(safeChoices);
        } catch (JsonProcessingException e) {
            throw new BusinessException(
                    "AI_GENERATED_QUESTION_SAVE_ERROR",
                    "AI 생성 문제 보기를 저장하는 중 오류가 발생했습니다",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    private List<String> readChoicesJson(String choicesJson) {
        if (choicesJson == null || choicesJson.isBlank()) {
            return List.of();
        }

        try {
            return objectMapper.readValue(
                    choicesJson,
                    new TypeReference<List<String>>() {
                    }
            );
        } catch (JsonProcessingException e) {
            return List.of();
        }
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

    public AiChatSessionResponse createChatSession(String email, CreateChatSessionRequest request) {
        Long memberId = getCurrentMemberId(email);

        AiChatSession session;

        if (request.questionId() == null) {
            session = AiChatSession.createGeneral(memberId);
        } else {
            HistExamQuestion question = findQuestionBySyntheticId(request.questionId());
            session = AiChatSession.createWithQuestion(memberId, question);
        }

        AiChatSession savedSession = aiChatSessionRepository.save(session);

        return new AiChatSessionResponse(
                savedSession.getAiChatSessionId(),
                savedSession.getTitle()
        );
    }

    public List<ChatSessionSummaryResponse> getChatSessions(String email) {
        Long memberId = getCurrentMemberId(email);

        return aiChatSessionRepository.findByMemberIdOrderByUpdatedAtDesc(memberId)
                .stream()
                .map(this::toChatSessionSummaryResponse)
                .toList();
    }

    public List<ChatMessageResponse> getChatMessages(String email, Long sessionId) {
        Long memberId = getCurrentMemberId(email);
        findSession(sessionId, memberId);

        return aiChatMessageRepository.findByAiChatSessionIdOrderByCreatedAtAsc(sessionId)
                .stream()
                .map(this::toChatMessageResponse)
                .toList();
    }

    public ChatAnswerResponse sendChatMessage(
            String email,
            Long sessionId,
            ChatMessageRequest request
    ) {
        Long memberId = getCurrentMemberId(email);
        AiChatSession session = findSession(sessionId, memberId);

        aiChatMessageRepository.save(
                AiChatMessage.user(session.getAiChatSessionId(), request.message())
        );

        String prompt = buildSessionPrompt(session, request.message());

        ChatAnswerResponse answerResponse = postToAiServer(
                "/api/v1/chat/exam",
                new ChatMessageRequest(prompt),
                ChatAnswerResponse.class
        );

        aiChatMessageRepository.save(
                AiChatMessage.ai(session.getAiChatSessionId(), answerResponse.answer())
        );

        session.touch();
        aiChatSessionRepository.save(session);

        return answerResponse;
    }

    private AiChatSession findSession(Long sessionId, Long memberId) {
        return aiChatSessionRepository.findByAiChatSessionIdAndMemberId(sessionId, memberId)
                .orElseThrow(() -> new BusinessException(
                        "AI_CHAT_SESSION_NOT_FOUND",
                        "AI 채팅 세션을 찾을 수 없습니다",
                        HttpStatus.NOT_FOUND
                ));
    }

    private String buildSessionPrompt(AiChatSession session, String message) {
        if (session.getExamRound() == null || session.getQNo() == null) {
            return message;
        }

        return """
            사용자는 한국사능력검정시험 기출문제에 대해 질문하고 있습니다.

            [관련 문제]
            %d회 %d번

            [사용자 질문]
            %s
            """.formatted(session.getExamRound(), session.getQNo(), message);
    }

    private ChatSessionSummaryResponse toChatSessionSummaryResponse(AiChatSession session) {
        Long questionId = null;

        if (session.getExamRound() != null && session.getQNo() != null) {
            questionId = session.getExamRound() * 1000L + session.getQNo();
        }

        return new ChatSessionSummaryResponse(
                session.getAiChatSessionId(),
                session.getTitle(),
                questionId,
                session.getCreatedAt(),
                session.getUpdatedAt()
        );
    }

    private ChatMessageResponse toChatMessageResponse(AiChatMessage message) {
        return new ChatMessageResponse(
                message.getAiChatMessageId(),
                message.getSender(),
                message.getMessageText(),
                message.getCreatedAt()
        );
    }

    private Long getCurrentMemberId(String email) {
        if (email == null || email.isBlank()) {
            throw new BusinessException(
                    "UNAUTHORIZED",
                    "로그인이 필요합니다",
                    HttpStatus.UNAUTHORIZED
            );
        }

        Member member = memberRepository.findByEmail(email)
                .filter(m -> !m.isDeleted())
                .orElseThrow(() -> new BusinessException(
                        "MEMBER_NOT_FOUND",
                        "회원 정보를 찾을 수 없습니다",
                        HttpStatus.UNAUTHORIZED
                ));

        return member.getId();
    }

    public AiChatSessionResponse updateChatSessionTitle(
            String email,
            Long sessionId,
            UpdateChatSessionTitleRequest request
    ) {
        Long memberId = getCurrentMemberId(email);
        AiChatSession session = findSession(sessionId, memberId);

        session.changeTitle(request.title());

        AiChatSession savedSession = aiChatSessionRepository.save(session);

        return new AiChatSessionResponse(
                savedSession.getAiChatSessionId(),
                savedSession.getTitle()
        );
    }

    public void deleteChatSession(String email, Long sessionId) {
        Long memberId = getCurrentMemberId(email);
        AiChatSession session = findSession(sessionId, memberId);

        aiChatSessionRepository.delete(session);
    }
    @Transactional
    public SolveAiGeneratedQuestionResponse solveGeneratedQuestion(
            String email,
            Long generatedQuestionId,
            SolveAiGeneratedQuestionRequest request
    ) {
        Long memberId = getCurrentMemberId(email);

        AiGeneratedQuestion question = aiGeneratedQuestionRepository.findById(generatedQuestionId)
                .orElseThrow(() -> new BusinessException(
                        "AI_GENERATED_QUESTION_NOT_FOUND",
                        "AI 생성 문제를 찾을 수 없습니다",
                        HttpStatus.NOT_FOUND
                ));

        findGeneratedQuestionSet(question.getAiGeneratedQuestionSetId(), memberId);

        if (request.selectedChoiceIndex() == null && !hasText(request.selectedAnswerText())) {
            throw new BusinessException(
                    "SELECTED_ANSWER_REQUIRED",
                    "선택한 답안을 입력해주세요",
                    HttpStatus.BAD_REQUEST
            );
        }

        String selectedAnswerText = resolveSelectedAnswerText(question, request);
        boolean isCorrect = isGeneratedAnswerCorrect(question, request.selectedChoiceIndex(), selectedAnswerText);

        AiGeneratedQuestionSolveRecord record = AiGeneratedQuestionSolveRecord.create(
                memberId,
                question.getAiGeneratedQuestionId(),
                request.selectedChoiceIndex(),
                selectedAnswerText,
                question.getAnswerText(),
                isCorrect
        );

        AiGeneratedQuestionSolveRecord savedRecord =
                aiGeneratedQuestionSolveRecordRepository.save(record);

        return new SolveAiGeneratedQuestionResponse(
                savedRecord.getAiGeneratedQuestionSolveRecordId(),
                question.getAiGeneratedQuestionId(),
                savedRecord.getSelectedChoiceIndex(),
                savedRecord.getSelectedAnswerText(),
                savedRecord.getCorrectAnswerText(),
                savedRecord.getIsCorrect(),
                question.getExplanation()
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<AiGeneratedQuestionSolveRecordResponse> getGeneratedQuestionSolveRecords(
            String email,
            int page,
            int size
    ) {
        Long memberId = getCurrentMemberId(email);

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);

        Pageable pageable = PageRequest.of(safePage, safeSize);

        Page<AiGeneratedQuestionSolveRecord> result =
                aiGeneratedQuestionSolveRecordRepository.findByMemberIdOrderBySolvedAtDesc(memberId, pageable);

        List<AiGeneratedQuestionSolveRecordResponse> content = result.getContent()
                .stream()
                .map(this::toGeneratedQuestionSolveRecordResponse)
                .toList();

        return new PageResponse<>(
                content,
                result.getTotalElements(),
                result.getTotalPages(),
                result.getNumber(),
                result.getSize()
        );
    }

    private AiGeneratedQuestionSolveRecordResponse toGeneratedQuestionSolveRecordResponse(
            AiGeneratedQuestionSolveRecord record
    ) {
        AiGeneratedQuestion question = aiGeneratedQuestionRepository.findById(record.getAiGeneratedQuestionId())
                .orElse(null);

        return new AiGeneratedQuestionSolveRecordResponse(
                record.getAiGeneratedQuestionSolveRecordId(),
                record.getAiGeneratedQuestionId(),
                question == null ? null : question.getQuestionText(),
                record.getSelectedChoiceIndex(),
                record.getSelectedAnswerText(),
                record.getCorrectAnswerText(),
                record.getIsCorrect(),
                record.getSolvedAt()
        );
    }

    private String resolveSelectedAnswerText(
            AiGeneratedQuestion question,
            SolveAiGeneratedQuestionRequest request
    ) {
        if (hasText(request.selectedAnswerText())) {
            return request.selectedAnswerText().trim();
        }

        if (request.selectedChoiceIndex() == null) {
            return null;
        }

        List<String> choices = readChoicesJson(question.getChoicesJson());
        int index = request.selectedChoiceIndex() - 1;

        if (index >= 0 && index < choices.size()) {
            return choices.get(index);
        }

        return String.valueOf(request.selectedChoiceIndex());
    }

    private boolean isGeneratedAnswerCorrect(
            AiGeneratedQuestion question,
            Integer selectedChoiceIndex,
            String selectedAnswerText
    ) {
        String correctAnswer = question.getAnswerText();

        Integer correctIndex = extractAnswerIndex(correctAnswer);

        if (correctIndex != null && selectedChoiceIndex != null) {
            return correctIndex.equals(selectedChoiceIndex);
        }

        String selected = normalizeAnswer(selectedAnswerText);
        String correct = normalizeAnswer(correctAnswer);

        if (!hasText(selected) || !hasText(correct)) {
            return false;
        }

        return selected.equals(correct)
                || selected.contains(correct)
                || correct.contains(selected);
    }

    private Integer extractAnswerIndex(String answer) {
        if (!hasText(answer)) {
            return null;
        }

        if (answer.contains("①")) {
            return 1;
        }
        if (answer.contains("②")) {
            return 2;
        }
        if (answer.contains("③")) {
            return 3;
        }
        if (answer.contains("④")) {
            return 4;
        }
        if (answer.contains("⑤")) {
            return 5;
        }

        for (char c : answer.toCharArray()) {
            if (c >= '1' && c <= '5') {
                return c - '0';
            }
        }

        return null;
    }

    private String normalizeAnswer(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replaceAll("\\s+", "")
                .replace("①", "1")
                .replace("②", "2")
                .replace("③", "3")
                .replace("④", "4")
                .replace("⑤", "5")
                .trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}