package com.gongsoop.studyspace.entity;

import com.gongsoop.global.converter.YesNoBooleanConverter;
import jakarta.persistence.*;

@Entity
@Table(name = "STUDY_ROOM")
public class StudyRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "study_room_seq")
    @SequenceGenerator(name = "study_room_seq", sequenceName = "SEQ_STUDY_ROOM", allocationSize = 1)
    @Column(name = "STUDY_ROOM_ID")
    private Long id;

    @Column(name = "ROOM_NAME", nullable = false, length = 100)
    private String name;

    @Column(name = "MAP_NO", nullable = false)
    private Integer mapNo;

    @Convert(converter = YesNoBooleanConverter.class)
    @Column(name = "IS_ACTIVE", nullable = false, length = 1)
    private Boolean active;

    protected StudyRoom() {
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Integer getMapNo() {
        return mapNo;
    }

    public boolean isActive() {
        return Boolean.TRUE.equals(active);
    }
}
