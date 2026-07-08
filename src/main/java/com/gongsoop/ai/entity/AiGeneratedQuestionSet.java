package com.gongsoop.ai.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "AI_GENERATED_QUESTION_SETS")
public class AiGeneratedQuestionSet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ai_generated_question_sets_seq")
    @SequenceGenerator(
            name = "ai_generated_question_sets_seq",
            sequenceName = "SEQ_AI_GENERATED_QUESTION_SETS",
            allocationSize = 1
    )
    @Column(name = "AI_GENERATED_QUESTION_SET_ID")
    private Long aiGeneratedQuestionSetId;

    @Column(name = "MEMBER_ID", nullable = false)
    private Long memberId;

    @Column(name = "TOPIC", nullable = false, length = 100)
    private String topic;

    @Column(name = "DIFFICULTY", nullable = false, length = 30)
    private String difficulty;

    @Column(name = "QUESTION_TYPE", nullable = false, length = 50)
    private String questionType;

    @Column(name = "QUESTION_COUNT", nullable = false)
    private Integer questionCount;

    @Column(name = "INCLUDE_EXPLANATION", nullable = false, length = 1)
    private String includeExplanation;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    protected AiGeneratedQuestionSet() {
    }

    private AiGeneratedQuestionSet(
            Long memberId,
            String topic,
            String difficulty,
            String questionType,
            Integer questionCount,
            Boolean includeExplanation
    ) {
        this.memberId = memberId;
        this.topic = topic;
        this.difficulty = difficulty;
        this.questionType = questionType;
        this.questionCount = questionCount;
        this.includeExplanation = Boolean.FALSE.equals(includeExplanation) ? "N" : "Y";
        this.createdAt = LocalDateTime.now();
    }

    public static AiGeneratedQuestionSet create(
            Long memberId,
            String topic,
            String difficulty,
            String questionType,
            Integer questionCount,
            Boolean includeExplanation
    ) {
        return new AiGeneratedQuestionSet(
                memberId,
                topic,
                difficulty,
                questionType,
                questionCount,
                includeExplanation
        );
    }

    public Long getAiGeneratedQuestionSetId() {
        return aiGeneratedQuestionSetId;
    }

    public Long getMemberId() {
        return memberId;
    }

    public String getTopic() {
        return topic;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public String getQuestionType() {
        return questionType;
    }

    public Integer getQuestionCount() {
        return questionCount;
    }

    public Boolean getIncludeExplanation() {
        return "Y".equals(includeExplanation);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}