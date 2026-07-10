package com.gongsoop.voice.dto.response;

public record AvailableVoiceRoomResponse(
        Long studyZoneId,
        String zoneCode,
        String zoneName,
        Long studyRoomId,
        Integer mapNo,
        Long studyChannelId,
        Integer channelNo,
        Long seatId,
        Integer seatNo,
        boolean voiceEnabled
) {
}