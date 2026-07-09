package com.gongsoop.jangwon.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "JANGWON_APPLICATIONS")
public class JangwonApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "jangwon_application_seq")
    @SequenceGenerator(
            name = "jangwon_application_seq",
            sequenceName = "SEQ_JANGWON_APPLICATIONS",
            allocationSize = 1
    )
    @Column(name = "JANGWON_APPLICATION_ID")
    private Long jangwonApplicationId;

    @Column(name = "MEMBER_ID", nullable = false)
    private Long memberId;

    @Column(name = "DISPLAY_NICKNAME", nullable = false, length = 100)
    private String displayNickname;

    @Column(name = "DISPLAY_NAME", nullable = false, length = 100)
    private String displayName;

    @Column(name = "CHARACTER_NAME", nullable = false, length = 100)
    private String characterName;

    @Column(name = "CHARACTER_IMAGE_URL", length = 1000)
    private String characterImageUrl;

    @Column(name = "CERTIFICATE_IMAGE_URL", nullable = false, length = 1000)
    private String certificateImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 20)
    private JangwonStatus status;

    @Column(name = "ADMIN_MEMO", length = 1000)
    private String adminMemo;

    @Column(name = "IS_VISIBLE", nullable = false)
    private Integer isVisible;

    @Column(name = "APPLIED_AT", nullable = false)
    private LocalDateTime appliedAt;

    @Column(name = "REVIEWED_AT")
    private LocalDateTime reviewedAt;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT", nullable = false)
    private LocalDateTime updatedAt;

    protected JangwonApplication() {
    }

    private JangwonApplication(
            Long memberId,
            String displayNickname,
            String displayName,
            String characterName,
            String characterImageUrl,
            String certificateImageUrl
    ) {
        LocalDateTime now = LocalDateTime.now();

        this.memberId = memberId;
        this.displayNickname = displayNickname;
        this.displayName = displayName;
        this.characterName = characterName;
        this.characterImageUrl = characterImageUrl;
        this.certificateImageUrl = certificateImageUrl;
        this.status = JangwonStatus.PENDING;
        this.adminMemo = null;
        this.isVisible = 0;
        this.appliedAt = now;
        this.reviewedAt = null;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static JangwonApplication create(
            Long memberId,
            String displayNickname,
            String displayName,
            String characterName,
            String characterImageUrl,
            String certificateImageUrl
    ) {
        return new JangwonApplication(
                memberId,
                displayNickname,
                displayName,
                characterName,
                characterImageUrl,
                certificateImageUrl
        );
    }

    public void approve() {
        LocalDateTime now = LocalDateTime.now();

        this.status = JangwonStatus.APPROVED;
        this.adminMemo = null;
        this.isVisible = 1;
        this.reviewedAt = now;
        this.updatedAt = now;
    }

    public void reject(String adminMemo) {
        LocalDateTime now = LocalDateTime.now();

        this.status = JangwonStatus.REJECTED;
        this.adminMemo = adminMemo;
        this.isVisible = 0;
        this.reviewedAt = now;
        this.updatedAt = now;
    }

    public Long getJangwonApplicationId() {
        return jangwonApplicationId;
    }

    public Long getMemberId() {
        return memberId;
    }

    public String getDisplayNickname() {
        return displayNickname;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getCharacterName() {
        return characterName;
    }

    public String getCharacterImageUrl() {
        return characterImageUrl;
    }

    public String getCertificateImageUrl() {
        return certificateImageUrl;
    }

    public JangwonStatus getStatus() {
        return status;
    }

    public String getAdminMemo() {
        return adminMemo;
    }

    public boolean isVisible() {
        return isVisible != null && isVisible == 1;
    }

    public Integer getIsVisibleValue() {
        return isVisible;
    }

    public LocalDateTime getAppliedAt() {
        return appliedAt;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}