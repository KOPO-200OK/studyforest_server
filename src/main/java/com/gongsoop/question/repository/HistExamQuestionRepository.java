package com.gongsoop.question.repository;

import com.gongsoop.question.entity.HistExamQuestion;
import com.gongsoop.question.entity.HistExamQuestionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface HistExamQuestionRepository extends JpaRepository<HistExamQuestion, HistExamQuestionId>,
        JpaSpecificationExecutor<HistExamQuestion> {

    List<HistExamQuestion> findByIdExamRoundOrderByIdQNoAsc(Integer examRound);

    List<HistExamQuestion> findByEraContainingIgnoreCase(String era);

    List<HistExamQuestion> findByCategoryContainingIgnoreCase(String category);
}