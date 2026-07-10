package com.gongsoop.inquiry.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "INQUIRY_COMMENTS")
public class InquiryComment {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "inquiry_comment_seq")
    @SequenceGenerator(
            name = "inquiry_comment_seq",
            sequenceName = "SEQ_INQUIRY_COMMENTS",
            allocationSize = 1
    )
    @Column(name = "COMMENT_ID")
    private Long commentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "INQUIRY_ID", nullable = false)
    private Inquiry inquiry;

    @Lob
    @Column(name = "CONTENT", nullable = false)
    private String content;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT", nullable = false)
    private LocalDateTime updatedAt;

    protected InquiryComment() {
    }

    private InquiryComment(Inquiry inquiry, String content) {
        this.inquiry = inquiry;
        this.content = content;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public static InquiryComment create(Inquiry inquiry, String content) {
        return new InquiryComment(inquiry, content);
    }

    public void update(String content) {
        this.content = content;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getCommentId() {
        return commentId;
    }

    public Inquiry getInquiry() {
        return inquiry;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
