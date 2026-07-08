package com.gongsoop.ai.repository;

import com.gongsoop.ai.entity.AiChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiChatMessageRepository extends JpaRepository<AiChatMessage, Long> {

    List<AiChatMessage> findByAiChatSessionIdOrderByCreatedAtAsc(Long aiChatSessionId);
}