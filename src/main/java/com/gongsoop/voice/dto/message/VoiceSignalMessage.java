package com.gongsoop.voice.dto.message;

public record VoiceSignalMessage(
        Long studyZoneId,
        String type,
        String targetEmail,
        String senderEmail,
        String payload
) {
    public VoiceSignalMessage withSender(String senderEmail) {
        return new VoiceSignalMessage(
                studyZoneId,
                type,
                targetEmail,
                senderEmail,
                payload
        );
    }
}