package com.gongsoop.notice.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "NOTICES")
public class Notice {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "notice_seq")
    @SequenceGenerator(
            name = "notice_seq",
            sequenceName = "SEQ_NOTICES",
            allocationSize = 1
    )
    @Column(name = "NOTICE_ID")
    private Long noticeId;

    @Column(name = "TITLE", nullable = false, length = 200)
    private String title;

    @Lob
    @Column(name = "CONTENT", nullable = false)
    private String content;

    @Column(name = "IS_PINNED", nullable = false)
    private Integer isPinned;

    @Column(name = "IS_PUBLISHED", nullable = false)
    private Integer isPublished;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT", nullable = false)
    private LocalDateTime updatedAt;

    protected Notice() {
    }

    private Notice(
            String title,
            String content,
            boolean pinned,
            boolean published
    ) {
        this.title = title;
        this.content = content;
        this.isPinned = pinned ? 1 : 0;
        this.isPublished = published ? 1 : 0;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public static Notice create(
            String title,
            String content,
            boolean pinned,
            boolean published
    ) {
        return new Notice(title, content, pinned, published);
    }

    public void update(
            String title,
            String content,
            boolean pinned,
            boolean published
    ) {
        this.title = title;
        this.content = content;
        this.isPinned = pinned ? 1 : 0;
        this.isPublished = published ? 1 : 0;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getNoticeId() {
        return noticeId;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public boolean isPinned() {
        return isPinned != null && isPinned == 1;
    }

    public boolean isPublished() {
        return isPublished != null && isPublished == 1;
    }

    public Integer getIsPublishedValue() {
        return isPublished;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}