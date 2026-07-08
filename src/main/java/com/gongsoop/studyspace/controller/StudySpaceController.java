package com.gongsoop.studyspace.controller;

import com.gongsoop.global.response.ApiResponse;
import com.gongsoop.studyspace.dto.response.SeatStatusResponse;
import com.gongsoop.studyspace.dto.response.StudyChannelResponse;
import com.gongsoop.studyspace.dto.response.StudyRoomResponse;
import com.gongsoop.studyspace.service.StudySpaceQueryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class StudySpaceController {

    private final StudySpaceQueryService studySpaceQueryService;

    public StudySpaceController(StudySpaceQueryService studySpaceQueryService) {
        this.studySpaceQueryService = studySpaceQueryService;
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
}
