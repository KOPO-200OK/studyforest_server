package com.gongsoop.voice.dto.message;

public record VoiceParticipantEvent(
        String type,
        Long studyZoneId,
        String email,
        String name
) {
    public static VoiceParticipantEvent joined(Long studyZoneId, String email, String name) {
        return new VoiceParticipantEvent("JOINED", studyZoneId, email, name);
    }

    public static VoiceParticipantEvent left(Long studyZoneId, String email, String name) {
        return new VoiceParticipantEvent("LEFT", studyZoneId, email, name);
    }
}