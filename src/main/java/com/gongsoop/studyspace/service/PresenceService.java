package com.gongsoop.studyspace.service;

import com.gongsoop.studyspace.config.StudySpaceRealtimeProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 접속자(presence) · 재접속 대기 · WS 세션 매핑을 Redis로 관리한다.
 * DB(Oracle)를 최종 확정 데이터로 두고, 고빈도·휘발성 상태만 Redis에 얹는 캐시 계층이다.
 */
@Service
public class PresenceService {

    private final StringRedisTemplate redis;
    private final StudySpaceRealtimeProperties properties;

    public PresenceService(StringRedisTemplate redis, StudySpaceRealtimeProperties properties) {
        this.redis = redis;
        this.properties = properties;
    }

    /** heartbeat 수신마다 생존 TTL 갱신. */
    public void refresh(Long sessionId) {
        refresh(sessionId, properties.getPresenceTtl());
    }

    /** 임의 TTL로 presence를 심는다(재기동 유예 seeding 등). */
    public void refresh(Long sessionId, java.time.Duration ttl) {
        redis.opsForValue().set(
                presenceKey(sessionId),
                Long.toString(System.currentTimeMillis()),
                ttl
        );
    }

    /** presence 키가 살아 있으면 접속 중으로 본다. */
    public boolean isAlive(Long sessionId) {
        return Boolean.TRUE.equals(redis.hasKey(presenceKey(sessionId)));
    }

    public void clearPresence(Long sessionId) {
        redis.delete(presenceKey(sessionId));
    }

    /** DB last_seen_at 쓰기 스로틀. SETNX로 판단해 throttle 간격이 지났을 때만 true. */
    public boolean tryMarkDbTouched(Long sessionId) {
        Boolean first = redis.opsForValue().setIfAbsent(
                dbTouchKey(sessionId), "1", properties.getDbLastSeenThrottle());
        return Boolean.TRUE.equals(first);
    }

    /** 재접속 대기 창 개방(Redis TTL이 1차 판정). 만료 = 재접속 실패. */
    public void startReconnectWindow(Long sessionId) {
        redis.opsForValue().set(reconnectKey(sessionId), "1", properties.getReconnectWindow());
    }

    /** 재기동 유예 등 임의 기간의 재접속 창 개방. */
    public void startReconnectWindow(Long sessionId, java.time.Duration window) {
        redis.opsForValue().set(reconnectKey(sessionId), "1", window);
    }

    public boolean isReconnectWindowOpen(Long sessionId) {
        return Boolean.TRUE.equals(redis.hasKey(reconnectKey(sessionId)));
    }

    public void clearReconnect(Long sessionId) {
        redis.delete(reconnectKey(sessionId));
    }

    // WS 세션 ↔ 학습 세션 매핑. 단일 연결 정책: current-ws를 최신 join/heartbeat로 갱신하고
    // 종료가 현재 연결일 때만 끊김 처리해, 재연결/새 탭으로 살아 있는 세션을 보호한다.

    public void linkWsSession(String wsSessionId, Long studySessionId) {
        redis.opsForValue().set(
                wsLinkKey(wsSessionId),
                Long.toString(studySessionId),
                java.time.Duration.ofHours(6)
        );
        redis.opsForValue().set(
                currentWsKey(studySessionId),
                wsSessionId,
                java.time.Duration.ofHours(6)
        );
    }

    public Long resolveWsSession(String wsSessionId) {
        String value = redis.opsForValue().get(wsLinkKey(wsSessionId));
        return value == null ? null : Long.valueOf(value);
    }

    /** 주어진 WS가 이 세션의 현재(최신) 연결인지. 오래된 연결의 종료 이벤트를 걸러낸다. */
    public boolean isCurrentWs(Long studySessionId, String wsSessionId) {
        return wsSessionId.equals(redis.opsForValue().get(currentWsKey(studySessionId)));
    }

    public void unlink(String wsSessionId) {
        redis.delete(wsLinkKey(wsSessionId));
    }

    private String presenceKey(Long sessionId) {
        return "presence:session:" + sessionId;
    }

    private String dbTouchKey(Long sessionId) {
        return "last-db-touch:session:" + sessionId;
    }

    private String reconnectKey(Long sessionId) {
        return "reconnect:session:" + sessionId;
    }

    private String wsLinkKey(String wsSessionId) {
        return "ws:link:" + wsSessionId;
    }

    private String currentWsKey(Long studySessionId) {
        return "session:" + studySessionId + ":current-ws";
    }
}
