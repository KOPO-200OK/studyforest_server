package com.gongsoop.global.controller;

import com.gongsoop.global.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/api/v1/health")
    public ApiResponse<Map<String, Object>> health() {
        return ApiResponse.success("서버가 정상 동작 중입니다", Map.of(
                "status", "UP",
                "time", LocalDateTime.now().toString()
        ));
    }
}
