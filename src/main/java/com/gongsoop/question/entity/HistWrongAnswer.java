package com.gongsoop.question.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "HIST_WRONG_ANSWERS")
public class HistWrongAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hist_wrong_answers_seq")
    @SequenceGenerator(
            name = "hist_wrong_answers_seq",
            sequenceName = "SEQ_HIST_WRONG_ANSWERS",
            allocationSize = 1
    )
    @Column(name = "WRONG_ANSWER_ID")
    private Long wrongAnswerId;

    @Column(name = "MEMBER_ID", nullable = false)
    private Long memberId;

    @Column(name = "EXAM_ROUND", nullable = false)
    private Integer examRound;

    @Column(name = "Q_NO", nullable = false)
    private Integer qNo;

    @Column(name = "WRONG_COUNT", nullable = false)
    private Integer wrongCount;

    @Column(name = "LAST_SELECTED_ANSWER", nullable = false)
    private Integer lastSelectedAnswer;

    @Column(name = "CORRECT_ANSWER", nullable = false)
    private Integer correctAnswer;

    @Column(name = "IS_RESOLVED", nullable = false, length = 1)
    private String isResolved;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "RESOLVED_AT")
    private LocalDateTime resolvedAt;

    protected HistWrongAnswer() {
    }

    private HistWrongAnswer(
            Long memberId,
            HistExamQuestion question,
            Integer selectedAnswer
    ) {
        this.memberId = memberId;
        this.examRound = question.getExamRound();
        this.qNo = question.getQNo();
        this.wrongCount = 1;
        this.lastSelectedAnswer = selectedAnswer;
        this.correctAnswer = question.getAnswer();
        this.isResolved = "N";
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public static HistWrongAnswer create(
            Long memberId,
            HistExamQuestion question,
            Integer selectedAnswer
    ) {
        return new HistWrongAnswer(memberId, question, selectedAnswer);
    }

    public void markWrong(Integer selectedAnswer, Integer correctAnswer) {
        this.wrongCount += 1;
        this.lastSelectedAnswer = selectedAnswer;
        this.correctAnswer = correctAnswer;
        this.isResolved = "N";
        this.resolvedAt = null;
        this.updatedAt = LocalDateTime.now();
    }

    public void resolve() {
        this.isResolved = "Y";
        this.resolvedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Long getWrongAnswerId() {
        return wrongAnswerId;
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

    public Integer getWrongCount() {
        return wrongCount;
    }

    public Integer getLastSelectedAnswer() {
        return lastSelectedAnswer;
    }

    public Integer getCorrectAnswer() {
        return correctAnswer;
    }

    public String getIsResolved() {
        return isResolved;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public boolean isResolved() {
        return "Y".equals(isResolved);
    }
}