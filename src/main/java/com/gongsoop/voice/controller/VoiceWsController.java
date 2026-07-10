package com.gongsoop.voice.controller;

import com.gongsoop.voice.dto.message.VoiceSignalMessage;
import com.gongsoop.voice.service.VoiceRoomService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class VoiceWsController {

    private final VoiceRoomService voiceRoomService;
    private final SimpMessagingTemplate messagingTemplate;

    public VoiceWsController(
            VoiceRoomService voiceRoomService,
            SimpMessagingTemplate messagingTemplate
    ) {
        this.voiceRoomService = voiceRoomService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/voice/zones/{studyZoneId}/signal")
    public void signal(
            @DestinationVariable Long studyZoneId,
            VoiceSignalMessage message,
            Principal principal
    ) {
        String senderEmail = principal == null ? null : principal.getName();

        voiceRoomService.validateSignal(studyZoneId, senderEmail);

        VoiceSignalMessage forwarded = message.withSender(senderEmail);

        messagingTemplate.convertAndSendToUser(
                message.targetEmail(),
                "/queue/voice/signals",
                forwarded
        );
    }
}