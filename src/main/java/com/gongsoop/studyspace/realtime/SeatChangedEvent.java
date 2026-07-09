package com.gongsoop.studyspace.realtime;

/**
 * 좌석 변화 도메인 이벤트. 완성된 {@link SeatEventMessage}만 담아(엔티티 참조 없음) 발행하고,
 * 실제 Redis 방송은 커밋 이후에만 이뤄진다({@link SeatEventPublisher}).
 */
public record SeatChangedEvent(SeatEventMessage message) {
}
