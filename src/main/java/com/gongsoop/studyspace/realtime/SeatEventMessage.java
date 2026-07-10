package com.gongsoop.studyspace.realtime;

/**
 * 좌석 실시간 변화 공개 payload. 다른 회원의 개인정보나 내부 세션 식별자는 포함하지 않는다.
 */
public record SeatEventMessage(
        Type type,
        Long channelId,
        Long seatId,
        Integer seatNo,
        Integer characterId
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
