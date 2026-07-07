package com.gongsoop.question.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@Table(name = "HIST_EXAM_QUESTIONS")
public class HistExamQuestion {

    @EmbeddedId
    private HistExamQuestionId id;

    @Lob
    @Column(name = "Q_TEXT")
    private String questionText;

    @Column(name = "POINT")
    private Integer point;

    @Column(name = "CHOICE1")
    private String choice1;

    @Column(name = "CHOICE2")
    private String choice2;

    @Column(name = "CHOICE3")
    private String choice3;

    @Column(name = "CHOICE4")
    private String choice4;

    @Column(name = "CHOICE5")
    private String choice5;

    @Column(name = "ANSWER")
    private Integer answer;

    @Lob
    @Column(name = "Q_PASSAGE")
    private String passage;

    @Column(name = "ERA")
    private String era;

    @Column(name = "CATEGORY")
    private String category;

    protected HistExamQuestion() {
    }

    public HistExamQuestionId getId() {
        return id;
    }

    public Integer getExamRound() {
        return id.getExamRound();
    }

    public Integer getQNo() {
        return id.getQNo();
    }

    public String getQuestionText() {
        return questionText;
    }

    public Integer getPoint() {
        return point;
    }

    public String getChoice1() {
        return choice1;
    }

    public String getChoice2() {
        return choice2;
    }

    public String getChoice3() {
        return choice3;
    }

    public String getChoice4() {
        return choice4;
    }

    public String getChoice5() {
        return choice5;
    }

    public Integer getAnswer() {
        return answer;
    }

    public String getPassage() {
        return passage;
    }

    public String getEra() {
        return era;
    }

    public String getCategory() {
        return category;
    }

    public Long getSyntheticQuestionId() {
        return getExamRound() * 1000L + getQNo();
    }
}