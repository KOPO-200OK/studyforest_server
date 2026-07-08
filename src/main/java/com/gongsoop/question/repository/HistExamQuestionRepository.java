package com.gongsoop.question.repository;

import com.gongsoop.question.entity.HistExamQuestion;
import com.gongsoop.question.entity.HistExamQuestionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface HistExamQuestionRepository extends JpaRepository<HistExamQuestion, HistExamQuestionId>,
        JpaSpecificationExecutor<HistExamQuestion> {

    @Query("""
            SELECT q
            FROM HistExamQuestion q
            WHERE q.id.examRound = :examRound
            ORDER BY q.id.qNo ASC
            """)
    List<HistExamQuestion> findByIdExamRoundOrderByIdQNoAsc(
            @Param("examRound") Integer examRound
    );

    @Query("""
            SELECT q
            FROM HistExamQuestion q
            WHERE LOWER(q.era) LIKE LOWER(CONCAT(CONCAT('%', :era), '%'))
            """)
    List<HistExamQuestion> findByEraContainingIgnoreCase(
            @Param("era") String era
    );

    @Query("""
            SELECT q
            FROM HistExamQuestion q
            WHERE LOWER(q.category) LIKE LOWER(CONCAT(CONCAT('%', :category), '%'))
            """)
    List<HistExamQuestion> findByCategoryContainingIgnoreCase(
            @Param("category") String category
    );
}