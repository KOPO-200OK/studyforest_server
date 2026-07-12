package com.gongsoop.admin.studyroom.controller;

import com.gongsoop.admin.studyroom.dto.request.UpdateAdminSeatActiveRequest;
import com.gongsoop.admin.studyroom.dto.response.AdminSeatResponse;
import com.gongsoop.admin.studyroom.service.AdminStudyRoomService;
import com.gongsoop.global.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/study-rooms")
public class AdminStudyRoomController {

    private final AdminStudyRoomService adminStudyRoomService;

    public AdminStudyRoomController(
            AdminStudyRoomService adminStudyRoomService
    ) {
        this.adminStudyRoomService = adminStudyRoomService;
    }

    /**
     * 관리자용 전체 좌석 목록 조회
     */
    @GetMapping("/seats")
    public ApiResponse<List<AdminSeatResponse>> getSeats() {
        return ApiResponse.success(
                "관리자 좌석 목록을 조회했습니다",
                adminStudyRoomService.getSeats()
        );
    }

    /**
     * 관리자용 좌석 활성화·비활성화
     */
    @PatchMapping("/seats/{seatId}/active")
    public ApiResponse<AdminSeatResponse> updateSeatActive(
            @PathVariable Long seatId,
            @Valid @RequestBody UpdateAdminSeatActiveRequest request
    ) {
        return ApiResponse.success(
                request.active()
                        ? "좌석을 활성화했습니다"
                        : "좌석을 비활성화했습니다",
                adminStudyRoomService.updateSeatActive(
                        seatId,
                        request
                )
        );
    }
}