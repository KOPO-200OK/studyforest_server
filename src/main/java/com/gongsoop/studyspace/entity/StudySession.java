package com.gongsoop.studyspace.entity;

import com.gongsoop.member.entity.Member;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "STUDY_SESSION")
public class StudySession {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "study_session_seq")
    @SequenceGenerator(name = "study_session_seq", sequenceName = "SEQ_STUDY_SESSION", allocationSize = 1)
    @Column(name = "STUDY_SESSION_ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "MEMBER_ID", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "STUDY_CHANNEL_ID", nullable = false)
    private StudyChannel studyChannel;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "SEAT_ID", nullable = false)
    private Seat seat;

    @Column(name = "SUBJECT", nullable = false, length = 50)
    private String subject;

    @Column(name = "STUDY_DATE", nullable = false)
    private LocalDate studyDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS_CODE", nullable = false, length = 20)
    private StudySessionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "RESUME_STATUS_CODE", length = 12)
    private StudySessionStatus resumeStatus;

    @Column(name = "STARTED_AT", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "LAST_RESUMED_AT")
    private LocalDateTime lastResumedAt;

    @Column(name = "PAUSED_AT")
    private LocalDateTime pausedAt;

    @Column(name = "PAUSE_DEADLINE_AT")
    private LocalDateTime pauseDeadlineAt;

    @Column(name = "ENDED_AT")
    private LocalDateTime endedAt;

    @Column(name = "ACCUMULATED_SECONDS", nullable = false)
    private Long accumulatedSeconds;

    @Enumerated(EnumType.STRING)
    @Column(name = "END_REASON_CODE", length = 20)
    private StudySessionEndReason endReason;

    @Version
    @Column(name = "VERSION_NO", nullable = false)
    private Long version;

    protected StudySession() {
    }

    public Long getId() {
        return id;
    }

    public StudySessionStatus getStatus() {
        return status;
    }
}
