package com.gongsoop.studyspace.dto.response;

import com.gongsoop.studyspace.entity.Seat;

public record SeatStatusResponse(
        Long seatId,
        Integer seatNo,
        boolean active,
        boolean occupied
) {
    public static SeatStatusResponse of(Seat seat, boolean occupied) {
        return new SeatStatusResponse(seat.getId(), seat.getSeatNo(), seat.isActive(), occupied);
    }
}
