package com.gongsoop.voice.controller;

import com.gongsoop.global.response.ApiResponse;
import com.gongsoop.voice.dto.response.AvailableVoiceRoomResponse;
import com.gongsoop.voice.dto.response.VoiceJoinResponse;
import com.gongsoop.voice.dto.response.VoiceParticipantResponse;
import com.gongsoop.voice.service.VoiceRoomService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/voice")
public class VoiceRoomController {

    private final VoiceRoomService voiceRoomService;

    public VoiceRoomController(VoiceRoomService voiceRoomService) {
        this.voiceRoomService = voiceRoomService;
    }

    @GetMapping("/me/room")
    public ApiResponse<AvailableVoiceRoomResponse> getMyAvailableVoiceRoom(
            @AuthenticationPrincipal String email
    ) {
        return ApiResponse.success(
                "사용 가능한 음성방을 조회했습니다",
                voiceRoomService.getMyAvailableVoiceRoom(email)
        );
    }

    @GetMapping("/zones/{studyZoneId}/participants")
    public ApiResponse<List<VoiceParticipantResponse>> getParticipants(
            @PathVariable Long studyZoneId
    ) {
        return ApiResponse.success(
                "음성방 참가자 목록을 조회했습니다",
                voiceRoomService.getParticipants(studyZoneId, null)
        );
    }

    @PostMapping("/zones/{studyZoneId}/join")
    public ApiResponse<VoiceJoinResponse> join(
            @PathVariable Long studyZoneId,
            @AuthenticationPrincipal String email
    ) {
        return ApiResponse.success(
                "음성방에 입장했습니다",
                voiceRoomService.join(studyZoneId, email)
        );
    }

    @DeleteMapping("/zones/{studyZoneId}/leave")
    public ApiResponse<Void> leave(
            @PathVariable Long studyZoneId,
            @AuthenticationPrincipal String email
    ) {
        voiceRoomService.leave(studyZoneId, email);

        return ApiResponse.success(
                "음성방에서 퇴장했습니다",
                null
        );
    }
}