package com.gongsoop.ai.repository;

import com.gongsoop.ai.entity.AiGeneratedQuestionSolveRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;

public interface AiGeneratedQuestionSolveRecordRepository
        extends JpaRepository<
        AiGeneratedQuestionSolveRecord,
        Long
        > {

    Page<AiGeneratedQuestionSolveRecord> findByMemberIdOrderBySolvedAtDesc(
            Long memberId,
            Pageable pageable
    );

    void deleteByAiGeneratedQuestionIdIn(
            Collection<Long> aiGeneratedQuestionIds
    );
}