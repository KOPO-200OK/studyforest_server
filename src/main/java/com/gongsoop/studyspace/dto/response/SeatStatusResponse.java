package com.gongsoop.studyspace.dto.response;

import com.gongsoop.studyspace.entity.Seat;

public record SeatStatusResponse(
        Long seatId,
        Integer seatNo,
        boolean active,
        boolean occupied,
        Integer characterId
) {
    public static SeatStatusResponse of(Seat seat, Integer characterId) {
        return new SeatStatusResponse(
                seat.getId(), seat.getSeatNo(), seat.isActive(), characterId != null, characterId);
    }
}
