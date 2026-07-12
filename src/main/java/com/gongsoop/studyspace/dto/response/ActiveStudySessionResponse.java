package com.gongsoop.studyspace.dto.response;

import com.gongsoop.studyspace.entity.SeatOccupancy;
import com.gongsoop.studyspace.entity.StudySessionStatus;

import java.time.LocalDateTime;

public record ActiveStudySessionResponse(
        Long studySessionId,
        Long studyRoomId,
        Integer mapNo,
        Long studyChannelId,
        Integer channelNo,
        Long seatId,
        Integer seatNo,
        StudySessionStatus status,
        long elapsedSeconds
) {
    public static ActiveStudySessionResponse from(SeatOccupancy occupancy, LocalDateTime now) {
        return new ActiveStudySessionResponse(
                occupancy.getStudySession().getId(),
                occupancy.getStudyChannel().getStudyRoom().getId(),
                occupancy.getStudyChannel().getStudyRoom().getMapNo(),
                occupancy.getStudyChannel().getId(),
                occupancy.getStudyChannel().getChannelNo(),
                occupancy.getSeat().getId(),
                occupancy.getSeat().getSeatNo(),
                occupancy.getStudySession().getStatus(),
                occupancy.getStudySession().elapsedSeconds(now)
        );
    }
}
