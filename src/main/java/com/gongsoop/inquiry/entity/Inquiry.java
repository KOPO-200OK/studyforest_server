package com.gongsoop.inquiry.entity;

import com.gongsoop.member.entity.Member;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "INQUIRIES")
public class Inquiry {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "inquiry_seq")
    @SequenceGenerator(
            name = "inquiry_seq",
            sequenceName = "SEQ_INQUIRIES",
            allocationSize = 1
    )
    @Column(name = "INQUIRY_ID")
    private Long inquiryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID", nullable = false)
    private Member member;

    @Column(name = "TITLE", nullable = false, length = 200)
    private String title;

    @Lob
    @Column(name = "CONTENT", nullable = false)
    private String content;

    @Column(name = "IS_SECRET", nullable = false)
    private boolean isSecret;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT", nullable = false)
    private LocalDateTime updatedAt;

    protected Inquiry() {
    }

    private Inquiry(Member member, String title, String content, boolean isSecret) {
        this.member = member;
        this.title = title;
        this.content = content;
        this.isSecret = isSecret;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public static Inquiry create(Member member, String title, String content, boolean isSecret) {
        return new Inquiry(member, title, content, isSecret);
    }

    public void update(String title, String content, boolean isSecret) {
        this.title = title;
        this.content = content;
        this.isSecret = isSecret;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getInquiryId() {
        return inquiryId;
    }

    public Member getMember() {
        return member;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public boolean isSecret() {
        return isSecret;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
