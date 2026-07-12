package com.gongsoop.question.service;

import com.gongsoop.global.exception.BusinessException;
import com.gongsoop.member.entity.Member;
import com.gongsoop.member.repository.MemberRepository;
import com.gongsoop.question.dto.request.SolveQuestionRequest;
import com.gongsoop.question.dto.response.PageResponse;
import com.gongsoop.question.dto.response.QuestionDetailResponse;
import com.gongsoop.question.dto.response.QuestionOptionResponse;
import com.gongsoop.question.dto.response.QuestionSummaryResponse;
import com.gongsoop.question.dto.response.SolveResultResponse;
import com.gongsoop.question.dto.response.WrongAnswerSummaryResponse;
import com.gongsoop.question.entity.HistExamQuestion;
import com.gongsoop.question.entity.HistExamQuestionId;
import com.gongsoop.question.entity.HistSolveRecord;
import com.gongsoop.question.entity.HistWrongAnswer;
import com.gongsoop.question.repository.HistExamQuestionRepository;
import com.gongsoop.question.repository.HistSolveRecordRepository;
import com.gongsoop.question.repository.HistWrongAnswerRepository;
import jakarta.persistence.criteria.Path;
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
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class QuestionBankService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final int MAX_RANDOM_QUESTION_COUNT = 30;

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

    /**
     * 문제 목록을 조회합니다.
     */
    public PageResponse<QuestionSummaryResponse> getQuestions(
            Integer examRound,
            String periodCode,
            String era,
            String category,
            int page,
            int size
    ) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(
                Math.max(size, 1),
                MAX_PAGE_SIZE
        );

        PageRequest pageRequest = PageRequest.of(
                safePage,
                safeSize,
                Sort.by(
                        Sort.Order.asc("id.examRound"),
                        Sort.Order.asc("id.qNo")
                )
        );

        Page<HistExamQuestion> result =
                histExamQuestionRepository.findAll(
                        buildSearchCondition(
                                examRound,
                                periodCode,
                                era,
                                category
                        ),
                        pageRequest
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

    /**
     * 단일 문제 상세 정보를 조회합니다.
     */
    public QuestionDetailResponse getQuestion(Long questionId) {
        HistExamQuestion question =
                findActiveQuestion(questionId);

        return toDetail(question, false);
    }

    /**
     * 문제를 채점하고 풀이 기록과 오답노트를 저장합니다.
     */
    @Transactional
    public SolveResultResponse solveQuestion(
            Long questionId,
            SolveQuestionRequest request,
            String email
    ) {
        Long memberId = getCurrentMemberId(email);
        HistExamQuestion question =
                findActiveQuestion(questionId);

        boolean isCorrect =
                question.getAnswer()
                        .equals(request.selectedOptionId());

        String solveType = normalizeSolveType(
                request.solveType()
        );

        HistSolveRecord solveRecord =
                histSolveRecordRepository.save(
                        HistSolveRecord.create(
                                memberId,
                                question,
                                request.selectedOptionId(),
                                isCorrect,
                                solveType
                        )
                );

        if (!isCorrect) {
            saveOrUpdateWrongAnswer(
                    memberId,
                    question,
                    request.selectedOptionId()
            );
        }

        return new SolveResultResponse(
                isCorrect,
                question.getAnswer(),
                buildExplanation(),
                solveRecord.getSolveRecordId()
        );
    }

    /**
     * 조건에 맞는 랜덤 문제를 조회합니다.
     */
    public List<QuestionDetailResponse> getRandomQuestions(
            int count,
            Integer examRound,
            String periodCode,
            String era,
            String category
    ) {
        int safeCount = Math.min(
                Math.max(count, 1),
                MAX_RANDOM_QUESTION_COUNT
        );

        List<HistExamQuestion> questions =
                new ArrayList<>(
                        histExamQuestionRepository.findAll(
                                buildSearchCondition(
                                        examRound,
                                        periodCode,
                                        era,
                                        category
                                )
                        )
                );

        Collections.shuffle(questions);

        return questions.stream()
                .limit(safeCount)
                .map(question -> toDetail(question, false))
                .toList();
    }

    /**
     * 사용자의 오답노트 목록을 조회합니다.
     */
    public PageResponse<WrongAnswerSummaryResponse> getWrongAnswers(
            String email,
            Boolean resolved,
            int page,
            int size
    ) {
        Long memberId = getCurrentMemberId(email);

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(
                Math.max(size, 1),
                MAX_PAGE_SIZE
        );

        PageRequest pageRequest = PageRequest.of(
                safePage,
                safeSize,
                Sort.by(
                        Sort.Direction.DESC,
                        "updatedAt"
                )
        );

        Page<HistWrongAnswer> result;

        if (resolved == null) {
            result =
                    histWrongAnswerRepository.findByMemberId(
                            memberId,
                            pageRequest
                    );
        } else {
            result =
                    histWrongAnswerRepository
                            .findByMemberIdAndIsResolved(
                                    memberId,
                                    resolved ? "Y" : "N",
                                    pageRequest
                            );
        }

        List<WrongAnswerSummaryResponse> content =
                result.getContent()
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

    /**
     * 오답노트 문제를 다시 채점합니다.
     */
    @Transactional
    public SolveResultResponse retryWrongAnswer(
            Long wrongAnswerId,
            SolveQuestionRequest request,
            String email
    ) {
        Long memberId = getCurrentMemberId(email);

        HistWrongAnswer wrongAnswer =
                histWrongAnswerRepository
                        .findByWrongAnswerIdAndMemberId(
                                wrongAnswerId,
                                memberId
                        )
                        .orElseThrow(() ->
                                new BusinessException(
                                        "WRONG_ANSWER_NOT_FOUND",
                                        "오답노트를 찾을 수 없습니다",
                                        HttpStatus.NOT_FOUND
                                )
                        );

        HistExamQuestion question =
                histExamQuestionRepository.findById(
                                new HistExamQuestionId(
                                        wrongAnswer.getExamRound(),
                                        wrongAnswer.getQNo()
                                )
                        )
                        .orElseThrow(() ->
                                new BusinessException(
                                        "QUESTION_NOT_FOUND",
                                        "문제를 찾을 수 없습니다",
                                        HttpStatus.NOT_FOUND
                                )
                        );

        if (Boolean.TRUE.equals(question.isDeleted())) {
            throw new BusinessException(
                    "QUESTION_NOT_FOUND",
                    "삭제된 문제입니다",
                    HttpStatus.NOT_FOUND
            );
        }

        boolean isCorrect =
                question.getAnswer()
                        .equals(request.selectedOptionId());

        HistSolveRecord solveRecord =
                histSolveRecordRepository.save(
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
            wrongAnswer.markWrong(
                    request.selectedOptionId(),
                    question.getAnswer()
            );
        }

        return new SolveResultResponse(
                isCorrect,
                question.getAnswer(),
                buildExplanation(),
                solveRecord.getSolveRecordId()
        );
    }

    /**
     * 오답노트가 존재하면 갱신하고,
     * 존재하지 않으면 새로 생성합니다.
     */
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
                        wrongAnswer ->
                                wrongAnswer.markWrong(
                                        selectedAnswer,
                                        question.getAnswer()
                                ),
                        () ->
                                histWrongAnswerRepository.save(
                                        HistWrongAnswer.create(
                                                memberId,
                                                question,
                                                selectedAnswer
                                        )
                                )
                );
    }

    /**
     * 오답노트 엔티티를 화면용 DTO로 변환합니다.
     */
    private WrongAnswerSummaryResponse toWrongAnswerSummary(
            HistWrongAnswer wrongAnswer
    ) {
        HistExamQuestion question =
                histExamQuestionRepository.findById(
                                new HistExamQuestionId(
                                        wrongAnswer.getExamRound(),
                                        wrongAnswer.getQNo()
                                )
                        )
                        .orElseThrow(() ->
                                new BusinessException(
                                        "QUESTION_NOT_FOUND",
                                        "문제를 찾을 수 없습니다",
                                        HttpStatus.NOT_FOUND
                                )
                        );

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

    /**
     * 로그인한 회원의 식별자를 조회합니다.
     */
    private Long getCurrentMemberId(String email) {
        if (email == null || email.isBlank()) {
            throw new BusinessException(
                    "UNAUTHORIZED",
                    "로그인이 필요합니다",
                    HttpStatus.UNAUTHORIZED
            );
        }

        Member member = memberRepository.findByEmail(email)
                .filter(foundMember ->
                        !foundMember.isDeleted()
                )
                .orElseThrow(() ->
                        new BusinessException(
                                "MEMBER_NOT_FOUND",
                                "회원 정보를 찾을 수 없습니다",
                                HttpStatus.NOT_FOUND
                        )
                );

        return member.getId();
    }

    /**
     * 문제 검색 조건을 생성합니다.
     */
    private Specification<HistExamQuestion> buildSearchCondition(
            Integer examRound,
            String periodCode,
            String era,
            String category
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(
                    criteriaBuilder.equal(
                            root.get("isDeleted"),
                            0
                    )
            );

            if (examRound != null) {
                predicates.add(
                        criteriaBuilder.equal(
                                root.get("id").get("examRound"),
                                examRound
                        )
                );
            }

            if (
                    periodCode != null &&
                            !periodCode.isBlank()
            ) {
                predicates.add(
                        buildPeriodPredicate(
                                root.<String>get("era"),
                                periodCode,
                                criteriaBuilder
                        )
                );
            }

            if (era != null && !era.isBlank()) {
                predicates.add(
                        criteriaBuilder.like(
                                criteriaBuilder.lower(
                                        root.get("era")
                                ),
                                "%" + era.trim()
                                        .toLowerCase(Locale.ROOT) + "%"
                        )
                );
            }

            if (
                    category != null &&
                            !category.isBlank()
            ) {
                predicates.add(
                        criteriaBuilder.like(
                                criteriaBuilder.lower(
                                        root.get("category")
                                ),
                                "%" + category.trim()
                                        .toLowerCase(Locale.ROOT) + "%"
                        )
                );
            }

            return criteriaBuilder.and(
                    predicates.toArray(
                            new Predicate[0]
                    )
            );
        };
    }

    /**
     * 프론트 시대 코드를 DB ERA 컬럼 조건으로 변환합니다.
     *
     * PREHISTORY:
     * - 선사
     * - 선사(고조선)
     * - 선사(신석기)
     * - 선사(청동기)
     *
     * THREE_KINGDOMS:
     * - 고대
     * - 고대(삼국)
     * - 고대(고구려)
     * - 고대(백제)
     * - 고대(신라)
     * - 고대(가야)
     * - 고대(발해)
     *
     * GORYEO:
     * - 고려
     * - 고대~고려
     *
     * JOSEON:
     * - 조선
     * - 조선전기
     * - 조선후기
     *
     * MODERN:
     * - 근대
     * - 근대(개항기)
     * - 근대(대한제국)
     * - 일제
     * - 일제강점기
     * - 현대
     * - 근대~현대
     */
    private Predicate buildPeriodPredicate(
            Path<String> eraPath,
            String periodCode,
            jakarta.persistence.criteria.CriteriaBuilder criteriaBuilder
    ) {
        String normalizedPeriodCode =
                periodCode.trim()
                        .toUpperCase(Locale.ROOT);

        return switch (normalizedPeriodCode) {
            case "PREHISTORY" ->
                    criteriaBuilder.like(
                            eraPath,
                            "선사%"
                    );

            case "THREE_KINGDOMS" ->
                    criteriaBuilder.like(
                            eraPath,
                            "고대%"
                    );

            case "GORYEO" ->
                    criteriaBuilder.or(
                            criteriaBuilder.like(
                                    eraPath,
                                    "고려%"
                            ),
                            criteriaBuilder.like(
                                    eraPath,
                                    "%~고려%"
                            )
                    );

            case "JOSEON" ->
                    criteriaBuilder.like(
                            eraPath,
                            "조선%"
                    );

            case "MODERN" ->
                    criteriaBuilder.or(
                            criteriaBuilder.like(
                                    eraPath,
                                    "근대%"
                            ),
                            criteriaBuilder.like(
                                    eraPath,
                                    "일제%"
                            ),
                            criteriaBuilder.like(
                                    eraPath,
                                    "현대%"
                            )
                    );

            default ->
                    throw new BusinessException(
                            "INVALID_PERIOD_CODE",
                            "시대 코드가 올바르지 않습니다",
                            HttpStatus.BAD_REQUEST
                    );
        };
    }

    /**
     * 문제 목록 DTO를 생성합니다.
     */
    private QuestionSummaryResponse toSummary(
            HistExamQuestion question
    ) {
        String questionText =
                question.getQuestionText() == null
                        ? ""
                        : question.getQuestionText();

        String preview =
                questionText.length() <= 45
                        ? questionText
                        : questionText.substring(0, 45) + "...";

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

    /**
     * 문제 상세 DTO를 생성합니다.
     */
    private QuestionDetailResponse toDetail(
            HistExamQuestion question,
            boolean revealAnswer
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
                buildOptions(question, revealAnswer)
        );
    }

    /**
     * 문제의 보기를 생성합니다.
     */
    private List<QuestionOptionResponse> buildOptions(
            HistExamQuestion question,
            boolean revealAnswer
    ) {
        List<QuestionOptionResponse> options =
                new ArrayList<>();

        addOption(
                options,
                1,
                question.getChoice1(),
                question.getAnswer(),
                revealAnswer
        );

        addOption(
                options,
                2,
                question.getChoice2(),
                question.getAnswer(),
                revealAnswer
        );

        addOption(
                options,
                3,
                question.getChoice3(),
                question.getAnswer(),
                revealAnswer
        );

        addOption(
                options,
                4,
                question.getChoice4(),
                question.getAnswer(),
                revealAnswer
        );

        addOption(
                options,
                5,
                question.getChoice5(),
                question.getAnswer(),
                revealAnswer
        );

        return options;
    }

    /**
     * 내용이 존재하는 보기만 응답 목록에 추가합니다.
     */
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

        Boolean isCorrect = null;

        if (revealAnswer) {
            isCorrect = answer != null &&
                    answer.equals(optionNo);
        }

        options.add(
                new QuestionOptionResponse(
                        optionNo,
                        optionNo,
                        content,
                        isCorrect,
                        null
                )
        );
    }

    /**
     * 현재는 임시 해설을 반환합니다.
     *
     * 이후 AI 해설 API와 연결할 예정입니다.
     */
    private String buildExplanation() {
        return "해설은 추후 AI 서버를 통해 생성될 예정입니다.";
    }

    /**
     * 삭제되지 않은 문제를 조회합니다.
     */
    private HistExamQuestion findActiveQuestion(
            Long questionId
    ) {
        HistExamQuestionId id =
                decodeQuestionId(questionId);

        HistExamQuestion question =
                histExamQuestionRepository.findById(id)
                        .orElseThrow(() ->
                                new BusinessException(
                                        "QUESTION_NOT_FOUND",
                                        "문제를 찾을 수 없습니다",
                                        HttpStatus.NOT_FOUND
                                )
                        );

        if (Boolean.TRUE.equals(question.isDeleted())) {
            throw new BusinessException(
                    "QUESTION_NOT_FOUND",
                    "문제를 찾을 수 없습니다",
                    HttpStatus.NOT_FOUND
            );
        }

        return question;
    }

    /**
     * 합성 문제 ID를 복합키로 변환합니다.
     *
     * 예:
     * 57001
     * → 57회 1번
     */
    private HistExamQuestionId decodeQuestionId(
            Long questionId
    ) {
        if (
                questionId == null ||
                        questionId <= 0
        ) {
            throw new BusinessException(
                    "INVALID_QUESTION_ID",
                    "문제 ID가 올바르지 않습니다",
                    HttpStatus.BAD_REQUEST
            );
        }

        int examRound =
                (int) (questionId / 1000);

        int questionNumber =
                (int) (questionId % 1000);

        if (
                examRound <= 0 ||
                        questionNumber <= 0
        ) {
            throw new BusinessException(
                    "INVALID_QUESTION_ID",
                    "문제 ID가 올바르지 않습니다",
                    HttpStatus.BAD_REQUEST
            );
        }

        return new HistExamQuestionId(
                examRound,
                questionNumber
        );
    }

    /**
     * 잘못된 solveType이 DB에 저장되지 않도록 제한합니다.
     */
    private String normalizeSolveType(
            String solveType
    ) {
        if (
                solveType == null ||
                        solveType.isBlank()
        ) {
            return "PERIOD";
        }

        String normalized =
                solveType.trim()
                        .toUpperCase(Locale.ROOT);

        return switch (normalized) {
            case "PERIOD",
                 "RANDOM",
                 "MOCK",
                 "WRONG_RETRY" -> normalized;

            default ->
                    throw new BusinessException(
                            "INVALID_SOLVE_TYPE",
                            "풀이 유형이 올바르지 않습니다",
                            HttpStatus.BAD_REQUEST
                    );
        };
    }
}