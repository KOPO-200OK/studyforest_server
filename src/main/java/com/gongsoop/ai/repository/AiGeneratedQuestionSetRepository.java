package com.gongsoop.ai.repository;

import com.gongsoop.ai.entity.AiGeneratedQuestionSet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AiGeneratedQuestionSetRepository extends JpaRepository<AiGeneratedQuestionSet, Long> {

    Page<AiGeneratedQuestionSet> findByMemberIdOrderByCreatedAtDesc(Long memberId, Pageable pageable);

    Optional<AiGeneratedQuestionSet> findByAiGeneratedQuestionSetIdAndMemberId(
            Long aiGeneratedQuestionSetId,
            Long memberId
    );
}