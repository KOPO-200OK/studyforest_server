package com.gongsoop.mockexam.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "HIST_MOCK_EXAMS")
public class HistMockExam {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hist_mock_exams_seq")
    @SequenceGenerator(
            name = "hist_mock_exams_seq",
            sequenceName = "SEQ_HIST_MOCK_EXAMS",
            allocationSize = 1
    )
    @Column(name = "MOCK_EXAM_ID")
    private Long mockExamId;

    @Column(name = "MEMBER_ID", nullable = false)
    private Long memberId;

    @Column(name = "TITLE", nullable = false, length = 100)
    private String title;

    @Column(name = "TOTAL_QUESTION_COUNT", nullable = false)
    private Integer totalQuestionCount;

    @Column(name = "CORRECT_COUNT")
    private Integer correctCount;

    @Column(name = "SCORE")
    private Double score;

    @Column(name = "STATUS", nullable = false, length = 20)
    private String status;

    @Column(name = "STARTED_AT", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "SUBMITTED_AT")
    private LocalDateTime submittedAt;

    protected HistMockExam() {
    }

    private HistMockExam(Long memberId, String title, Integer totalQuestionCount) {
        this.memberId = memberId;
        this.title = title;
        this.totalQuestionCount = totalQuestionCount;
        this.correctCount = null;
        this.score = null;
        this.status = "STARTED";
        this.startedAt = LocalDateTime.now();
        this.submittedAt = null;
    }

    public static HistMockExam create(Long memberId, String title, Integer totalQuestionCount) {
        return new HistMockExam(memberId, title, totalQuestionCount);
    }

    public void submit(Integer correctCount) {
        this.correctCount = correctCount;
        this.score = totalQuestionCount == 0 ? 0.0 : correctCount * 100.0 / totalQuestionCount;
        this.status = "SUBMITTED";
        this.submittedAt = LocalDateTime.now();
    }

    public boolean isSubmitted() {
        return "SUBMITTED".equals(status);
    }

    public Long getMockExamId() {
        return mockExamId;
    }

    public Long getMemberId() {
        return memberId;
    }

    public String getTitle() {
        return title;
    }

    public Integer getTotalQuestionCount() {
        return totalQuestionCount;
    }

    public Integer getCorrectCount() {
        return correctCount;
    }

    public Double getScore() {
        return score;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }
}