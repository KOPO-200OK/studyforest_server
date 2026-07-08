package com.gongsoop.study.service;

import com.gongsoop.global.exception.BusinessException;
import com.gongsoop.member.entity.Member;
import com.gongsoop.member.repository.MemberRepository;
import com.gongsoop.study.dto.response.StudySummaryResponse;
import com.gongsoop.study.dto.response.WeaknessAnalysisResponse;
import com.gongsoop.study.dto.response.WeaknessItemResponse;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class StudyAnalysisService {

    private final JdbcTemplate jdbcTemplate;
    private final MemberRepository memberRepository;

    public StudyAnalysisService(JdbcTemplate jdbcTemplate, MemberRepository memberRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.memberRepository = memberRepository;
    }

    public StudySummaryResponse getSummary(String email) {
        Long memberId = getCurrentMemberId(email);

        StudySummaryResponse baseSummary = jdbcTemplate.queryForObject(
                """
                SELECT
                    COUNT(*) AS TOTAL_SOLVED_COUNT,
                    NVL(SUM(CASE WHEN IS_CORRECT = 'Y' THEN 1 ELSE 0 END), 0) AS CORRECT_COUNT,
                    NVL(SUM(CASE WHEN IS_CORRECT = 'N' THEN 1 ELSE 0 END), 0) AS WRONG_COUNT,
                    ROUND(
                        CASE
                            WHEN COUNT(*) = 0 THEN 0
                            ELSE NVL(SUM(CASE WHEN IS_CORRECT = 'Y' THEN 1 ELSE 0 END), 0) * 100 / COUNT(*)
                        END,
                        2
                    ) AS ACCURACY_RATE
                FROM HIST_SOLVE_RECORDS
                WHERE MEMBER_ID = ?
                """,
                (rs, rowNum) -> new StudySummaryResponse(
                        getLong(rs, "TOTAL_SOLVED_COUNT"),
                        getLong(rs, "CORRECT_COUNT"),
                        getLong(rs, "WRONG_COUNT"),
                        getDouble(rs, "ACCURACY_RATE"),
                        0L,
                        0L,
                        0.0
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

        return new StudySummaryResponse(
                baseSummary.totalSolvedCount(),
                baseSummary.correctCount(),
                baseSummary.wrongCount(),
                baseSummary.accuracyRate(),
                unresolvedWrongCount == null ? 0L : unresolvedWrongCount,
                mockExamStats.submittedMockExamCount(),
                mockExamStats.averageMockExamScore()
        );
    }

    public WeaknessAnalysisResponse getWeaknessAnalysis(String email) {
        Long memberId = getCurrentMemberId(email);

        List<WeaknessItemResponse> items = jdbcTemplate.query(
                """
                SELECT
                    NVL(q.ERA, '미분류') AS ERA,
                    NVL(q.CATEGORY, '미분류') AS CATEGORY,
                    COUNT(*) AS SOLVED_COUNT,
                    NVL(SUM(CASE WHEN r.IS_CORRECT = 'Y' THEN 1 ELSE 0 END), 0) AS CORRECT_COUNT,
                    NVL(SUM(CASE WHEN r.IS_CORRECT = 'N' THEN 1 ELSE 0 END), 0) AS WRONG_COUNT,
                    ROUND(
                        CASE
                            WHEN COUNT(*) = 0 THEN 0
                            ELSE NVL(SUM(CASE WHEN r.IS_CORRECT = 'Y' THEN 1 ELSE 0 END), 0) * 100 / COUNT(*)
                        END,
                        2
                    ) AS ACCURACY_RATE
                FROM HIST_SOLVE_RECORDS r
                JOIN HIST_EXAM_QUESTIONS q
                  ON r.EXAM_ROUND = q.EXAM_ROUND
                 AND r.Q_NO = q.Q_NO
                WHERE r.MEMBER_ID = ?
                GROUP BY NVL(q.ERA, '미분류'), NVL(q.CATEGORY, '미분류')
                ORDER BY ACCURACY_RATE ASC, SOLVED_COUNT DESC
                """,
                (rs, rowNum) -> new WeaknessItemResponse(
                        rs.getString("ERA"),
                        rs.getString("CATEGORY"),
                        getLong(rs, "SOLVED_COUNT"),
                        getLong(rs, "CORRECT_COUNT"),
                        getLong(rs, "WRONG_COUNT"),
                        getDouble(rs, "ACCURACY_RATE")
                ),
                memberId
        );

        return new WeaknessAnalysisResponse(items);
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

    private Long getLong(ResultSet rs, String columnName) throws SQLException {
        Number value = (Number) rs.getObject(columnName);
        return value == null ? 0L : value.longValue();
    }

    private Double getDouble(ResultSet rs, String columnName) throws SQLException {
        Number value = (Number) rs.getObject(columnName);
        return value == null ? 0.0 : value.doubleValue();
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private record MockExamStats(
            Long submittedMockExamCount,
            Double averageMockExamScore
    ) {
    }
}