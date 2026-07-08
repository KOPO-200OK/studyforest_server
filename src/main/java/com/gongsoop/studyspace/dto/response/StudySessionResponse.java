package com.gongsoop.studyspace.dto.response;

import com.gongsoop.studyspace.entity.StudySession;
import com.gongsoop.studyspace.entity.StudySessionEndReason;
import com.gongsoop.studyspace.entity.StudySessionStatus;

import java.time.LocalDateTime;

public record StudySessionResponse(
        Long studySessionId,
        Long studyChannelId,
        Long seatId,
        StudySessionStatus status,
        StudySessionEndReason endReason,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        Long accumulatedSeconds
) {
    public static StudySessionResponse from(StudySession session) {
        return new StudySessionResponse(
                session.getId(),
                session.getStudyChannel().getId(),
                session.getSeat().getId(),
                session.getStatus(),
                session.getEndReason(),
                session.getStartedAt(),
                session.getEndedAt(),
                session.getAccumulatedSeconds()
        );
    }
}
