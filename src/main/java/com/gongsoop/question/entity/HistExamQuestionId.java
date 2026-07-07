package com.gongsoop.question.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class HistExamQuestionId implements Serializable {

    @Column(name = "EXAM_ROUND")
    private Integer examRound;

    @Column(name = "Q_NO")
    private Integer qNo;

    protected HistExamQuestionId() {
    }

    public HistExamQuestionId(Integer examRound, Integer qNo) {
        this.examRound = examRound;
        this.qNo = qNo;
    }

    public Integer getExamRound() {
        return examRound;
    }

    public Integer getQNo() {
        return qNo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof HistExamQuestionId that)) {
            return false;
        }
        return Objects.equals(examRound, that.examRound)
                && Objects.equals(qNo, that.qNo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(examRound, qNo);
    }
}