package com.gongsoop.question.service;

import com.gongsoop.global.exception.BusinessException;
import com.gongsoop.member.entity.Member;
import com.gongsoop.member.repository.MemberRepository;
import com.gongsoop.question.dto.request.SolveQuestionRequest;
import com.gongsoop.question.dto.response.*;
import com.gongsoop.question.entity.HistExamQuestion;
import com.gongsoop.question.entity.HistExamQuestionId;
import com.gongsoop.question.entity.HistSolveRecord;
import com.gongsoop.question.entity.HistWrongAnswer;
import com.gongsoop.question.repository.HistExamQuestionRepository;
import com.gongsoop.question.repository.HistSolveRecordRepository;
import com.gongsoop.question.repository.HistWrongAnswerRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class QuestionBankService {

    private final HistExamQuestionRepository histExamQuestionRepository;
    private final HistSolveRecordRepository histSolveRecordRepository;
    private final HistWrongAnswerRepository histWrongAnswerRepository;
    private final MemberRepository memberRepository;

    public QuestionBankService(
            HistExamQuestionRepository histExamQuestionRepository,
            HistSolveRecordRepository histSolveRecordRepository,
            HistWrongAnswerRepository histWrongAnswerRepository,
            MemberRepository memberRepository
    ) {
        this.histExamQuestionRepository = histExamQuestionRepository;
        this.histSolveRecordRepository = histSolveRecordRepository;
        this.histWrongAnswerRepository = histWrongAnswerRepository;
        this.memberRepository = memberRepository;
    }

    public PageResponse<QuestionSummaryResponse> getQuestions(
            Integer examRound,
            String era,
            String category,
            int page,
            int size
    ) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);

        Page<HistExamQuestion> result = histExamQuestionRepository.findAll(
                buildSearchCondition(examRound, era, category),
                PageRequest.of(safePage, safeSize)
        );

        List<QuestionSummaryResponse> content = result.getContent()
                .stream()
                .map(this::toSummary)
                .toList();

        return new PageResponse<>(
                content,
                result.getTotalElements(),
                result.getTotalPages(),
                result.getNumber(),
                result.getSize()
        );
    }

    public QuestionDetailResponse getQuestion(Long questionId) {
        HistExamQuestion question = getQuestionEntity(questionId);
        return toDetail(question, false);
    }

    @Transactional
    public SolveResultResponse solveQuestion(
            Long questionId,
            SolveQuestionRequest request,
            String email
    ) {
        Long memberId = getCurrentMemberId(email);
        HistExamQuestion question = getQuestionEntity(questionId);

        boolean isCorrect = question.getAnswer().equals(request.selectedOptionId());

        HistSolveRecord solveRecord = histSolveRecordRepository.save(
                HistSolveRecord.create(
                        memberId,
                        question,
                        request.selectedOptionId(),
                        isCorrect,
                        request.solveType()
                )
        );

        if (!isCorrect) {
            saveOrUpdateWrongAnswer(memberId, question, request.selectedOptionId());
        }

        return new SolveResultResponse(
                isCorrect,
                question.getAnswer(),
                buildExplanation(),
                solveRecord.getSolveRecordId()
        );
    }

    public List<QuestionDetailResponse> getRandomQuestions(
            int count,
            Integer examRound,
            String era,
            String category
    ) {
        int safeCount = Math.min(Math.max(count, 1), 20);

        List<HistExamQuestion> questions = histExamQuestionRepository.findAll(
                buildSearchCondition(examRound, era, category)
        );

        Collections.shuffle(questions);

        return questions.stream()
                .limit(safeCount)
                .map(question -> toDetail(question, false))
                .toList();
    }

    public PageResponse<WrongAnswerSummaryResponse> getWrongAnswers(
            String email,
            Boolean resolved,
            int page,
            int size
    ) {
        Long memberId = getCurrentMemberId(email);

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);

        PageRequest pageRequest = PageRequest.of(
                safePage,
                safeSize,
                Sort.by(Sort.Direction.DESC, "updatedAt")
        );

        Page<HistWrongAnswer> result;

        if (resolved == null) {
            result = histWrongAnswerRepository.findByMemberId(memberId, pageRequest);
        } else {
            result = histWrongAnswerRepository.findByMemberIdAndIsResolved(
                    memberId,
                    resolved ? "Y" : "N",
                    pageRequest
            );
        }

        List<WrongAnswerSummaryResponse> content = result.getContent()
                .stream()
                .map(this::toWrongAnswerSummary)
                .toList();

        return new PageResponse<>(
                content,
                result.getTotalElements(),
                result.getTotalPages(),
                result.getNumber(),
                result.getSize()
        );
    }

    @Transactional
    public SolveResultResponse retryWrongAnswer(
            Long wrongAnswerId,
            SolveQuestionRequest request,
            String email
    ) {
        Long memberId = getCurrentMemberId(email);

        HistWrongAnswer wrongAnswer = histWrongAnswerRepository
                .findByWrongAnswerIdAndMemberId(wrongAnswerId, memberId)
                .orElseThrow(() -> new BusinessException(
                        "WRONG_ANSWER_NOT_FOUND",
                        "오답노트를 찾을 수 없습니다",
                        HttpStatus.NOT_FOUND
                ));

        HistExamQuestion question = histExamQuestionRepository.findById(
                new HistExamQuestionId(wrongAnswer.getExamRound(), wrongAnswer.getQNo())
        ).orElseThrow(() -> new BusinessException(
                "QUESTION_NOT_FOUND",
                "문제를 찾을 수 없습니다",
                HttpStatus.NOT_FOUND
        ));

        boolean isCorrect = question.getAnswer().equals(request.selectedOptionId());

        HistSolveRecord solveRecord = histSolveRecordRepository.save(
                HistSolveRecord.create(
                        memberId,
                        question,
                        request.selectedOptionId(),
                        isCorrect,
                        "WRONG_RETRY"
                )
        );

        if (isCorrect) {
            wrongAnswer.resolve();
        } else {
            wrongAnswer.markWrong(request.selectedOptionId(), question.getAnswer());
        }

        return new SolveResultResponse(
                isCorrect,
                question.getAnswer(),
                buildExplanation(),
                solveRecord.getSolveRecordId()
        );
    }

    private void saveOrUpdateWrongAnswer(
            Long memberId,
            HistExamQuestion question,
            Integer selectedAnswer
    ) {
        histWrongAnswerRepository
                .findByMemberIdAndExamRoundAndQNo(
                        memberId,
                        question.getExamRound(),
                        question.getQNo()
                )
                .ifPresentOrElse(
                        wrongAnswer -> wrongAnswer.markWrong(selectedAnswer, question.getAnswer()),
                        () -> histWrongAnswerRepository.save(
                                HistWrongAnswer.create(memberId, question, selectedAnswer)
                        )
                );
    }

    private WrongAnswerSummaryResponse toWrongAnswerSummary(HistWrongAnswer wrongAnswer) {
        HistExamQuestion question = histExamQuestionRepository.findById(
                new HistExamQuestionId(wrongAnswer.getExamRound(), wrongAnswer.getQNo())
        ).orElseThrow(() -> new BusinessException(
                "QUESTION_NOT_FOUND",
                "문제를 찾을 수 없습니다",
                HttpStatus.NOT_FOUND
        ));

        return new WrongAnswerSummaryResponse(
                wrongAnswer.getWrongAnswerId(),
                toSummary(question),
                wrongAnswer.getWrongCount(),
                wrongAnswer.isResolved(),
                wrongAnswer.getLastSelectedAnswer(),
                wrongAnswer.getCorrectAnswer(),
                wrongAnswer.getCreatedAt()
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
                        HttpStatus.NOT_FOUND
                ));

        return member.getId();
    }

    private HistExamQuestion getQuestionEntity(Long questionId) {
        HistExamQuestionId id = decodeQuestionId(questionId);

        return histExamQuestionRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        "QUESTION_NOT_FOUND",
                        "문제를 찾을 수 없습니다",
                        HttpStatus.NOT_FOUND
                ));
    }

    private Specification<HistExamQuestion> buildSearchCondition(
            Integer examRound,
            String era,
            String category
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (examRound != null) {
                predicates.add(criteriaBuilder.equal(root.get("id").get("examRound"), examRound));
            }

            if (era != null && !era.isBlank()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("era")),
                        "%" + era.toLowerCase() + "%"
                ));
            }

            if (category != null && !category.isBlank()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("category")),
                        "%" + category.toLowerCase() + "%"
                ));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private QuestionSummaryResponse toSummary(HistExamQuestion question) {
        String qText = question.getQuestionText() == null
                ? ""
                : question.getQuestionText();

        String preview = qText.length() <= 45
                ? qText
                : qText.substring(0, 45) + "...";

        return new QuestionSummaryResponse(
                question.getSyntheticQuestionId(),
                question.getExamRound(),
                question.getQNo(),
                question.getEra(),
                question.getCategory(),
                question.getPoint(),
                preview
        );
    }

    private QuestionDetailResponse toDetail(HistExamQuestion question, boolean revealAnswer) {
        return new QuestionDetailResponse(
                question.getSyntheticQuestionId(),
                question.getExamRound(),
                question.getQNo(),
                question.getQuestionText(),
                question.getPassage(),
                question.getPoint(),
                question.getEra(),
                question.getCategory(),
                buildOptions(question, revealAnswer)
        );
    }

    private List<QuestionOptionResponse> buildOptions(HistExamQuestion question, boolean revealAnswer) {
        List<QuestionOptionResponse> options = new ArrayList<>();

        addOption(options, 1, question.getChoice1(), question.getAnswer(), revealAnswer);
        addOption(options, 2, question.getChoice2(), question.getAnswer(), revealAnswer);
        addOption(options, 3, question.getChoice3(), question.getAnswer(), revealAnswer);
        addOption(options, 4, question.getChoice4(), question.getAnswer(), revealAnswer);
        addOption(options, 5, question.getChoice5(), question.getAnswer(), revealAnswer);

        return options;
    }

    private void addOption(
            List<QuestionOptionResponse> options,
            int optionNo,
            String content,
            Integer answer,
            boolean revealAnswer
    ) {
        if (content == null || content.isBlank()) {
            return;
        }

        options.add(new QuestionOptionResponse(
                optionNo,
                optionNo,
                content,
                revealAnswer ? answer.equals(optionNo) : null,
                null
        ));
    }

    private String buildExplanation() {
        return "해설은 추후 AI 서버를 통해 생성될 예정입니다.";
    }

    private HistExamQuestionId decodeQuestionId(Long questionId) {
        if (questionId == null || questionId <= 0) {
            throw new BusinessException(
                    "INVALID_QUESTION_ID",
                    "문제 ID가 올바르지 않습니다",
                    HttpStatus.BAD_REQUEST
            );
        }

        int examRound = (int) (questionId / 1000);
        int qNo = (int) (questionId % 1000);

        if (examRound <= 0 || qNo <= 0) {
            throw new BusinessException(
                    "INVALID_QUESTION_ID",
                    "문제 ID가 올바르지 않습니다",
                    HttpStatus.BAD_REQUEST
            );
        }

        return new HistExamQuestionId(examRound, qNo);
    }
}