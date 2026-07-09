package com.gongsoop.mockexam.service;

import com.gongsoop.mockexam.dto.response.MockExamSummaryResponse;
import com.gongsoop.question.dto.response.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import com.gongsoop.global.exception.BusinessException;
import com.gongsoop.member.entity.Member;
import com.gongsoop.member.repository.MemberRepository;
import com.gongsoop.mockexam.dto.request.MockExamAnswerRequest;
import com.gongsoop.mockexam.dto.request.MockExamCreateRequest;
import com.gongsoop.mockexam.dto.request.MockExamSubmitRequest;
import com.gongsoop.mockexam.dto.response.MockExamAnswerResultResponse;
import com.gongsoop.mockexam.dto.response.MockExamQuestionResponse;
import com.gongsoop.mockexam.dto.response.MockExamResultResponse;
import com.gongsoop.mockexam.dto.response.MockExamStartResponse;
import com.gongsoop.mockexam.entity.HistMockExam;
import com.gongsoop.mockexam.entity.HistMockExamQuestion;
import com.gongsoop.mockexam.repository.HistMockExamQuestionRepository;
import com.gongsoop.mockexam.repository.HistMockExamRepository;
import com.gongsoop.question.dto.response.QuestionDetailResponse;
import com.gongsoop.question.dto.response.QuestionOptionResponse;
import com.gongsoop.question.entity.HistExamQuestion;
import com.gongsoop.question.entity.HistSolveRecord;
import com.gongsoop.question.entity.HistWrongAnswer;
import com.gongsoop.question.repository.HistExamQuestionRepository;
import com.gongsoop.question.repository.HistSolveRecordRepository;
import com.gongsoop.question.repository.HistWrongAnswerRepository;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.gongsoop.mockexam.dto.request.MockExamSaveAnswersRequest;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class MockExamService {

    private final HistMockExamRepository mockExamRepository;
    private final HistMockExamQuestionRepository mockExamQuestionRepository;
    private final HistExamQuestionRepository questionRepository;
    private final HistSolveRecordRepository solveRecordRepository;
    private final HistWrongAnswerRepository wrongAnswerRepository;
    private final MemberRepository memberRepository;

    public MockExamService(
            HistMockExamRepository mockExamRepository,
            HistMockExamQuestionRepository mockExamQuestionRepository,
            HistExamQuestionRepository questionRepository,
            HistSolveRecordRepository solveRecordRepository,
            HistWrongAnswerRepository wrongAnswerRepository,
            MemberRepository memberRepository
    ) {
        this.mockExamRepository = mockExamRepository;
        this.mockExamQuestionRepository = mockExamQuestionRepository;
        this.questionRepository = questionRepository;
        this.solveRecordRepository = solveRecordRepository;
        this.wrongAnswerRepository = wrongAnswerRepository;
        this.memberRepository = memberRepository;
    }

    @Transactional
    public MockExamStartResponse createMockExam(MockExamCreateRequest request, String email) {
        Long memberId = getCurrentMemberId(email);

        int count = request.count() == null ? 20 : request.count();

        List<HistExamQuestion> candidates = questionRepository.findAll(
                buildQuestionSpecification(request.examRound(), request.era(), request.category())
        );

        if (candidates.size() < count) {
            throw new BusinessException(
                    "NOT_ENOUGH_QUESTIONS",
                    "조건에 맞는 문제가 부족합니다",
                    HttpStatus.BAD_REQUEST
            );
        }

        Collections.shuffle(candidates);

        List<HistExamQuestion> selectedQuestions = candidates.stream()
                .limit(count)
                .toList();

        String title = hasText(request.title()) ? request.title().trim() : "한국사 모의고사";

        HistMockExam mockExam = mockExamRepository.save(
                HistMockExam.create(memberId, title, count)
        );

        List<HistMockExamQuestion> examQuestions = new ArrayList<>();

        for (int i = 0; i < selectedQuestions.size(); i++) {
            examQuestions.add(
                    HistMockExamQuestion.create(
                            mockExam.getMockExamId(),
                            selectedQuestions.get(i),
                            i + 1
                    )
            );
        }

        List<HistMockExamQuestion> savedQuestions = mockExamQuestionRepository.saveAll(examQuestions);

        return toStartResponse(mockExam, savedQuestions);
    }

    public MockExamStartResponse getMockExam(Long mockExamId, String email) {
        Long memberId = getCurrentMemberId(email);
        HistMockExam mockExam = getMyMockExam(mockExamId, memberId);
        List<HistMockExamQuestion> questions =
                mockExamQuestionRepository.findByMockExamIdOrderByQuestionOrderAsc(mockExamId);

        return toStartResponse(mockExam, questions);
    }

    @Transactional
    public void saveMockExamAnswers(Long mockExamId, MockExamSaveAnswersRequest request, String email) {
        Long memberId = getCurrentMemberId(email);
        HistMockExam mockExam = getMyMockExam(mockExamId, memberId);

        if (mockExam.isSubmitted()) {
            throw new BusinessException(
                    "MOCK_EXAM_ALREADY_SUBMITTED",
                    "이미 제출된 모의고사는 답안을 수정할 수 없습니다",
                    HttpStatus.BAD_REQUEST
            );
        }

        List<HistMockExamQuestion> examQuestions =
                mockExamQuestionRepository.findByMockExamIdOrderByQuestionOrderAsc(mockExamId);

        Map<Long, Integer> answerMap = toAnswerMap(request.answers());

        Set<Long> expectedQuestionIds = examQuestions.stream()
                .map(q -> q.getQuestion().getSyntheticQuestionId())
                .collect(Collectors.toSet());

        if (!expectedQuestionIds.containsAll(answerMap.keySet())) {
            throw new BusinessException(
                    "INVALID_MOCK_EXAM_ANSWERS",
                    "해당 모의고사에 포함되지 않은 문제가 있습니다",
                    HttpStatus.BAD_REQUEST
            );
        }

        for (HistMockExamQuestion examQuestion : examQuestions) {
            Long questionId = examQuestion.getQuestion().getSyntheticQuestionId();

            if (answerMap.containsKey(questionId)) {
                examQuestion.mark(answerMap.get(questionId));
            }
        }

        mockExamQuestionRepository.saveAll(examQuestions);
    }

    @Transactional
    public MockExamResultResponse submitMockExam(Long mockExamId, MockExamSubmitRequest request, String email) {
        Long memberId = getCurrentMemberId(email);
        HistMockExam mockExam = getMyMockExam(mockExamId, memberId);

        if (mockExam.isSubmitted()) {
            throw new BusinessException(
                    "MOCK_EXAM_ALREADY_SUBMITTED",
                    "이미 제출된 모의고사입니다",
                    HttpStatus.BAD_REQUEST
            );
        }

        List<HistMockExamQuestion> examQuestions =
                mockExamQuestionRepository.findByMockExamIdOrderByQuestionOrderAsc(mockExamId);

        Map<Long, Integer> answerMap = request == null
                ? toSavedAnswerMap(examQuestions)
                : toAnswerMap(request.answers());

        Set<Long> expectedQuestionIds = examQuestions.stream()
                .map(q -> q.getQuestion().getSyntheticQuestionId())
                .collect(Collectors.toSet());

        if (!answerMap.keySet().equals(expectedQuestionIds)) {
            throw new BusinessException(
                    "INVALID_MOCK_EXAM_ANSWERS",
                    "모의고사의 모든 문제에 답안을 제출해야 합니다",
                    HttpStatus.BAD_REQUEST
            );
        }

        int correctCount = 0;

        for (HistMockExamQuestion examQuestion : examQuestions) {
            HistExamQuestion question = examQuestion.getQuestion();
            Long questionId = question.getSyntheticQuestionId();

            Integer selectedAnswer = answerMap.get(questionId);

            examQuestion.mark(selectedAnswer);

            boolean isCorrect = examQuestion.isCorrect();

            if (isCorrect) {
                correctCount++;
            }

            solveRecordRepository.save(
                    HistSolveRecord.create(
                            memberId,
                            question,
                            selectedAnswer,
                            isCorrect,
                            "MOCK_EXAM"
                    )
            );

            updateWrongAnswer(memberId, question, selectedAnswer, isCorrect);
        }

        mockExamQuestionRepository.saveAll(examQuestions);

        mockExam.submit(correctCount);

        return toResultResponse(mockExam, examQuestions);
    }

    public MockExamResultResponse getMockExamResult(Long mockExamId, String email) {
        Long memberId = getCurrentMemberId(email);
        HistMockExam mockExam = getMyMockExam(mockExamId, memberId);

        if (!mockExam.isSubmitted()) {
            throw new BusinessException(
                    "MOCK_EXAM_NOT_SUBMITTED",
                    "아직 제출되지 않은 모의고사입니다",
                    HttpStatus.BAD_REQUEST
            );
        }

        List<HistMockExamQuestion> questions =
                mockExamQuestionRepository.findByMockExamIdOrderByQuestionOrderAsc(mockExamId);

        return toResultResponse(mockExam, questions);
    }

    private Map<Long, Integer> toAnswerMap(List<MockExamAnswerRequest> answers) {
        Map<Long, Integer> answerMap = new HashMap<>();

        for (MockExamAnswerRequest answer : answers) {
            if (answerMap.containsKey(answer.questionId())) {
                throw new BusinessException(
                        "DUPLICATED_ANSWER",
                        "중복 제출된 답안이 있습니다",
                        HttpStatus.BAD_REQUEST
                );
            }

            answerMap.put(answer.questionId(), answer.selectedOptionId());
        }

        return answerMap;
    }

    private Map<Long, Integer> toSavedAnswerMap(List<HistMockExamQuestion> examQuestions) {
        Map<Long, Integer> answerMap = new HashMap<>();

        for (HistMockExamQuestion examQuestion : examQuestions) {
            if (examQuestion.getSelectedAnswer() == null) {
                throw new BusinessException(
                        "INVALID_MOCK_EXAM_ANSWERS",
                        "모의고사의 모든 문제에 답안을 제출해야 합니다",
                        HttpStatus.BAD_REQUEST
                );
            }

            answerMap.put(
                    examQuestion.getQuestion().getSyntheticQuestionId(),
                    examQuestion.getSelectedAnswer()
            );
        }

        return answerMap;
    }

    private void updateWrongAnswer(
            Long memberId,
            HistExamQuestion question,
            Integer selectedAnswer,
            boolean isCorrect
    ) {
        Optional<HistWrongAnswer> optionalWrongAnswer =
                wrongAnswerRepository.findByMemberIdAndExamRoundAndQNo(
                        memberId,
                        question.getExamRound(),
                        question.getQNo()
                );

        if (isCorrect) {
            optionalWrongAnswer.ifPresent(HistWrongAnswer::resolve);
            return;
        }

        if (optionalWrongAnswer.isPresent()) {
            optionalWrongAnswer.get().markWrong(selectedAnswer, question.getAnswer());
            return;
        }

        wrongAnswerRepository.save(
                HistWrongAnswer.create(memberId, question, selectedAnswer)
        );
    }

    private HistMockExam getMyMockExam(Long mockExamId, Long memberId) {
        return mockExamRepository.findByMockExamIdAndMemberId(mockExamId, memberId)
                .orElseThrow(() -> new BusinessException(
                        "MOCK_EXAM_NOT_FOUND",
                        "모의고사를 찾을 수 없습니다",
                        HttpStatus.NOT_FOUND
                ));
    }

    private Long getCurrentMemberId(String email) {
        if (!hasText(email)) {
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

    private Specification<HistExamQuestion> buildQuestionSpecification(
            Integer examRound,
            String era,
            String category
    ) {
        Specification<HistExamQuestion> specification = (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("isDeleted"), 0);

        if (examRound != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("id").get("examRound"), examRound)
            );
        }

        if (hasText(era)) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("era")),
                            "%" + era.toLowerCase() + "%"
                    )
            );
        }

        if (hasText(category)) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("category")),
                            "%" + category.toLowerCase() + "%"
                    )
            );
        }

        return specification;
    }

    private MockExamStartResponse toStartResponse(
            HistMockExam mockExam,
            List<HistMockExamQuestion> questions
    ) {
        List<MockExamQuestionResponse> questionResponses = questions.stream()
                .map(q -> new MockExamQuestionResponse(
                        q.getQuestionOrder(),
                        q.getSelectedAnswer(),
                        toQuestionDetailResponse(q.getQuestion(), false)
                ))
                .toList();

        return new MockExamStartResponse(
                mockExam.getMockExamId(),
                mockExam.getTitle(),
                mockExam.getTotalQuestionCount(),
                mockExam.getStatus(),
                mockExam.getStartedAt(),
                questionResponses
        );
    }

    private MockExamResultResponse toResultResponse(
            HistMockExam mockExam,
            List<HistMockExamQuestion> questions
    ) {
        List<MockExamAnswerResultResponse> answerResponses = questions.stream()
                .map(q -> new MockExamAnswerResultResponse(
                        q.getQuestion().getSyntheticQuestionId(),
                        q.getQuestionOrder(),
                        q.getSelectedAnswer(),
                        q.getCorrectAnswer(),
                        q.isCorrect()
                ))
                .toList();

        return new MockExamResultResponse(
                mockExam.getMockExamId(),
                mockExam.getTitle(),
                mockExam.getTotalQuestionCount(),
                mockExam.getCorrectCount(),
                mockExam.getScore(),
                mockExam.getStatus(),
                mockExam.getStartedAt(),
                mockExam.getSubmittedAt(),
                answerResponses
        );
    }

    private QuestionDetailResponse toQuestionDetailResponse(
            HistExamQuestion question,
            boolean includeCorrectAnswer
    ) {
        return new QuestionDetailResponse(
                question.getSyntheticQuestionId(),
                question.getExamRound(),
                question.getQNo(),
                question.getQuestionText(),
                question.getPassage(),
                question.getPoint(),
                question.getEra(),
                question.getCategory(),
                List.of(
                        toOptionResponse(1, question.getChoice1(), question.getAnswer(), includeCorrectAnswer),
                        toOptionResponse(2, question.getChoice2(), question.getAnswer(), includeCorrectAnswer),
                        toOptionResponse(3, question.getChoice3(), question.getAnswer(), includeCorrectAnswer),
                        toOptionResponse(4, question.getChoice4(), question.getAnswer(), includeCorrectAnswer),
                        toOptionResponse(5, question.getChoice5(), question.getAnswer(), includeCorrectAnswer)
                )
        );
    }

    private QuestionOptionResponse toOptionResponse(
            Integer optionNo,
            String optionContent,
            Integer correctAnswer,
            boolean includeCorrectAnswer
    ) {
        return new QuestionOptionResponse(
                optionNo,
                optionNo,
                optionContent,
                includeCorrectAnswer ? optionNo.equals(correctAnswer) : null,
                null
        );
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    public PageResponse<MockExamSummaryResponse> getMockExamList(String email, int page, int size) {
        Long memberId = getCurrentMemberId(email);

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);

        Pageable pageable = PageRequest.of(safePage, safeSize);

        Page<HistMockExam> result = mockExamRepository.findByMemberIdOrderByStartedAtDesc(
                memberId,
                pageable
        );

        List<MockExamSummaryResponse> content = result.getContent().stream()
                .map(this::toSummaryResponse)
                .toList();

        return new PageResponse<>(
                content,
                result.getTotalElements(),
                result.getTotalPages(),
                result.getNumber(),
                result.getSize()
        );
    }

    private MockExamSummaryResponse toSummaryResponse(HistMockExam mockExam) {
        return new MockExamSummaryResponse(
                mockExam.getMockExamId(),
                mockExam.getTitle(),
                mockExam.getTotalQuestionCount(),
                mockExam.getCorrectCount(),
                mockExam.getScore(),
                mockExam.getStatus(),
                mockExam.getStartedAt(),
                mockExam.getSubmittedAt()
        );
    }
}