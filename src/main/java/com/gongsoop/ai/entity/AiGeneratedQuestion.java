package com.gongsoop.ai.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "AI_GENERATED_QUESTIONS")
public class AiGeneratedQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ai_generated_questions_seq")
    @SequenceGenerator(
            name = "ai_generated_questions_seq",
            sequenceName = "SEQ_AI_GENERATED_QUESTIONS",
            allocationSize = 1
    )
    @Column(name = "AI_GENERATED_QUESTION_ID")
    private Long aiGeneratedQuestionId;

    @Column(name = "AI_GENERATED_QUESTION_SET_ID", nullable = false)
    private Long aiGeneratedQuestionSetId;

    @Column(name = "QUESTION_ORDER", nullable = false)
    private Integer questionOrder;

    @Lob
    @Column(name = "QUESTION_TEXT", nullable = false)
    private String questionText;

    @Lob
    @Column(name = "CHOICES_JSON")
    private String choicesJson;

    @Column(name = "ANSWER_TEXT", length = 1000)
    private String answerText;

    @Lob
    @Column(name = "EXPLANATION")
    private String explanation;

    @Column(name = "ERA", length = 100)
    private String era;

    @Column(name = "TOPIC", length = 100)
    private String topic;

    @Column(name = "DIFFICULTY", length = 30)
    private String difficulty;

    @Lob
    @Column(name = "EXAM_TIP")
    private String examTip;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    protected AiGeneratedQuestion() {
    }

    private AiGeneratedQuestion(
            Long aiGeneratedQuestionSetId,
            Integer questionOrder,
            String questionText,
            String choicesJson,
            String answerText,
            String explanation,
            String era,
            String topic,
            String difficulty,
            String examTip
    ) {
        this.aiGeneratedQuestionSetId = aiGeneratedQuestionSetId;
        this.questionOrder = questionOrder;
        this.questionText = questionText;
        this.choicesJson = choicesJson;
        this.answerText = answerText;
        this.explanation = explanation;
        this.era = era;
        this.topic = topic;
        this.difficulty = difficulty;
        this.examTip = examTip;
        this.createdAt = LocalDateTime.now();
    }

    public static AiGeneratedQuestion create(
            Long aiGeneratedQuestionSetId,
            Integer questionOrder,
            String questionText,
            String choicesJson,
            String answerText,
            String explanation,
            String era,
            String topic,
            String difficulty,
            String examTip
    ) {
        return new AiGeneratedQuestion(
                aiGeneratedQuestionSetId,
                questionOrder,
                questionText,
                choicesJson,
                answerText,
                explanation,
                era,
                topic,
                difficulty,
                examTip
        );
    }

    public Long getAiGeneratedQuestionId() {
        return aiGeneratedQuestionId;
    }

    public Long getAiGeneratedQuestionSetId() {
        return aiGeneratedQuestionSetId;
    }

    public Integer getQuestionOrder() {
        return questionOrder;
    }

    public String getQuestionText() {
        return questionText;
    }

    public String getChoicesJson() {
        return choicesJson;
    }

    public String getAnswerText() {
        return answerText;
    }

    public String getExplanation() {
        return explanation;
    }

    public String getEra() {
        return era;
    }

    public String getTopic() {
        return topic;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public String getExamTip() {
        return examTip;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}