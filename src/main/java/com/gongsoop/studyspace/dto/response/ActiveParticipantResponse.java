package com.gongsoop.studyspace.dto.response;

import com.gongsoop.studyspace.entity.SeatOccupancy;
import com.gongsoop.studyspace.entity.StudySessionStatus;

import java.time.LocalDateTime;

public record ActiveParticipantResponse(
        Long memberId,
        String displayName,
        long elapsedSeconds,
        boolean running
) {
    public static ActiveParticipantResponse from(
            SeatOccupancy occupancy,
            LocalDateTime now
    ) {
        return new ActiveParticipantResponse(
                occupancy.getMember().getId(),
                occupancy.getMember().getNickname(),
                occupancy
                        .getStudySession()
                        .elapsedSeconds(now),
                occupancy
                        .getStudySession()
                        .getStatus()
                        == StudySessionStatus.RUNNING
        );
    }
}