package com.gongsoop.admin.service;

import com.gongsoop.admin.dto.request.AdminQuestionCreateRequest;
import com.gongsoop.admin.dto.request.AdminQuestionUpdateRequest;
import com.gongsoop.admin.dto.request.UpdateMemberDeleteStatusRequest;
import com.gongsoop.admin.dto.request.UpdateMemberRoleRequest;
import com.gongsoop.admin.dto.response.AdminDashboardResponse;
import com.gongsoop.admin.dto.response.AdminMemberDetailResponse;
import com.gongsoop.admin.dto.response.AdminMemberSummaryResponse;
import com.gongsoop.admin.dto.response.AdminQuestionChoiceResponse;
import com.gongsoop.admin.dto.response.AdminQuestionDetailResponse;
import com.gongsoop.admin.dto.response.AdminQuestionSummaryResponse;
import com.gongsoop.global.exception.BusinessException;
import com.gongsoop.question.dto.response.PageResponse;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class AdminService {

    private final JdbcTemplate jdbcTemplate;

    public AdminService(
            JdbcTemplate jdbcTemplate
    ) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 관리자 대시보드 통계를 조회합니다.
     *
     * MEMBERS는 DA2610.SF_USER를 가리키는
     * Oracle 시노님이므로 기존 이름을 유지합니다.
     */
    public AdminDashboardResponse getDashboard(
            String email
    ) {
        validateAdmin(email);

        Long totalMemberCount =
                count(
                        """
                        SELECT COUNT(*)
                        FROM MEMBERS
                        """
                );

        Long activeMemberCount =
                count(
                        """
                        SELECT COUNT(*)
                        FROM MEMBERS
                        WHERE IS_DELETED = 0
                        """
                );

        Long deletedMemberCount =
                count(
                        """
                        SELECT COUNT(*)
                        FROM MEMBERS
                        WHERE IS_DELETED = 1
                        """
                );

        Long histQuestionCount =
                count(
                        """
                        SELECT COUNT(*)
                        FROM HIST_EXAM_QUESTIONS
                        WHERE IS_DELETED = 0
                        """
                );

        Long histSolveRecordCount =
                count(
                        """
                        SELECT COUNT(*)
                        FROM HIST_SOLVE_RECORDS
                        """
                );

        Long mockExamCount =
                count(
                        """
                        SELECT COUNT(*)
                        FROM HIST_MOCK_EXAMS
                        """
                );

        Long aiGeneratedQuestionSetCount =
                count(
                        """
                        SELECT COUNT(*)
                        FROM AI_GENERATED_QUESTION_SETS
                        """
                );

        Long aiChatSessionCount =
                count(
                        """
                        SELECT COUNT(*)
                        FROM AI_CHAT_SESSIONS
                        """
                );

        return new AdminDashboardResponse(
                totalMemberCount,
                activeMemberCount,
                deletedMemberCount,
                histQuestionCount,
                histSolveRecordCount,
                mockExamCount,
                aiGeneratedQuestionSetCount,
                aiChatSessionCount
        );
    }

    /**
     * 관리자 회원 목록을 조회합니다.
     */
    public PageResponse<AdminMemberSummaryResponse> getMembers(
            String email,
            int page,
            int size,
            String keyword,
            String userRole,
            Boolean isDeleted
    ) {
        validateAdmin(email);

        int safePage =
                Math.max(
                        page,
                        0
                );

        int safeSize =
                Math.min(
                        Math.max(
                                size,
                                1
                        ),
                        50
                );

        int offset =
                safePage * safeSize;

        StringBuilder where =
                new StringBuilder(
                        " WHERE 1 = 1 "
                );

        List<Object> params =
                new ArrayList<>();

        if (hasText(keyword)) {
            where.append(
                    """
                     AND (
                         LOWER(NAME) LIKE LOWER(?)
                         OR LOWER(NICKNAME) LIKE LOWER(?)
                         OR LOWER(EMAIL) LIKE LOWER(?)
                     )
                    """
            );

            String likeKeyword =
                    "%"
                            + keyword.trim()
                            + "%";

            params.add(
                    likeKeyword
            );

            params.add(
                    likeKeyword
            );

            params.add(
                    likeKeyword
            );
        }

        if (hasText(userRole)) {
            where.append(
                    " AND USER_ROLE = ? "
            );

            params.add(
                    userRole
                            .trim()
                            .toUpperCase(
                                    Locale.ROOT
                            )
            );
        }

        if (isDeleted != null) {
            where.append(
                    " AND IS_DELETED = ? "
            );

            params.add(
                    Boolean.TRUE.equals(
                            isDeleted
                    )
                            ? 1
                            : 0
            );
        }

        Long totalElements =
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM MEMBERS "
                                + where,
                        Long.class,
                        params.toArray()
                );

        List<Object> listParams =
                new ArrayList<>(
                        params
                );

        listParams.add(
                offset
        );

        listParams.add(
                safeSize
        );

        List<AdminMemberSummaryResponse> content =
                jdbcTemplate.query(
                        """
                        SELECT
                            USER_ID,
                            NAME,
                            NICKNAME,
                            BIRTHDATE,
                            EMAIL,
                            USER_ROLE,
                            CHARACTER_ID,
                            IS_DELETED
                        FROM MEMBERS
                        """
                                + where
                                + """
                        ORDER BY USER_ID DESC
                        OFFSET ? ROWS
                        FETCH NEXT ? ROWS ONLY
                        """,
                        (
                                resultSet,
                                rowNumber
                        ) ->
                                new AdminMemberSummaryResponse(
                                        getLong(
                                                resultSet,
                                                "USER_ID"
                                        ),
                                        resultSet.getString(
                                                "NAME"
                                        ),
                                        resultSet.getString(
                                                "NICKNAME"
                                        ),
                                        toLocalDate(
                                                resultSet.getDate(
                                                        "BIRTHDATE"
                                                )
                                        ),
                                        resultSet.getString(
                                                "EMAIL"
                                        ),
                                        resultSet.getString(
                                                "USER_ROLE"
                                        ),
                                        getInteger(
                                                resultSet,
                                                "CHARACTER_ID"
                                        ),
                                        getBooleanByNumber(
                                                resultSet,
                                                "IS_DELETED"
                                        )
                                ),
                        listParams.toArray()
                );

        int totalPages =
                totalElements == null
                        || totalElements == 0
                        ? 0
                        : (int) Math.ceil(
                        (double) totalElements
                                / safeSize
                );

        return new PageResponse<>(
                content,
                totalElements == null
                        ? 0L
                        : totalElements,
                totalPages,
                safePage,
                safeSize
        );
    }

    /**
     * 관리자 회원 상세 정보를 조회합니다.
     */
    public AdminMemberDetailResponse getMemberDetail(
            String email,
            Long memberId
    ) {
        validateAdmin(email);

        AdminMemberSummaryResponse member =
                jdbcTemplate.query(
                        """
                        SELECT
                            USER_ID,
                            NAME,
                            NICKNAME,
                            BIRTHDATE,
                            EMAIL,
                            USER_ROLE,
                            CHARACTER_ID,
                            IS_DELETED
                        FROM MEMBERS
                        WHERE USER_ID = ?
                        """,
                        resultSet -> {
                            if (!resultSet.next()) {
                                throw new BusinessException(
                                        "MEMBER_NOT_FOUND",
                                        "회원을 찾을 수 없습니다",
                                        HttpStatus.NOT_FOUND
                                );
                            }

                            return new AdminMemberSummaryResponse(
                                    getLong(
                                            resultSet,
                                            "USER_ID"
                                    ),
                                    resultSet.getString(
                                            "NAME"
                                    ),
                                    resultSet.getString(
                                            "NICKNAME"
                                    ),
                                    toLocalDate(
                                            resultSet.getDate(
                                                    "BIRTHDATE"
                                            )
                                    ),
                                    resultSet.getString(
                                            "EMAIL"
                                    ),
                                    resultSet.getString(
                                            "USER_ROLE"
                                    ),
                                    getInteger(
                                            resultSet,
                                            "CHARACTER_ID"
                                    ),
                                    getBooleanByNumber(
                                            resultSet,
                                            "IS_DELETED"
                                    )
                            );
                        },
                        memberId
                );

        Long histSolvedCount =
                count(
                        """
                        SELECT COUNT(*)
                        FROM HIST_SOLVE_RECORDS
                        WHERE MEMBER_ID = ?
                        """,
                        memberId
                );

        Long aiGeneratedSolvedCount =
                count(
                        """
                        SELECT COUNT(*)
                        FROM AI_GENERATED_QUESTION_SOLVE_RECORDS
                        WHERE MEMBER_ID = ?
                        """,
                        memberId
                );

        Long mockExamCount =
                count(
                        """
                        SELECT COUNT(*)
                        FROM HIST_MOCK_EXAMS
                        WHERE MEMBER_ID = ?
                        """,
                        memberId
                );

        Long aiGeneratedQuestionSetCount =
                count(
                        """
                        SELECT COUNT(*)
                        FROM AI_GENERATED_QUESTION_SETS
                        WHERE MEMBER_ID = ?
                        """,
                        memberId
                );

        Long aiChatSessionCount =
                count(
                        """
                        SELECT COUNT(*)
                        FROM AI_CHAT_SESSIONS
                        WHERE MEMBER_ID = ?
                        """,
                        memberId
                );

        return new AdminMemberDetailResponse(
                member.memberId(),
                member.name(),
                member.nickname(),
                member.birthdate(),
                member.email(),
                member.userRole(),
                member.characterId(),
                member.isDeleted(),
                histSolvedCount,
                aiGeneratedSolvedCount,
                mockExamCount,
                aiGeneratedQuestionSetCount,
                aiChatSessionCount
        );
    }

    /**
     * 회원 권한을 변경합니다.
     */
    @Transactional
    public AdminMemberSummaryResponse updateMemberRole(
            String email,
            Long memberId,
            UpdateMemberRoleRequest request
    ) {
        validateAdmin(email);

        int updatedCount =
                jdbcTemplate.update(
                        """
                        UPDATE MEMBERS
                        SET USER_ROLE = ?
                        WHERE USER_ID = ?
                        """,
                        request.userRole()
                                .trim()
                                .toUpperCase(
                                        Locale.ROOT
                                ),
                        memberId
                );

        if (updatedCount == 0) {
            throw new BusinessException(
                    "MEMBER_NOT_FOUND",
                    "회원을 찾을 수 없습니다",
                    HttpStatus.NOT_FOUND
            );
        }

        return getMemberSummaryById(
                memberId
        );
    }

    /**
     * 회원 삭제 상태를 변경합니다.
     */
    @Transactional
    public AdminMemberSummaryResponse updateMemberDeleteStatus(
            String email,
            Long memberId,
            UpdateMemberDeleteStatusRequest request
    ) {
        validateAdmin(email);

        int updatedCount =
                jdbcTemplate.update(
                        """
                        UPDATE MEMBERS
                        SET IS_DELETED = ?
                        WHERE USER_ID = ?
                        """,
                        Boolean.TRUE.equals(
                                request.isDeleted()
                        )
                                ? 1
                                : 0,
                        memberId
                );

        if (updatedCount == 0) {
            throw new BusinessException(
                    "MEMBER_NOT_FOUND",
                    "회원을 찾을 수 없습니다",
                    HttpStatus.NOT_FOUND
            );
        }

        return getMemberSummaryById(
                memberId
        );
    }

    /**
     * 관리자 문제 목록을 조회합니다.
     */
    public PageResponse<AdminQuestionSummaryResponse> getQuestions(
            String email,
            int page,
            int size,
            Integer examRound,
            String era,
            String category,
            String keyword,
            Boolean isDeleted
    ) {
        validateAdmin(email);

        int safePage =
                Math.max(
                        page,
                        0
                );

        int safeSize =
                Math.min(
                        Math.max(
                                size,
                                1
                        ),
                        50
                );

        int offset =
                safePage * safeSize;

        StringBuilder where =
                new StringBuilder(
                        " WHERE 1 = 1 "
                );

        List<Object> params =
                new ArrayList<>();

        if (examRound != null) {
            where.append(
                    " AND EXAM_ROUND = ? "
            );

            params.add(
                    examRound
            );
        }

        if (hasText(era)) {
            where.append(
                    " AND LOWER(ERA) LIKE LOWER(?) "
            );

            params.add(
                    "%"
                            + era.trim()
                            + "%"
            );
        }

        if (hasText(category)) {
            where.append(
                    " AND LOWER(CATEGORY) LIKE LOWER(?) "
            );

            params.add(
                    "%"
                            + category.trim()
                            + "%"
            );
        }

        if (hasText(keyword)) {
            where.append(
                    " AND LOWER(Q_TEXT) LIKE LOWER(?) "
            );

            params.add(
                    "%"
                            + keyword.trim()
                            + "%"
            );
        }

        if (isDeleted != null) {
            where.append(
                    " AND IS_DELETED = ? "
            );

            params.add(
                    Boolean.TRUE.equals(
                            isDeleted
                    )
                            ? 1
                            : 0
            );
        }

        Long totalElements =
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM HIST_EXAM_QUESTIONS "
                                + where,
                        Long.class,
                        params.toArray()
                );

        List<Object> listParams =
                new ArrayList<>(
                        params
                );

        listParams.add(
                offset
        );

        listParams.add(
                safeSize
        );

        List<AdminQuestionSummaryResponse> content =
                jdbcTemplate.query(
                        """
                        SELECT
                            EXAM_ROUND,
                            Q_NO,
                            ERA,
                            CATEGORY,
                            POINT,
                            Q_TEXT,
                            IS_DELETED
                        FROM HIST_EXAM_QUESTIONS
                        """
                                + where
                                + """
                        ORDER BY EXAM_ROUND DESC,
                                 Q_NO ASC
                        OFFSET ? ROWS
                        FETCH NEXT ? ROWS ONLY
                        """,
                        (
                                resultSet,
                                rowNumber
                        ) -> {
                            Integer round =
                                    getInteger(
                                            resultSet,
                                            "EXAM_ROUND"
                                    );

                            Integer qNo =
                                    getInteger(
                                            resultSet,
                                            "Q_NO"
                                    );

                            return new AdminQuestionSummaryResponse(
                                    round * 1000L + qNo,
                                    round,
                                    qNo,
                                    resultSet.getString(
                                            "ERA"
                                    ),
                                    resultSet.getString(
                                            "CATEGORY"
                                    ),
                                    getInteger(
                                            resultSet,
                                            "POINT"
                                    ),
                                    preview(
                                            resultSet.getString(
                                                    "Q_TEXT"
                                            ),
                                            80
                                    ),
                                    getBooleanByNumber(
                                            resultSet,
                                            "IS_DELETED"
                                    )
                            );
                        },
                        listParams.toArray()
                );

        int totalPages =
                totalElements == null
                        || totalElements == 0
                        ? 0
                        : (int) Math.ceil(
                        (double) totalElements
                                / safeSize
                );

        return new PageResponse<>(
                content,
                totalElements == null
                        ? 0L
                        : totalElements,
                totalPages,
                safePage,
                safeSize
        );
    }

    /**
     * 관리자 문제 상세 정보를 조회합니다.
     */
    public AdminQuestionDetailResponse getQuestionDetail(
            String email,
            Long questionId
    ) {
        validateAdmin(email);

        QuestionKey key =
                parseQuestionId(
                        questionId
                );

        return getQuestionDetailByKey(
                key.examRound(),
                key.qNo()
        );
    }

    /**
     * 관리자 문제를 생성합니다.
     */
    @Transactional
    public AdminQuestionDetailResponse createQuestion(
            String email,
            AdminQuestionCreateRequest request
    ) {
        validateAdmin(email);

        Long exists =
                count(
                        """
                        SELECT COUNT(*)
                        FROM HIST_EXAM_QUESTIONS
                        WHERE EXAM_ROUND = ?
                          AND Q_NO = ?
                        """,
                        request.examRound(),
                        request.qNo()
                );

        if (exists > 0) {
            throw new BusinessException(
                    "QUESTION_ALREADY_EXISTS",
                    "이미 같은 회차와 번호의 문제가 존재합니다",
                    HttpStatus.CONFLICT
            );
        }

        jdbcTemplate.update(
                """
                INSERT INTO HIST_EXAM_QUESTIONS (
                    EXAM_ROUND,
                    Q_NO,
                    Q_TEXT,
                    POINT,
                    CHOICE1,
                    CHOICE2,
                    CHOICE3,
                    CHOICE4,
                    CHOICE5,
                    ANSWER,
                    Q_PASSAGE,
                    ERA,
                    CATEGORY,
                    IS_DELETED
                )
                VALUES (
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    0
                )
                """,
                request.examRound(),
                request.qNo(),
                request.questionText(),
                request.point(),
                request.choice1(),
                request.choice2(),
                request.choice3(),
                request.choice4(),
                request.choice5(),
                request.answer(),
                request.passage(),
                request.era(),
                request.category()
        );

        return getQuestionDetailByKey(
                request.examRound(),
                request.qNo()
        );
    }

    /**
     * 관리자 문제를 수정합니다.
     */
    @Transactional
    public AdminQuestionDetailResponse updateQuestion(
            String email,
            Long questionId,
            AdminQuestionUpdateRequest request
    ) {
        validateAdmin(email);

        QuestionKey key =
                parseQuestionId(
                        questionId
                );

        int updatedCount =
                jdbcTemplate.update(
                        """
                        UPDATE HIST_EXAM_QUESTIONS
                        SET
                            Q_TEXT = ?,
                            POINT = ?,
                            CHOICE1 = ?,
                            CHOICE2 = ?,
                            CHOICE3 = ?,
                            CHOICE4 = ?,
                            CHOICE5 = ?,
                            ANSWER = ?,
                            Q_PASSAGE = ?,
                            ERA = ?,
                            CATEGORY = ?,
                            IS_DELETED = ?
                        WHERE EXAM_ROUND = ?
                          AND Q_NO = ?
                        """,
                        request.questionText(),
                        request.point(),
                        request.choice1(),
                        request.choice2(),
                        request.choice3(),
                        request.choice4(),
                        request.choice5(),
                        request.answer(),
                        request.passage(),
                        request.era(),
                        request.category(),
                        Boolean.TRUE.equals(
                                request.isDeleted()
                        )
                                ? 1
                                : 0,
                        key.examRound(),
                        key.qNo()
                );

        if (updatedCount == 0) {
            throw new BusinessException(
                    "QUESTION_NOT_FOUND",
                    "문제를 찾을 수 없습니다",
                    HttpStatus.NOT_FOUND
            );
        }

        return getQuestionDetailByKey(
                key.examRound(),
                key.qNo()
        );
    }

    /**
     * 관리자 문제를 논리 삭제합니다.
     */
    @Transactional
    public void deleteQuestion(
            String email,
            Long questionId
    ) {
        validateAdmin(email);

        QuestionKey key =
                parseQuestionId(
                        questionId
                );

        int updatedCount =
                jdbcTemplate.update(
                        """
                        UPDATE HIST_EXAM_QUESTIONS
                        SET IS_DELETED = 1
                        WHERE EXAM_ROUND = ?
                          AND Q_NO = ?
                        """,
                        key.examRound(),
                        key.qNo()
                );

        if (updatedCount == 0) {
            throw new BusinessException(
                    "QUESTION_NOT_FOUND",
                    "문제를 찾을 수 없습니다",
                    HttpStatus.NOT_FOUND
            );
        }
    }

    /**
     * 관리자 권한을 확인합니다.
     */
    private void validateAdmin(
            String email
    ) {
        if (!hasText(email)) {
            throw new BusinessException(
                    "UNAUTHORIZED",
                    "로그인이 필요합니다",
                    HttpStatus.UNAUTHORIZED
            );
        }

        Long adminCount =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM MEMBERS
                        WHERE EMAIL = ?
                          AND USER_ROLE = 'ADMIN'
                          AND IS_DELETED = 0
                        """,
                        Long.class,
                        email
                );

        if (
                adminCount == null
                        || adminCount == 0
        ) {
            throw new BusinessException(
                    "ADMIN_FORBIDDEN",
                    "관리자만 접근할 수 있습니다",
                    HttpStatus.FORBIDDEN
            );
        }
    }

    /**
     * 회원 한 명의 공통 요약 정보를 조회합니다.
     */
    private AdminMemberSummaryResponse getMemberSummaryById(
            Long memberId
    ) {
        return jdbcTemplate.query(
                """
                SELECT
                    USER_ID,
                    NAME,
                    NICKNAME,
                    BIRTHDATE,
                    EMAIL,
                    USER_ROLE,
                    CHARACTER_ID,
                    IS_DELETED
                FROM MEMBERS
                WHERE USER_ID = ?
                """,
                resultSet -> {
                    if (!resultSet.next()) {
                        throw new BusinessException(
                                "MEMBER_NOT_FOUND",
                                "회원을 찾을 수 없습니다",
                                HttpStatus.NOT_FOUND
                        );
                    }

                    return new AdminMemberSummaryResponse(
                            getLong(
                                    resultSet,
                                    "USER_ID"
                            ),
                            resultSet.getString(
                                    "NAME"
                            ),
                            resultSet.getString(
                                    "NICKNAME"
                            ),
                            toLocalDate(
                                    resultSet.getDate(
                                            "BIRTHDATE"
                                    )
                            ),
                            resultSet.getString(
                                    "EMAIL"
                            ),
                            resultSet.getString(
                                    "USER_ROLE"
                            ),
                            getInteger(
                                    resultSet,
                                    "CHARACTER_ID"
                            ),
                            getBooleanByNumber(
                                    resultSet,
                                    "IS_DELETED"
                            )
                    );
                },
                memberId
        );
    }

    /**
     * 관리자 문제 상세 정보를 복합키로 조회합니다.
     */
    private AdminQuestionDetailResponse getQuestionDetailByKey(
            Integer examRound,
            Integer qNo
    ) {
        return jdbcTemplate.query(
                """
                SELECT
                    EXAM_ROUND,
                    Q_NO,
                    Q_TEXT,
                    Q_PASSAGE,
                    POINT,
                    CHOICE1,
                    CHOICE2,
                    CHOICE3,
                    CHOICE4,
                    CHOICE5,
                    ANSWER,
                    ERA,
                    CATEGORY,
                    IS_DELETED
                FROM HIST_EXAM_QUESTIONS
                WHERE EXAM_ROUND = ?
                  AND Q_NO = ?
                """,
                resultSet -> {
                    if (!resultSet.next()) {
                        throw new BusinessException(
                                "QUESTION_NOT_FOUND",
                                "문제를 찾을 수 없습니다",
                                HttpStatus.NOT_FOUND
                        );
                    }

                    Integer round =
                            getInteger(
                                    resultSet,
                                    "EXAM_ROUND"
                            );

                    Integer number =
                            getInteger(
                                    resultSet,
                                    "Q_NO"
                            );

                    return new AdminQuestionDetailResponse(
                            round * 1000L
                                    + number,
                            round,
                            number,
                            resultSet.getString(
                                    "Q_TEXT"
                            ),
                            resultSet.getString(
                                    "Q_PASSAGE"
                            ),
                            getInteger(
                                    resultSet,
                                    "POINT"
                            ),
                            resultSet.getString(
                                    "ERA"
                            ),
                            resultSet.getString(
                                    "CATEGORY"
                            ),
                            List.of(
                                    new AdminQuestionChoiceResponse(
                                            1,
                                            resultSet.getString(
                                                    "CHOICE1"
                                            )
                                    ),
                                    new AdminQuestionChoiceResponse(
                                            2,
                                            resultSet.getString(
                                                    "CHOICE2"
                                            )
                                    ),
                                    new AdminQuestionChoiceResponse(
                                            3,
                                            resultSet.getString(
                                                    "CHOICE3"
                                            )
                                    ),
                                    new AdminQuestionChoiceResponse(
                                            4,
                                            resultSet.getString(
                                                    "CHOICE4"
                                            )
                                    ),
                                    new AdminQuestionChoiceResponse(
                                            5,
                                            resultSet.getString(
                                                    "CHOICE5"
                                            )
                                    )
                            ),
                            getInteger(
                                    resultSet,
                                    "ANSWER"
                            ),
                            getBooleanByNumber(
                                    resultSet,
                                    "IS_DELETED"
                            )
                    );
                },
                examRound,
                qNo
        );
    }

    /**
     * 합성 문제 ID를 회차와 문제 번호로 분리합니다.
     *
     * 예:
     * 900001 → 900회 1번
     */
    private QuestionKey parseQuestionId(
            Long questionId
    ) {
        if (
                questionId == null
                        || questionId <= 0
        ) {
            throw new BusinessException(
                    "INVALID_QUESTION_ID",
                    "문제 ID가 올바르지 않습니다",
                    HttpStatus.BAD_REQUEST
            );
        }

        int examRound =
                (int) (
                        questionId
                                / 1000
                );

        int qNo =
                (int) (
                        questionId
                                % 1000
                );

        if (
                examRound <= 0
                        || qNo <= 0
        ) {
            throw new BusinessException(
                    "INVALID_QUESTION_ID",
                    "문제 ID가 올바르지 않습니다",
                    HttpStatus.BAD_REQUEST
            );
        }

        return new QuestionKey(
                examRound,
                qNo
        );
    }

    private Long count(
            String sql,
            Object... params
    ) {
        Long result =
                jdbcTemplate.queryForObject(
                        sql,
                        Long.class,
                        params
                );

        return result == null
                ? 0L
                : result;
    }

    private Long getLong(
            ResultSet resultSet,
            String columnName
    ) throws SQLException {
        Number value =
                (Number) resultSet.getObject(
                        columnName
                );

        return value == null
                ? 0L
                : value.longValue();
    }

    private Integer getInteger(
            ResultSet resultSet,
            String columnName
    ) throws SQLException {
        Number value =
                (Number) resultSet.getObject(
                        columnName
                );

        return value == null
                ? null
                : value.intValue();
    }

    private Boolean getBooleanByNumber(
            ResultSet resultSet,
            String columnName
    ) throws SQLException {
        Number value =
                (Number) resultSet.getObject(
                        columnName
                );

        return value != null
                && value.intValue() == 1;
    }

    private LocalDate toLocalDate(
            Date date
    ) {
        return date == null
                ? null
                : date.toLocalDate();
    }

    private String preview(
            String value,
            int maxLength
    ) {
        if (value == null) {
            return null;
        }

        String trimmed =
                value.trim();

        if (
                trimmed.length()
                        <= maxLength
        ) {
            return trimmed;
        }

        return trimmed.substring(
                0,
                maxLength
        ) + "...";
    }

    private boolean hasText(
            String value
    ) {
        return value != null
                && !value
                .trim()
                .isEmpty();
    }

    private record QuestionKey(
            Integer examRound,
            Integer qNo
    ) {
    }
}