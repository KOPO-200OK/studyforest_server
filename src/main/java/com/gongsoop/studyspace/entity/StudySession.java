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
    @JoinColumn(name = "USER_ID", nullable = false)
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

    public static StudySession start(
            Member member,
            StudyChannel studyChannel,
            Seat seat,
            String subject,
            LocalDateTime now
    ) {
        StudySession session = new StudySession();
        session.member = member;
        session.studyChannel = studyChannel;
        session.seat = seat;
        session.subject = subject;
        session.studyDate = now.toLocalDate();
        session.status = StudySessionStatus.RUNNING;
        session.startedAt = now;
        session.lastResumedAt = now;
        session.accumulatedSeconds = 0L;
        session.version = 0L;
        return session;
    }

    /** RUNNING이 아니면 no-op. pauseDeadline은 서버 정책값을 넘겨받는다. */
    public void pause(LocalDateTime now, LocalDateTime pauseDeadline) {
        if (status != StudySessionStatus.RUNNING) {
            return;
        }
        accumulateRunningTime(now);
        status = StudySessionStatus.PAUSED;
        pausedAt = now;
        pauseDeadlineAt = pauseDeadline;
    }

    /** PAUSED가 아니면 no-op. */
    public void resume(LocalDateTime now) {
        if (status != StudySessionStatus.PAUSED) {
            return;
        }
        status = StudySessionStatus.RUNNING;
        lastResumedAt = now;
        pausedAt = null;
        pauseDeadlineAt = null;
    }

    /**
     * 활성 상태가 아니면 no-op. 공부 시간은 lastSeenAt까지만 인정하고, 복구용 상태를
     * resumeStatus에 보존한다(PAUSED였다면 pauseDeadline은 유지).
     */
    public void disconnect(LocalDateTime lastSeenAt) {
        if (status != StudySessionStatus.RUNNING && status != StudySessionStatus.PAUSED) {
            return;
        }
        resumeStatus = status;
        accumulateRunningTime(lastSeenAt);
        status = StudySessionStatus.DISCONNECTED;
    }

    /** DISCONNECTED가 아니면 no-op. 끊긴 구간을 제외하기 위해 RUNNING 복구 시 lastResumedAt을 재설정. */
    public void reconnect(LocalDateTime now) {
        if (status != StudySessionStatus.DISCONNECTED) {
            return;
        }
        StudySessionStatus restored =
                resumeStatus == null ? StudySessionStatus.RUNNING : resumeStatus;
        status = restored;
        if (restored == StudySessionStatus.RUNNING) {
            lastResumedAt = now;
        }
        resumeStatus = null;
    }

    public void completeByUser(LocalDateTime now) {
        terminate(StudySessionStatus.COMPLETED, StudySessionEndReason.USER_EXIT, now);
    }

    public void terminateByPauseTimeout(LocalDateTime now) {
        terminate(StudySessionStatus.AUTO_TERMINATED, StudySessionEndReason.PAUSE_TIMEOUT, now);
    }

    public void terminateByReconnectTimeout(LocalDateTime now) {
        terminate(StudySessionStatus.AUTO_TERMINATED, StudySessionEndReason.RECONNECT_TIMEOUT, now);
    }

    public void terminateByAdmin(LocalDateTime now) {
        terminate(StudySessionStatus.FORCED_TERMINATED, StudySessionEndReason.ADMIN_FORCE_EXIT, now);
    }

    public boolean isTerminal() {
        return status == StudySessionStatus.COMPLETED
                || status == StudySessionStatus.AUTO_TERMINATED
                || status == StudySessionStatus.FORCED_TERMINATED;
    }

    /** 화면 표시용 누적 공부 시간(초). RUNNING이면 마지막 재개 이후 경과분을 더한다. */
    public long elapsedSeconds(LocalDateTime now) {
        long base = accumulatedSeconds == null ? 0L : accumulatedSeconds;
        if (status == StudySessionStatus.RUNNING && lastResumedAt != null) {
            base += Math.max(java.time.Duration.between(lastResumedAt, now).getSeconds(), 0L);
        }
        return base;
    }

    private void terminate(
            StudySessionStatus terminalStatus,
            StudySessionEndReason reason,
            LocalDateTime now
    ) {
        if (isTerminal()) {
            return;
        }
        accumulateRunningTime(now);
        status = terminalStatus;
        endReason = reason;
        endedAt = now;
        pausedAt = null;
        pauseDeadlineAt = null;
        resumeStatus = null;
    }

    private void accumulateRunningTime(LocalDateTime until) {
        if (status == StudySessionStatus.RUNNING && lastResumedAt != null) {
            long elapsedSeconds = java.time.Duration.between(lastResumedAt, until).getSeconds();
            accumulatedSeconds += Math.max(elapsedSeconds, 0L);
        }
    }

    public Long getId() {
        return id;
    }

    public Member getMember() {
        return member;
    }

    public StudyChannel getStudyChannel() {
        return studyChannel;
    }

    public StudySessionStatus getResumeStatus() {
        return resumeStatus;
    }

    public LocalDateTime getLastResumedAt() {
        return lastResumedAt;
    }

    public LocalDateTime getPauseDeadlineAt() {
        return pauseDeadlineAt;
    }

    public Seat getSeat() {
        return seat;
    }

    public StudySessionStatus getStatus() {
        return status;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public LocalDateTime getEndedAt() {
        return endedAt;
    }

    public Long getAccumulatedSeconds() {
        return accumulatedSeconds;
    }

    public StudySessionEndReason getEndReason() {
        return endReason;
    }
}
