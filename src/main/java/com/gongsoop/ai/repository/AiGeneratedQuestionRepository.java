package com.gongsoop.ai.repository;

import com.gongsoop.ai.entity.AiGeneratedQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiGeneratedQuestionRepository extends JpaRepository<AiGeneratedQuestion, Long> {

    List<AiGeneratedQuestion> findByAiGeneratedQuestionSetIdOrderByQuestionOrderAsc(
            Long aiGeneratedQuestionSetId
    );
}