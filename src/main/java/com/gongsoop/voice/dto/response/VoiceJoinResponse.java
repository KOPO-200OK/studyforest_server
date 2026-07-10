package com.gongsoop.voice.dto.response;

import java.util.List;

public record VoiceJoinResponse(
        Long studyZoneId,
        String zoneName,
        List<VoiceParticipantResponse> participants
) {
}