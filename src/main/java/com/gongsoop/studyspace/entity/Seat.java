package com.gongsoop.studyspace.entity;

import com.gongsoop.global.converter.YesNoBooleanConverter;
import jakarta.persistence.*;

@Entity
@Table(name = "SEAT")
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seat_seq")
    @SequenceGenerator(name = "seat_seq", sequenceName = "SEQ_SEAT", allocationSize = 1)
    @Column(name = "SEAT_ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "STUDY_ROOM_ID", nullable = false)
    private StudyRoom studyRoom;

    @Column(name = "SEAT_NO", nullable = false)
    private Integer seatNo;

    @Convert(converter = YesNoBooleanConverter.class)
    @Column(name = "IS_ACTIVE", nullable = false, length = 1)
    private Boolean active;

    protected Seat() {
    }

    public Long getId() {
        return id;
    }

    public StudyRoom getStudyRoom() {
        return studyRoom;
    }

    public Integer getSeatNo() {
        return seatNo;
    }

    public boolean isActive() {
        return Boolean.TRUE.equals(active);
    }
}
