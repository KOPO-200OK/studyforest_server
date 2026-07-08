package com.gongsoop.mockexam.entity;

import com.gongsoop.question.entity.HistExamQuestion;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "HIST_MOCK_EXAM_QUESTIONS")
public class HistMockExamQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hist_mock_exam_questions_seq")
    @SequenceGenerator(
            name = "hist_mock_exam_questions_seq",
            sequenceName = "SEQ_HIST_MOCK_EXAM_QUESTIONS",
            allocationSize = 1
    )
    @Column(name = "MOCK_EXAM_QUESTION_ID")
    private Long mockExamQuestionId;

    @Column(name = "MOCK_EXAM_ID", nullable = false)
    private Long mockExamId;

    @Column(name = "EXAM_ROUND", nullable = false)
    private Integer examRound;

    @Column(name = "Q_NO", nullable = false)
    private Integer qNo;

    @Column(name = "QUESTION_ORDER", nullable = false)
    private Integer questionOrder;

    @Column(name = "SELECTED_ANSWER")
    private Integer selectedAnswer;

    @Column(name = "CORRECT_ANSWER", nullable = false)
    private Integer correctAnswer;

    @Column(name = "IS_CORRECT", length = 1)
    private String isCorrect;

    @Column(name = "ANSWERED_AT")
    private LocalDateTime answeredAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "EXAM_ROUND", referencedColumnName = "EXAM_ROUND", insertable = false, updatable = false),
            @JoinColumn(name = "Q_NO", referencedColumnName = "Q_NO", insertable = false, updatable = false)
    })
    private HistExamQuestion question;

    protected HistMockExamQuestion() {
    }

    private HistMockExamQuestion(Long mockExamId, HistExamQuestion question, Integer questionOrder) {
        this.mockExamId = mockExamId;
        this.examRound = question.getExamRound();
        this.qNo = question.getQNo();
        this.questionOrder = questionOrder;
        this.selectedAnswer = null;
        this.correctAnswer = question.getAnswer();
        this.isCorrect = null;
        this.answeredAt = null;
        this.question = question;
    }

    public static HistMockExamQuestion create(Long mockExamId, HistExamQuestion question, Integer questionOrder) {
        return new HistMockExamQuestion(mockExamId, question, questionOrder);
    }

    public void mark(Integer selectedAnswer) {
        this.selectedAnswer = selectedAnswer;
        this.isCorrect = correctAnswer.equals(selectedAnswer) ? "Y" : "N";
        this.answeredAt = LocalDateTime.now();
    }

    public boolean isCorrect() {
        return "Y".equals(isCorrect);
    }

    public Long getMockExamQuestionId() {
        return mockExamQuestionId;
    }

    public Long getMockExamId() {
        return mockExamId;
    }

    public Integer getExamRound() {
        return examRound;
    }

    public Integer getQNo() {
        return qNo;
    }

    public Integer getQuestionOrder() {
        return questionOrder;
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

    public LocalDateTime getAnsweredAt() {
        return answeredAt;
    }

    public HistExamQuestion getQuestion() {
        return question;
    }
}