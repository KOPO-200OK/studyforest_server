package com.gongsoop.dashboard.service;

import com.gongsoop.dashboard.dto.response.DashboardResponse;
import com.gongsoop.dashboard.dto.response.DashboardSummaryResponse;
import com.gongsoop.dashboard.dto.response.RecentActivityResponse;
import com.gongsoop.global.exception.BusinessException;
import com.gongsoop.member.entity.Member;
import com.gongsoop.member.repository.MemberRepository;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final JdbcTemplate jdbcTemplate;
    private final MemberRepository memberRepository;

    public DashboardService(
            JdbcTemplate jdbcTemplate,
            MemberRepository memberRepository
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.memberRepository = memberRepository;
    }

    public DashboardResponse getDashboard(String email) {
        DashboardSummaryResponse summary = getSummary(email);
        List<RecentActivityResponse> recentActivities = getRecentActivities(email, 10);

        return new DashboardResponse(summary, recentActivities);
    }

    public DashboardSummaryResponse getSummary(String email) {
        Long memberId = getCurrentMemberId(email);

        SolveStats histStats = jdbcTemplate.queryForObject(
                """
                SELECT
                    COUNT(*) AS SOLVED_COUNT,
                    NVL(SUM(CASE WHEN IS_CORRECT = 'Y' THEN 1 ELSE 0 END), 0) AS CORRECT_COUNT,
                    NVL(SUM(CASE WHEN IS_CORRECT = 'N' THEN 1 ELSE 0 END), 0) AS WRONG_COUNT
                FROM HIST_SOLVE_RECORDS
                WHERE MEMBER_ID = ?
                """,
                (rs, rowNum) -> new SolveStats(
                        getLong(rs, "SOLVED_COUNT"),
                        getLong(rs, "CORRECT_COUNT"),
                        getLong(rs, "WRONG_COUNT")
                ),
                memberId
        );

        SolveStats aiStats = jdbcTemplate.queryForObject(
                """
                SELECT
                    COUNT(*) AS SOLVED_COUNT,
                    NVL(SUM(CASE WHEN IS_CORRECT = 'Y' THEN 1 ELSE 0 END), 0) AS CORRECT_COUNT,
                    NVL(SUM(CASE WHEN IS_CORRECT = 'N' THEN 1 ELSE 0 END), 0) AS WRONG_COUNT
                FROM AI_GENERATED_QUESTION_SOLVE_RECORDS
                WHERE MEMBER_ID = ?
                """,
                (rs, rowNum) -> new SolveStats(
                        getLong(rs, "SOLVED_COUNT"),
                        getLong(rs, "CORRECT_COUNT"),
                        getLong(rs, "WRONG_COUNT")
                ),
                memberId
        );

        Long unresolvedWrongCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM HIST_WRONG_ANSWERS
                WHERE MEMBER_ID = ?
                  AND IS_RESOLVED = 'N'
                """,
                Long.class,
                memberId
        );

        Long generatedQuestionSetCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM AI_GENERATED_QUESTION_SETS
                WHERE MEMBER_ID = ?
                """,
                Long.class,
                memberId
        );

        Long aiChatSessionCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM AI_CHAT_SESSIONS
                WHERE MEMBER_ID = ?
                """,
                Long.class,
                memberId
        );

        MockExamStats mockExamStats = jdbcTemplate.queryForObject(
                """
                SELECT
                    COUNT(*) AS SUBMITTED_MOCK_EXAM_COUNT,
                    NVL(ROUND(AVG(SCORE), 2), 0) AS AVERAGE_MOCK_EXAM_SCORE
                FROM HIST_MOCK_EXAMS
                WHERE MEMBER_ID = ?
                  AND STATUS = 'SUBMITTED'
                """,
                (rs, rowNum) -> new MockExamStats(
                        getLong(rs, "SUBMITTED_MOCK_EXAM_COUNT"),
                        getDouble(rs, "AVERAGE_MOCK_EXAM_SCORE")
                ),
                memberId
        );

        long histSolvedCount = histStats.solvedCount();
        long aiSolvedCount = aiStats.solvedCount();

        long totalSolvedCount = histStats.solvedCount() + aiStats.solvedCount();
        long correctCount = histStats.correctCount() + aiStats.correctCount();
        long wrongCount = histStats.wrongCount() + aiStats.wrongCount();

        double accuracyRate = totalSolvedCount == 0
                ? 0.0
                : Math.round((correctCount * 10000.0 / totalSolvedCount)) / 100.0;

        return new DashboardSummaryResponse(
                totalSolvedCount,
                correctCount,
                wrongCount,
                accuracyRate,
                histSolvedCount,
                aiSolvedCount,
                unresolvedWrongCount == null ? 0L : unresolvedWrongCount,
                generatedQuestionSetCount == null ? 0L : generatedQuestionSetCount,
                aiChatSessionCount == null ? 0L : aiChatSessionCount,
                mockExamStats.submittedMockExamCount(),
                mockExamStats.averageMockExamScore()
        );
    }

    public List<RecentActivityResponse> getRecentActivities(String email, int limit) {
        Long memberId = getCurrentMemberId(email);

        int safeLimit = Math.min(Math.max(limit, 1), 30);

        return jdbcTemplate.query(
                """
                SELECT *
                FROM (
                    SELECT
                        ACTIVITY_TYPE,
                        TITLE,
                        DESCRIPTION,
                        TARGET_ID,
                        CREATED_AT
                    FROM (
                        SELECT
                            'HIST_SOLVE' AS ACTIVITY_TYPE,
                            r.EXAM_ROUND || '회 ' || r.Q_NO || '번 기출 풀이' AS TITLE,
                            CASE WHEN r.IS_CORRECT = 'Y' THEN '정답' ELSE '오답' END AS DESCRIPTION,
                            (r.EXAM_ROUND * 1000 + r.Q_NO) AS TARGET_ID,
                            r.SOLVED_AT AS CREATED_AT
                        FROM HIST_SOLVE_RECORDS r
                        WHERE r.MEMBER_ID = ?

                        UNION ALL

                        SELECT
                            'AI_GENERATED_SOLVE' AS ACTIVITY_TYPE,
                            'AI 생성 문제 풀이' AS TITLE,
                            CASE WHEN r.IS_CORRECT = 'Y' THEN '정답' ELSE '오답' END AS DESCRIPTION,
                            r.AI_GENERATED_QUESTION_ID AS TARGET_ID,
                            r.SOLVED_AT AS CREATED_AT
                        FROM AI_GENERATED_QUESTION_SOLVE_RECORDS r
                        WHERE r.MEMBER_ID = ?

                        UNION ALL

                        SELECT
                            'MOCK_EXAM' AS ACTIVITY_TYPE,
                            m.TITLE AS TITLE,
                            CASE
                                WHEN m.STATUS = 'SUBMITTED' THEN NVL(TO_CHAR(m.SCORE), '0') || '점'
                                ELSE '진행 중'
                            END AS DESCRIPTION,
                            m.MOCK_EXAM_ID AS TARGET_ID,
                            COALESCE(m.SUBMITTED_AT, m.STARTED_AT) AS CREATED_AT
                        FROM HIST_MOCK_EXAMS m
                        WHERE m.MEMBER_ID = ?

                        UNION ALL

                        SELECT
                            'AI_GENERATED_SET' AS ACTIVITY_TYPE,
                            s.TOPIC || ' AI 문제 생성' AS TITLE,
                            s.QUESTION_COUNT || '문항 생성' AS DESCRIPTION,
                            s.AI_GENERATED_QUESTION_SET_ID AS TARGET_ID,
                            s.CREATED_AT AS CREATED_AT
                        FROM AI_GENERATED_QUESTION_SETS s
                        WHERE s.MEMBER_ID = ?

                        UNION ALL

                        SELECT
                            'AI_CHAT' AS ACTIVITY_TYPE,
                            s.TITLE AS TITLE,
                            'AI 채팅 세션' AS DESCRIPTION,
                            s.AI_CHAT_SESSION_ID AS TARGET_ID,
                            s.UPDATED_AT AS CREATED_AT
                        FROM AI_CHAT_SESSIONS s
                        WHERE s.MEMBER_ID = ?
                    )
                    ORDER BY CREATED_AT DESC
                )
                WHERE ROWNUM <= ?
                """,
                (rs, rowNum) -> new RecentActivityResponse(
                        rs.getString("ACTIVITY_TYPE"),
                        rs.getString("TITLE"),
                        rs.getString("DESCRIPTION"),
                        getLong(rs, "TARGET_ID"),
                        toLocalDateTime(rs.getTimestamp("CREATED_AT"))
                ),
                memberId,
                memberId,
                memberId,
                memberId,
                memberId,
                safeLimit
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

    private Long getLong(ResultSet rs, String columnName) throws SQLException {
        Number value = (Number) rs.getObject(columnName);
        return value == null ? 0L : value.longValue();
    }

    private Double getDouble(ResultSet rs, String columnName) throws SQLException {
        Number value = (Number) rs.getObject(columnName);
        return value == null ? 0.0 : value.doubleValue();
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private record SolveStats(
            Long solvedCount,
            Long correctCount,
            Long wrongCount
    ) {
    }

    private record MockExamStats(
            Long submittedMockExamCount,
            Double averageMockExamScore
    ) {
    }
}