package com.gongsoop.question.repository;

import com.gongsoop.question.entity.HistWrongAnswer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface HistWrongAnswerRepository extends JpaRepository<HistWrongAnswer, Long> {

    @Query("""
            SELECT w
            FROM HistWrongAnswer w
            WHERE w.memberId = :memberId
              AND w.examRound = :examRound
              AND w.qNo = :qNo
            """)
    Optional<HistWrongAnswer> findByMemberIdAndExamRoundAndQNo(
            @Param("memberId") Long memberId,
            @Param("examRound") Integer examRound,
            @Param("qNo") Integer qNo
    );

    @Query("""
            SELECT w
            FROM HistWrongAnswer w
            WHERE w.wrongAnswerId = :wrongAnswerId
              AND w.memberId = :memberId
            """)
    Optional<HistWrongAnswer> findByWrongAnswerIdAndMemberId(
            @Param("wrongAnswerId") Long wrongAnswerId,
            @Param("memberId") Long memberId
    );

    @Query("""
            SELECT w
            FROM HistWrongAnswer w
            WHERE w.memberId = :memberId
            """)
    Page<HistWrongAnswer> findByMemberId(
            @Param("memberId") Long memberId,
            Pageable pageable
    );

    @Query("""
            SELECT w
            FROM HistWrongAnswer w
            WHERE w.memberId = :memberId
              AND w.isResolved = :isResolved
            """)
    Page<HistWrongAnswer> findByMemberIdAndIsResolved(
            @Param("memberId") Long memberId,
            @Param("isResolved") String isResolved,
            Pageable pageable
    );
}