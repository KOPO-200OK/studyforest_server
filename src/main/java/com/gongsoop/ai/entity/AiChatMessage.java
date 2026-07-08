package com.gongsoop.ai.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "AI_CHAT_MESSAGES")
public class AiChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ai_chat_messages_seq")
    @SequenceGenerator(name = "ai_chat_messages_seq", sequenceName = "SEQ_AI_CHAT_MESSAGES", allocationSize = 1)
    @Column(name = "AI_CHAT_MESSAGE_ID")
    private Long aiChatMessageId;

    @Column(name = "AI_CHAT_SESSION_ID", nullable = false)
    private Long aiChatSessionId;

    @Column(name = "SENDER", nullable = false, length = 20)
    private String sender;

    @Lob
    @Column(name = "MESSAGE_TEXT", nullable = false)
    private String messageText;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    protected AiChatMessage() {
    }

    private AiChatMessage(Long aiChatSessionId, String sender, String messageText) {
        this.aiChatSessionId = aiChatSessionId;
        this.sender = sender;
        this.messageText = messageText;
        this.createdAt = LocalDateTime.now();
    }

    public static AiChatMessage user(Long aiChatSessionId, String messageText) {
        return new AiChatMessage(aiChatSessionId, "USER", messageText);
    }

    public static AiChatMessage ai(Long aiChatSessionId, String messageText) {
        return new AiChatMessage(aiChatSessionId, "AI", messageText);
    }

    public Long getAiChatMessageId() {
        return aiChatMessageId;
    }

    public Long getAiChatSessionId() {
        return aiChatSessionId;
    }

    public String getSender() {
        return sender;
    }

    public String getMessageText() {
        return messageText;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}