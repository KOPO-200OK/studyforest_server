package com.gongsoop.studyspace.dto.response;

import com.gongsoop.studyspace.entity.StudyRoom;

public record StudyRoomResponse(
        Long studyRoomId,
        String roomName,
        Integer mapNo
) {
    public static StudyRoomResponse from(StudyRoom room) {
        return new StudyRoomResponse(room.getId(), room.getName(), room.getMapNo());
    }
}
