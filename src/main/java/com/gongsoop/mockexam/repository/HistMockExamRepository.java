package com.gongsoop.mockexam.repository;

import com.gongsoop.mockexam.entity.HistMockExam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface HistMockExamRepository extends JpaRepository<HistMockExam, Long> {

    @Query("""
            SELECT e
            FROM HistMockExam e
            WHERE e.mockExamId = :mockExamId
              AND e.memberId = :memberId
            """)
    Optional<HistMockExam> findByMockExamIdAndMemberId(
            @Param("mockExamId") Long mockExamId,
            @Param("memberId") Long memberId
    );

    @Query("""
            SELECT e
            FROM HistMockExam e
            WHERE e.memberId = :memberId
            ORDER BY e.startedAt DESC
            """)
    Page<HistMockExam> findByMemberIdOrderByStartedAtDesc(
            @Param("memberId") Long memberId,
            Pageable pageable
    );
}