package com.gongsoop.ai.entity;

import com.gongsoop.question.entity.HistExamQuestion;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "AI_CHAT_SESSIONS")
public class AiChatSession {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ai_chat_sessions_seq")
    @SequenceGenerator(name = "ai_chat_sessions_seq", sequenceName = "SEQ_AI_CHAT_SESSIONS", allocationSize = 1)
    @Column(name = "AI_CHAT_SESSION_ID")
    private Long aiChatSessionId;

    @Column(name = "MEMBER_ID", nullable = false)
    private Long memberId;

    @Column(name = "EXAM_ROUND")
    private Integer examRound;

    @Column(name = "Q_NO")
    private Integer qNo;

    @Column(name = "TITLE", nullable = false, length = 200)
    private String title;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT", nullable = false)
    private LocalDateTime updatedAt;

    protected AiChatSession() {
    }

    private AiChatSession(Long memberId, Integer examRound, Integer qNo, String title) {
        this.memberId = memberId;
        this.examRound = examRound;
        this.qNo = qNo;
        this.title = title;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public static AiChatSession createGeneral(Long memberId) {
        return new AiChatSession(memberId, null, null, "한국사 AI 상담");
    }

    public static AiChatSession createWithQuestion(Long memberId, HistExamQuestion question) {
        String title = question.getExamRound() + "회 " + question.getQNo() + "번 문제 상담";
        return new AiChatSession(memberId, question.getExamRound(), question.getQNo(), title);
    }

    public void touch() {
        this.updatedAt = LocalDateTime.now();
    }

    public void changeTitle(String title) {
        this.title = title.trim();
        touch();
    }

    public Long getAiChatSessionId() {
        return aiChatSessionId;
    }

    public Long getMemberId() {
        return memberId;
    }

    public Integer getExamRound() {
        return examRound;
    }

    public Integer getQNo() {
        return qNo;
    }

    public String getTitle() {
        return title;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}