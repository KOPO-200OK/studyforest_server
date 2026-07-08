package com.gongsoop.mockexam.repository;

import com.gongsoop.mockexam.entity.HistMockExamQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface HistMockExamQuestionRepository extends JpaRepository<HistMockExamQuestion, Long> {

    @Query("""
            SELECT q
            FROM HistMockExamQuestion q
            JOIN FETCH q.question
            WHERE q.mockExamId = :mockExamId
            ORDER BY q.questionOrder ASC
            """)
    List<HistMockExamQuestion> findByMockExamIdOrderByQuestionOrderAsc(
            @Param("mockExamId") Long mockExamId
    );
}