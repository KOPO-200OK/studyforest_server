package com.gongsoop.studyspace.dto.response;

import com.gongsoop.studyspace.entity.StudySession;
import com.gongsoop.studyspace.entity.StudySessionStatus;

import java.time.LocalDateTime;

/**
 * heartbeat·pause·resume 등 상태 tick 응답. 화면 타이머 동기화를 위해 서버 Clock 기준의
 * 현재 시각과 누적 공부 시간을 함께 내린다. 클라이언트 타이머는 표시용으로만 쓰고 저장은
 * 서버 기준을 따른다.
 */
public record SessionTickResponse(
        Long studySessionId,
        StudySessionStatus status,
        LocalDateTime serverNow,
        long displayElapsedSeconds
) {
    public static SessionTickResponse of(StudySession session, LocalDateTime serverNow) {
        return new SessionTickResponse(
                session.getId(),
                session.getStatus(),
                serverNow,
                session.elapsedSeconds(serverNow)
        );
    }
}
