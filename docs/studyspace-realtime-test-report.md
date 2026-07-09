# 스터디 공간 실시간 좌석 시스템 — 테스트 설계·방식·결과

> 대상: `studyspace` 실시간 좌석(Redis 선점·presence + heartbeat/재접속/자동퇴실 + WebSocket)
>
> 브랜치: `feature/studyspace-realtime-redis-ws`
>
> 검증일: 2026-07-09 · 환경: 로컬 Redis 7 + Oracle Cloud ADB(`cloud` 프로파일)

---

## 1. 테스트 설계 방침

이 기능은 **동시성·시간 계산·비동기 상태 전이**가 얽혀 있어, 한 가지 방식만으로는 신뢰하기
어렵다. 그래서 아래 3계층으로 나눠 각 계층이 가장 잘 잡아내는 결함을 담당하게 설계했다.

```
        수동 end-to-end (실 DB + 실 Redis)   ← 통합 배선·실제 커밋·스케줄러·방송 관찰
      ────────────────────────────────────
      인프라 통합 (실 Redis, JUnit)          ← Lua CAD 등 Redis 원자성에 의존하는 안전장치
    ──────────────────────────────────────
    단위 (순수 도메인 + mock 오케스트레이션)  ← 상태머신·시간계산·스로틀·idempotency·경합 분기
  ────────────────────────────────────────
```

### 테스트 용이성을 위해 심어둔 장치
- **`Clock` 주입**: 모든 시간 판정을 주입된 `Clock`으로 하여 단위 테스트에서 시각을 고정
  (`Clock.fixed(...)`). "3초 뒤" 같은 실제 대기 없이 시간 경과 로직을 검증한다.
- **TTL·주기 외부화(`StudySpaceRealtimeProperties`)**: hold/presence/reconnect/pause/sweep
  간격을 `@ConfigurationProperties`로 빼서, 통합 테스트에선 초 단위로 줄여 자동퇴실을
  수십 초 안에 관찰했다(운영 기본값: presence 60s, reconnect 10m, pause 1h).
- **두 개의 생성자**: 프로덕션용(`@Autowired`)과 테스트용(package-private, `Clock` 주입)을
  분리해 스프링 컨텍스트 없이도 서비스를 조립해 테스트한다.

---

## 2. 계층별 테스트 방식과 대상

### 2.1 단위 — 순수 도메인 상태머신
- 파일: `src/test/java/.../entity/StudySessionStateMachineTest.java` (7 케이스)
- 방식: 인프라 없이 `StudySession` 엔티티만 조립해 상태 전이·시간 계산을 직접 단언.
- 검증 포인트
  - `disconnect`는 **마지막 `last_seen_at`까지만** 공부 시간 누적(끊긴 구간 미포함).
  - `reconnect`는 원래 상태로 복원하고 끊긴 구간을 시간에서 제외.
  - `pause`는 시간을 정지하고 `resume`은 이어서 누적.
  - PAUSED 상태에서 끊겨도 누적이 변하지 않고 재접속 시 PAUSED로 복원.
  - `disconnect`/`terminate`가 **idempotent**(중복 호출·이미 종료면 첫 결과 유지).
  - `elapsedSeconds(now)` = RUNNING이면 누적 + (now − lastResumedAt), 그 외 누적값.

### 2.2 단위 — 서비스 오케스트레이션(mock 리포지토리 + 실제 로직)
- 파일: `.../service/StudySessionServiceRealtimeTest.java` (13), `SeatMaintenanceServiceTest.java` (4),
  기존 `StudySessionServiceTest.java` (3)
- 방식: 리포지토리·Redis 서비스·이벤트 퍼블리셔를 Mockito로 대체하고, 서비스의 실제
  조율 로직만 검증. `ArgumentCaptor`로 발행된 방송 이벤트 타입을 단언.
- 검증 포인트
  - **heartbeat 스로틀**: `last-db-touch` 미통과 시 DB `last_seen_at` 쓰기 skip, presence는 항상 갱신.
  - DISCONNECTED 세션 heartbeat → 재접속 복구 + `clearReconnect` + RECONNECTED 방송.
  - 비소유자 heartbeat/pause/resume 거부.
  - **handleDisconnect idempotency**: 이미 끊김/종료·점유 없음이면 무방송 no-op.
  - 자동 퇴실(재접속·일시정지 만료): 점유 삭제 + 종료 사유 정확 + VACATED 방송 + Redis 정리.
  - orphan 세션 종결은 좌석 표시가 없으므로 **무방송**.
  - **선점 실패 시 DB 진입 자체 차단**(fail-fast).
  - **스윕 격리성**: 한 항목이 예외(낙관락 경합 등)를 던져도 나머지 항목은 계속 처리.
  - 스윕은 만료 대상만 선택(재접속창이 열려 있거나 presence가 살아 있으면 건너뜀).

### 2.3 인프라 통합 — 실제 Redis
- 파일: `.../service/SeatHoldServiceRedisTest.java` (4, Redis 없으면 자동 skip)
- 방식: 로컬 Redis에 실제 연결해 선점 락의 원자성을 검증(Oracle 불필요).
- 검증 포인트(**핵심 안전장치, 1순위**)
  - `tryHold` SETNX로 최초 1명만 성공, 나머지는 즉시 실패.
  - **"release old token must not delete new token"**: TTL 만료 후 다른 요청이 재선점한
    락을, 이전 요청의 release가 지우지 못함(Lua compare-and-delete).
  - 자기 토큰으로만 해제되고, null 토큰 release는 no-op.

### 2.4 수동 end-to-end — 실 DB + 실 Redis + 스케줄러 + 방송
- 방식: `cloud` 프로파일로 앱을 띄우되 TTL을 초 단위로 override
  (`presence-ttl=5s, presence-stale-threshold=6s, reconnect-window=8s, sweep-interval=3s,
  db-last-seen-throttle=2s`). 회원가입→로그인으로 실제 JWT를 발급받아 REST를 호출하고,
  **`redis-cli SUBSCRIBE seat-events`로 실시간 방송을 캡처**해 커밋 이후 발행을 눈으로 확인.
- 시드 데이터: 스터디룸 4개(공숲/서당/카페/오피스), 채널 3개, 좌석 24개 사용.

---

## 3. 수동 end-to-end 시나리오와 결과

TTL override: presence 5s · stale 6s · reconnect 8s · sweep 3s.

### 3.1 정상 학습 흐름 (점유→heartbeat→pause→resume→퇴실)
| 단계 | 관찰 | 판정 |
| --- | --- | --- |
| 좌석 점유 | RUNNING, DB row 생성, 좌석 `occupied:true`, `OCCUPIED` 방송 | ✅ |
| heartbeat(3초 간격) | `displayElapsedSeconds` 0 → 4 (서버 시각 누적), 방송 없음 | ✅ |
| pause | `PAUSED` 방송, 이후 heartbeat에도 경과초 **4에서 정지** | ✅ |
| resume | `RESUMED` 방송, RUNNING 복귀 | ✅ |
| 정상 퇴실 | COMPLETED / USER_EXIT, accumulated=4, `VACATED` 방송 | ✅ |

방송 순서: `OCCUPIED → PAUSED → RESUMED → VACATED` (heartbeat는 방송하지 않음 — 의도대로).

### 3.2 자동 퇴실 (heartbeat 끊김 → 시간초과)
방치 상태에서 관찰한 방송 타임라인:
```
17:34:30  OCCUPIED
17:34:36  DISCONNECTED     (+6s: presence 만료 + stale 감지 → presenceSweep)
17:34:45  VACATED / AUTO_TERMINATED  (+9s: reconnect 창 만료 → reconnectTimeoutSweep)
```
좌석도 해당 시점에 `occupied:false`로 전환. 설정한 TTL과 정확히 일치. ✅

### 3.3 재접속 복구 (만료 전 heartbeat)
```
17:36:04  OCCUPIED
17:36:13  DISCONNECTED     (방치로 끊김 판정)
17:36:14  RECONNECTED      (heartbeat 도착 → RUNNING 복구, 좌석 유지)
```
복구 후 `displayElapsedSeconds`는 **확인된 `last_seen_at`까지만** 인정(heartbeat 미전송
구간은 미크레딧). 규칙대로 동작. ✅

### 3.4 동시 점유 경합
같은 좌석에 서로 다른 두 사용자가 병렬로 점유 요청:
```
user1 → HTTP 200 (착석)
user2 → HTTP 409 SEAT_ALREADY_OCCUPIED
최종 좌석 점유자: 정확히 1명
```
Redis 선점락(빠른 실패) + DB 비관적 락·UNIQUE(최종 확정)의 이중 방어가 이중 부킹을 차단. ✅

---

## 4. 결과 요약

- **자동화 테스트: 47개 전부 통과 (실패 0, 스킵 0)**
  - 도메인 상태머신 7 · 서비스 오케스트레이션 20 · Redis 선점 4 · 엔티티 매핑/종료/기타 16
- **수동 end-to-end: 8개 시나리오 전부 통과** (실 DB + 실 Redis + 스케줄러 + 방송 관찰)
- 앱 기동 시 Oracle 연결(`DB_OK`), Redis `seat-events` 구독, STOMP 브로커 기동,
  서버 재기동 정리(StartupCleaner) 정상 확인.

### 검증 명령 재현
```bash
# 인프라
redis-server --daemonize yes            # 또는 docker compose up -d redis
# 자동화 테스트
./gradlew test
# 수동 end-to-end (짧은 TTL로 자동퇴실 관찰)
SPRING_PROFILES_ACTIVE=cloud ORACLE_WALLET_LOCATION=<wallet> \
  ./gradlew bootRun --args='--studyspace.realtime.presence-ttl=5s \
  --studyspace.realtime.presence-stale-threshold=6s \
  --studyspace.realtime.reconnect-window=8s --studyspace.realtime.sweep-interval=3s'
redis-cli SUBSCRIBE seat-events         # 방송 관찰
```

---

## 5. 코드 리뷰 반영 (2026-07-09)

동료 리뷰 후 아래를 수정하고 테스트를 추가했다(자동화 55개로 증가).

| 항목 | 문제 | 조치 |
| --- | --- | --- |
| 다중 연결 stale disconnect (P1) | 재접속/새 탭으로 살아 있는 세션이 이전 연결의 WS 종료로 끊길 수 있음 | **단일 연결 정책**: `session:{id}:current-ws`를 최신 join/heartbeat로 갱신하고, WS 종료가 현재 연결일 때만 `handleDisconnect` (`WebSocketEventListenerTest`) |
| 디버그 Runner 운영 경로 (P1) | `DbConnectionTestRunner`가 모든 기동에서 Oracle 전용 SQL 실행 | 파일 제거 |
| pause/resume no-op 방송 (P2) | 이미 그 상태여도 이벤트 발행 → 중복 방송 | 실제 상태 전이일 때만 publish (`pauseOnAlreadyPausedIsNoOp…`) |
| 상태 전이 시 last_seen 반영 (P2) | pause/resume이 presence·last_seen 미갱신 | 전이 시 `occupancy.touch()` + `presence.refresh()` 병행 |
| 재기동 grace 미적용 (P2) | 이미 DISCONNECTED인 세션은 startup grace 무시하고 첫 스윕에서 퇴실 | StartupCleaner가 DISCONNECTED 세션엔 `reconnect` 창을 grace만큼 seed → reconnect 스윕이 가드로 skip (`StudySpaceStartupCleanerTest`) |
| characterId 계약 (P3) | 이벤트 payload에 characterId 없음 | 아래 후속 항목으로 명시(§6) |

## 6. 알려진 한계·후속

- **STOMP 클라이언트 수신은 미검증**: 방송이 Redis 채널로 발행되는 것까지 실측했고,
  서버→STOMP relay 코드는 컨텍스트 로드로 배선만 확인. 실제 브라우저/`@stomp/stompjs`
  구독 수신은 프론트 연동 단계에서 검증 예정.
- **WS 종료 이벤트(SessionDisconnectEvent) 경로**: 자동퇴실은 REST 방치→presenceSweep
  경로로 실측. 실제 WS 연결 끊김 이벤트 트리거는 프론트 STOMP 연동 시 확인.
- **공유 dev DB에 테스트 유저·세션 이력 잔존**: 학습 세션은 설계상 물리삭제하지 않음.
  좌석 점유(seat_occupancy)는 전부 정상/자동 해제됨.
- Oracle Cloud ADB가 유휴로 멈추는 경향(ORA-03113) — 데모 전 미리 웜업 권장.
- **[계약 TODO] `SeatEventMessage.characterId` 부재**: 프론트 좌석 렌더링은 캐릭터 이미지를
  characterId로 선택하지만, 현재 `sf_user`에 캐릭터 컬럼이 없어 이벤트에 싣지 못한다. 결정 필요:
  (a) `sf_user`에 character 추가하고 이벤트/좌석 조회 응답에 포함, 또는 (b) 프론트가 별도
  소스(회원 프로필 조회)에서 캐릭터를 얻는다고 계약 확정. 좌석 렌더링 연동 전 정해야 한다.
