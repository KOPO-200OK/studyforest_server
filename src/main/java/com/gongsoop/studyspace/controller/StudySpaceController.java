package com.gongsoop.studyspace.controller;

import com.gongsoop.global.response.ApiResponse;
import com.gongsoop.studyspace.dto.request.OccupySeatRequest;
import com.gongsoop.studyspace.dto.response.SeatStatusResponse;
import com.gongsoop.studyspace.dto.response.StudyChannelResponse;
import com.gongsoop.studyspace.dto.response.StudyRoomResponse;
import com.gongsoop.studyspace.dto.response.StudySessionResponse;
import com.gongsoop.studyspace.service.StudySessionService;
import com.gongsoop.studyspace.service.StudySpaceQueryService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class StudySpaceController {

    private final StudySpaceQueryService studySpaceQueryService;
    private final StudySessionService studySessionService;

    public StudySpaceController(
            StudySpaceQueryService studySpaceQueryService,
            StudySessionService studySessionService
    ) {
        this.studySpaceQueryService = studySpaceQueryService;
        this.studySessionService = studySessionService;
    }

    @GetMapping("/study-rooms")
    public ApiResponse<List<StudyRoomResponse>> getStudyRooms() {
        return ApiResponse.success(
                "스터디 공간 목록을 조회했습니다",
                studySpaceQueryService.getActiveRooms()
        );
    }

    @GetMapping("/study-rooms/{studyRoomId}/channels")
    public ApiResponse<List<StudyChannelResponse>> getStudyChannels(@PathVariable Long studyRoomId) {
        return ApiResponse.success(
                "스터디 채널 목록을 조회했습니다",
                studySpaceQueryService.getActiveChannels(studyRoomId)
        );
    }

    @GetMapping("/study-channels/{studyChannelId}/seats")
    public ApiResponse<List<SeatStatusResponse>> getSeatStatuses(@PathVariable Long studyChannelId) {
        return ApiResponse.success(
                "채널 좌석 현황을 조회했습니다",
                studySpaceQueryService.getSeatStatuses(studyChannelId)
        );
    }

    @PostMapping("/study-channels/{studyChannelId}/seats/{seatId}/occupancy")
    public ApiResponse<StudySessionResponse> occupySeat(
            @PathVariable Long studyChannelId,
            @PathVariable Long seatId,
            @Valid @RequestBody(required = false) OccupySeatRequest request,
            @AuthenticationPrincipal String email
    ) {
        return ApiResponse.success(
                "좌석에 착석했습니다",
                studySessionService.occupySeat(studyChannelId, seatId, request, email)
        );
    }

    @DeleteMapping("/study-sessions/{studySessionId}/occupancy")
    public ApiResponse<StudySessionResponse> leaveSeat(
            @PathVariable Long studySessionId,
            @AuthenticationPrincipal String email
    ) {
        return ApiResponse.success(
                "좌석에서 퇴실했습니다",
                studySessionService.leaveSeat(studySessionId, email)
        );
    }
}
