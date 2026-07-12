package com.gongsoop.admin.studyroom.dto.response;

public record AdminSeatResponse(
        Long seatId,
        Long studyRoomId,
        String roomName,
        Integer mapNo,
        Integer seatNo,
        Boolean active,
        Boolean occupied
) {
}