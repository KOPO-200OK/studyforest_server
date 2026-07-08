package com.gongsoop.ai.repository;

import com.gongsoop.ai.entity.AiChatSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AiChatSessionRepository extends JpaRepository<AiChatSession, Long> {

    Optional<AiChatSession> findByAiChatSessionIdAndMemberId(Long aiChatSessionId, Long memberId);

    List<AiChatSession> findByMemberIdOrderByUpdatedAtDesc(Long memberId);
}