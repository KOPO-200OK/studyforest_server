package com.gongsoop.studyspace.entity;

import com.gongsoop.global.converter.YesNoBooleanConverter;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "STUDY_CHANNEL")
public class StudyChannel {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "study_channel_seq")
    @SequenceGenerator(name = "study_channel_seq", sequenceName = "SEQ_STUDY_CHANNEL", allocationSize = 1)
    @Column(name = "STUDY_CHANNEL_ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "STUDY_ROOM_ID", nullable = false)
    private StudyRoom studyRoom;

    @Column(name = "CHANNEL_NO", nullable = false)
    private Integer channelNo;

    @Column(name = "CHANNEL_NAME", nullable = false, length = 100)
    private String name;

    @Convert(converter = YesNoBooleanConverter.class)
    @Column(name = "IS_ACTIVE", nullable = false, length = 1)
    private Boolean active;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    protected StudyChannel() {
    }

    public Long getId() {
        return id;
    }

    public StudyRoom getStudyRoom() {
        return studyRoom;
    }

    public Integer getChannelNo() {
        return channelNo;
    }

    public String getName() {
        return name;
    }

    public boolean isActive() {
        return Boolean.TRUE.equals(active);
    }
}
