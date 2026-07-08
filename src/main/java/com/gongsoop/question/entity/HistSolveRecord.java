package com.gongsoop.question.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "HIST_SOLVE_RECORDS")
public class HistSolveRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hist_solve_records_seq")
    @SequenceGenerator(
            name = "hist_solve_records_seq",
            sequenceName = "SEQ_HIST_SOLVE_RECORDS",
            allocationSize = 1
    )
    @Column(name = "SOLVE_RECORD_ID")
    private Long solveRecordId;

    @Column(name = "MEMBER_ID", nullable = false)
    private Long memberId;

    @Column(name = "EXAM_ROUND", nullable = false)
    private Integer examRound;

    @Column(name = "Q_NO", nullable = false)
    private Integer qNo;

    @Column(name = "SELECTED_ANSWER", nullable = false)
    private Integer selectedAnswer;

    @Column(name = "CORRECT_ANSWER", nullable = false)
    private Integer correctAnswer;

    @Column(name = "IS_CORRECT", nullable = false, length = 1)
    private String isCorrect;

    @Column(name = "SOLVE_TYPE", nullable = false, length = 30)
    private String solveType;

    @Column(name = "SOLVED_AT", nullable = false)
    private LocalDateTime solvedAt;

    protected HistSolveRecord() {
    }

    private HistSolveRecord(
            Long memberId,
            Integer examRound,
            Integer qNo,
            Integer selectedAnswer,
            Integer correctAnswer,
            boolean isCorrect,
            String solveType
    ) {
        this.memberId = memberId;
        this.examRound = examRound;
        this.qNo = qNo;
        this.selectedAnswer = selectedAnswer;
        this.correctAnswer = correctAnswer;
        this.isCorrect = isCorrect ? "Y" : "N";
        this.solveType = solveType == null || solveType.isBlank() ? "HIST_EXAM" : solveType;
        this.solvedAt = LocalDateTime.now();
    }

    public static HistSolveRecord create(
            Long memberId,
            HistExamQuestion question,
            Integer selectedAnswer,
            boolean isCorrect,
            String solveType
    ) {
        return new HistSolveRecord(
                memberId,
                question.getExamRound(),
                question.getQNo(),
                selectedAnswer,
                question.getAnswer(),
                isCorrect,
                solveType
        );
    }

    public Long getSolveRecordId() {
        return solveRecordId;
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

    public Integer getSelectedAnswer() {
        return selectedAnswer;
    }

    public Integer getCorrectAnswer() {
        return correctAnswer;
    }

    public String getIsCorrect() {
        return isCorrect;
    }

    public String getSolveType() {
        return solveType;
    }

    public LocalDateTime getSolvedAt() {
        return solvedAt;
    }
}