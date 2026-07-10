package com.gongsoop.studyspace.entity;

import com.gongsoop.global.converter.YesNoBooleanConverter;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "STUDY_ZONE")
public class StudyZone {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "study_zone_seq")
    @SequenceGenerator(name = "study_zone_seq", sequenceName = "SEQ_STUDY_ZONE", allocationSize = 1)
    @Column(name = "STUDY_ZONE_ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "STUDY_ROOM_ID", nullable = false)
    private StudyRoom studyRoom;

    @Column(name = "ZONE_CODE", nullable = false, length = 50)
    private String zoneCode;

    @Column(name = "ZONE_NAME", nullable = false, length = 100)
    private String zoneName;

    @Convert(converter = YesNoBooleanConverter.class)
    @Column(name = "VOICE_ENABLED", nullable = false, length = 1)
    private Boolean voiceEnabled;

    @Convert(converter = YesNoBooleanConverter.class)
    @Column(name = "IS_ACTIVE", nullable = false, length = 1)
    private Boolean active;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    protected StudyZone() {
    }

    public Long getId() {
        return id;
    }

    public StudyRoom getStudyRoom() {
        return studyRoom;
    }

    public String getZoneCode() {
        return zoneCode;
    }

    public String getZoneName() {
        return zoneName;
    }

    public boolean isVoiceEnabled() {
        return Boolean.TRUE.equals(voiceEnabled);
    }

    public boolean isActive() {
        return Boolean.TRUE.equals(active);
    }
}