package com.gongsoop.ai.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "AI_GENERATED_QUESTION_SOLVE_RECORDS")
public class AiGeneratedQuestionSolveRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ai_gen_q_solve_records_seq")
    @SequenceGenerator(
            name = "ai_gen_q_solve_records_seq",
            sequenceName = "SEQ_AI_GEN_Q_SOLVE_RECORDS",
            allocationSize = 1
    )
    @Column(name = "AI_GENERATED_QUESTION_SOLVE_RECORD_ID")
    private Long aiGeneratedQuestionSolveRecordId;

    @Column(name = "MEMBER_ID", nullable = false)
    private Long memberId;

    @Column(name = "AI_GENERATED_QUESTION_ID", nullable = false)
    private Long aiGeneratedQuestionId;

    @Column(name = "SELECTED_CHOICE_INDEX")
    private Integer selectedChoiceIndex;

    @Column(name = "SELECTED_ANSWER_TEXT", length = 1000)
    private String selectedAnswerText;

    @Column(name = "CORRECT_ANSWER_TEXT", length = 1000)
    private String correctAnswerText;

    @Column(name = "IS_CORRECT", nullable = false, length = 1)
    private String isCorrect;

    @Column(name = "SOLVED_AT", nullable = false)
    private LocalDateTime solvedAt;

    protected AiGeneratedQuestionSolveRecord() {
    }

    private AiGeneratedQuestionSolveRecord(
            Long memberId,
            Long aiGeneratedQuestionId,
            Integer selectedChoiceIndex,
            String selectedAnswerText,
            String correctAnswerText,
            Boolean isCorrect
    ) {
        this.memberId = memberId;
        this.aiGeneratedQuestionId = aiGeneratedQuestionId;
        this.selectedChoiceIndex = selectedChoiceIndex;
        this.selectedAnswerText = selectedAnswerText;
        this.correctAnswerText = correctAnswerText;
        this.isCorrect = Boolean.TRUE.equals(isCorrect) ? "Y" : "N";
        this.solvedAt = LocalDateTime.now();
    }

    public static AiGeneratedQuestionSolveRecord create(
            Long memberId,
            Long aiGeneratedQuestionId,
            Integer selectedChoiceIndex,
            String selectedAnswerText,
            String correctAnswerText,
            Boolean isCorrect
    ) {
        return new AiGeneratedQuestionSolveRecord(
                memberId,
                aiGeneratedQuestionId,
                selectedChoiceIndex,
                selectedAnswerText,
                correctAnswerText,
                isCorrect
        );
    }

    public Long getAiGeneratedQuestionSolveRecordId() {
        return aiGeneratedQuestionSolveRecordId;
    }

    public Long getMemberId() {
        return memberId;
    }

    public Long getAiGeneratedQuestionId() {
        return aiGeneratedQuestionId;
    }

    public Integer getSelectedChoiceIndex() {
        return selectedChoiceIndex;
    }

    public String getSelectedAnswerText() {
        return selectedAnswerText;
    }

    public String getCorrectAnswerText() {
        return correctAnswerText;
    }

    public Boolean getIsCorrect() {
        return "Y".equals(isCorrect);
    }

    public LocalDateTime getSolvedAt() {
        return solvedAt;
    }
}