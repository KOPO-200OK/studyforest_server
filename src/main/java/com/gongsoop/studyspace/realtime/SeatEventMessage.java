package com.gongsoop.studyspace.realtime;

import com.gongsoop.studyspace.entity.StudySessionStatus;

import java.time.LocalDateTime;

/**
 * 좌석 실시간 변화 방송 payload. 트랜잭션 안에서 완성해 이벤트에 담으므로 리스너·구독자는
 * 엔티티를 다시 조회하지 않는다. Member에는 캐릭터 개념이 아직 없어 occupantName만 싣는다.
 */
public record SeatEventMessage(
        Type type,
        Long channelId,
        Long seatId,
        Integer seatNo,
        String occupantName,
        Long studySessionId,
        StudySessionStatus sessionStatus,
        LocalDateTime at
) {
    public enum Type {
        OCCUPIED,
        VACATED,
        DISCONNECTED,
        RECONNECTED,
        PAUSED,
        RESUMED,
        DISABLED
    }
}
