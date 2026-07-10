package com.gongsoop.studyspace.entity;

import com.gongsoop.member.entity.Member;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "SEAT_OCCUPANCY")
public class SeatOccupancy {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seat_occupancy_seq")
    @SequenceGenerator(name = "seat_occupancy_seq", sequenceName = "SEQ_SEAT_OCCUPANCY", allocationSize = 1)
    @Column(name = "SEAT_OCCUPANCY_ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "STUDY_CHANNEL_ID", nullable = false)
    private StudyChannel studyChannel;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "SEAT_ID", nullable = false)
    private Seat seat;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "USER_ID", nullable = false)
    private Member member;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "STUDY_SESSION_ID", nullable = false)
    private StudySession studySession;

    @Column(name = "OCCUPIED_AT", nullable = false)
    private LocalDateTime occupiedAt;

    @Column(name = "LAST_SEEN_AT", nullable = false)
    private LocalDateTime lastSeenAt;

    @Column(name = "DISCONNECTED_AT")
    private LocalDateTime disconnectedAt;

    @Column(name = "RECONNECT_DEADLINE_AT")
    private LocalDateTime reconnectDeadlineAt;

    @Version
    @Column(name = "VERSION_NO", nullable = false)
    private Long version;

    protected SeatOccupancy() {
    }

    public static SeatOccupancy occupy(
            StudyChannel studyChannel,
            Seat seat,
            Member member,
            StudySession studySession,
            LocalDateTime now
    ) {
        SeatOccupancy occupancy = new SeatOccupancy();
        occupancy.studyChannel = studyChannel;
        occupancy.seat = seat;
        occupancy.member = member;
        occupancy.studySession = studySession;
        occupancy.occupiedAt = now;
        occupancy.lastSeenAt = now;
        occupancy.version = 0L;
        return occupancy;
    }

    /** heartbeat 확인. 마지막 접속 시각을 갱신하고 끊김 판정 흔적을 지운다(재접속 포함). */
    public void touch(LocalDateTime now) {
        this.lastSeenAt = now;
        this.disconnectedAt = null;
        this.reconnectDeadlineAt = null;
    }

    /** 연결 중단 판정. 자동 퇴실 예정 시각(재접속 제한)을 함께 기록한다. */
    public void markDisconnected(LocalDateTime now, LocalDateTime reconnectDeadline) {
        this.disconnectedAt = now;
        this.reconnectDeadlineAt = reconnectDeadline;
    }

    public Long getId() {
        return id;
    }

    public StudyChannel getStudyChannel() {
        return studyChannel;
    }

    public Seat getSeat() {
        return seat;
    }

    public Member getMember() {
        return member;
    }

    public StudySession getStudySession() {
        return studySession;
    }

    public LocalDateTime getLastSeenAt() {
        return lastSeenAt;
    }

    public LocalDateTime getDisconnectedAt() {
        return disconnectedAt;
    }

    public LocalDateTime getReconnectDeadlineAt() {
        return reconnectDeadlineAt;
    }
}
