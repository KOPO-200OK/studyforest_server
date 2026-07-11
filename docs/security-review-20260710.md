# 시큐어 코딩 점검 결과 (2026-07-10)

## 1. 적용된 도구

- **SpotBugs + FindSecBugs**: `build.gradle`에 플러그인만 있고 `effort`/`reportLevel`/리포트 설정과 `config/spotbugs/exclude.xml`은 실제로는 커밋되어 있지 않았음(2026-07-12 확인). 이번 점검은 `effort=max`, `reportLevel=medium`, HTML/XML 리포트 활성화 설정을 로컬에서 **임시로만** 적용해 실행하고 결과 분석 후 되돌렸다 — 저장소에는 반영되지 않음. 도구 설정을 실제로 커밋할지는 팀 논의 필요(4장 참고).
- **OWASP Dependency-Check**: 아직 미도입. NVD 데이터베이스 초기 동기화가 네트워크 상황에 따라 오래 걸려 이번 점검 범위에서는 제외 — 별도 작업으로 도입 필요.
- 재현 방법: `build.gradle`의 `spotbugs` 플러그인 블록에 `effort = 'max'`, `reportLevel = 'medium'`을 추가하고 `./gradlew spotbugsMain` 실행 (리포트는 `build/reports/spotbugs/main.xml`, `main.html`).

## 2. 미해결 취약점 (조치 필요)

### [HIGH] 비밀번호 재설정 — 계정 탈취 가능
- `src/main/java/com/gongsoop/member/service/MemberService.java:42-47` (`findEmail`), `:56-61` (`resetPassword`)
- `src/main/java/com/gongsoop/member/controller/AuthController.java:38-48` (`/api/v1/auth/find-email`, `/api/v1/auth/reset-password`, 둘 다 `permitAll`)
- **문제**: `find-email`이 이름+생년월일만으로 실제 이메일을 반환(PII 노출)하고, `reset-password`는 이메일+이름+생년월일 일치만으로 비밀번호를 즉시 변경 — 이메일 소유 증명(OTP/인증 링크) 절차가 전혀 없음.
- **공격 시나리오**: 피해자의 이름+생년월일만 알면 ① find-email로 이메일 확보 → ② reset-password로 비밀번호 변경 → 계정 완전 탈취. 이메일함을 건드릴 필요 없음.
- **수정 방향**: 이메일로 만료시간이 있는 1회용 토큰을 발송하고, 토큰 검증 후에만 비밀번호 변경을 허용하도록 2단계 플로우로 분리. 현재 프로젝트에는 메일 발송 인프라(`spring-boot-starter-mail`, SMTP 설정)와 토큰 저장 테이블이 없어 신규 구축 필요 — **보류 중, 팀 논의 필요**.

### [MEDIUM] 음성 시그널링 — target 검증 누락
- `src/main/java/com/gongsoop/voice/controller/VoiceWsController.java:34-42`
- `src/main/java/com/gongsoop/voice/service/VoiceRoomService.java:126-129` (`validateSignal`)
- **문제**: 발신자(sender)가 해당 존에 착석했는지는 검증하지만, `message.targetEmail()`이 그 존의 실제 참가자인지는 검증하지 않음. `targetEmail`은 클라이언트가 임의로 지정 가능.
- **공격 시나리오**: 아무 존에나 착석한 사용자가 음성방에 있지도 않은 임의의 다른 회원에게 위조된 WebRTC 시그널(SDP/ICE)을 주입 가능. 발신자 표시는 정상이라 피해자 입장에서는 정상 시그널처럼 보임.
- **수정 방향**: `validateSignal`에서 `targetEmail`도 Redis 참가자 집합(`participantsKey(studyZoneId)`)에 속하는지 확인.

### [MEDIUM] STOMP SUBSCRIBE 인가 검사 누락 — 참가자 이메일/이름 노출
- `src/main/java/com/gongsoop/global/config/WebSocketConfig.java:40`
- `src/main/java/com/gongsoop/global/security/StompAuthChannelInterceptor.java:36-38`
- **문제**: 인터셉터가 `CONNECT` 프레임에서만 JWT를 검증하고 `SUBSCRIBE`는 그대로 통과. `enableSimpleBroker`가 모든 `/topic/**` 구독을 허용하므로, 로그인한 사용자면 누구나 임의 존의 `/topic/voice/zones/{studyZoneId}/participants`를 구독해 실명+이메일이 담긴 입장/퇴장 이벤트를 수집 가능.
- **수정 방향**: 구독 시점에 destination의 `studyZoneId`에 대해 요청자가 실제 참가자인지 확인하는 로직을 인터셉터에 추가.

## 3. 검증 완료 — 문제 없음

- STOMP CONNECT 인증 (`StompAuthChannelInterceptor`)
- 좌석/세션 소유권 체크 (`StudySessionService.isSessionOwner/heartbeat`)
- 음성방 입장 zone-mismatch 체크 (`VoiceRoomService.validateJoinableZone`)
- `InquiryService` 소유자 검증 (읽기/수정/삭제 모두 `member.getId()` 비교) + `AdminInquiryController`의 이중 권한 체크(`SecurityConfig` 매처 + `validateAdmin`)
- 캐릭터 프로필 응답(`SeatStatusResponse`, `CharacterResponse`)에 PII 없음
- `SeatHoldService`/`PresenceService`의 CAS 기반 소유권 검증 — 타인 좌석 점유/해제 불가
- CORS 설정 (명시적 allowlist, `allowCredentials(true)`와 조합해도 안전)
- CSRF 비활성화 (JWT stateless API라 타당)
- SQL 접근 전부 파라미터 바인딩 (`AdminService`의 동적 `WHERE`절 포함) — 인젝션 없음
- 관리자 API 이중 권한 체크 (`hasRole("ADMIN")` + 서버측 재검증)
- JWT 시크릿 하드코딩 없음 (`${JWT_SECRET}` 환경변수, 기본값 없음)
- AI 서버 클라이언트 — 고정 base-url, 사용자 제어 불가 (SSRF 없음)

## 4. 2026-07-12 추가 점검 — SpotBugs + FindSecBugs 실행 결과 (코드 미수정, 발견 사항만 기록)

`hyunju` 브랜치(오늘/이번 주 학습시간 기능 추가분 포함)를 대상으로 `effort=max`, `reportLevel=medium` 설정으로 `./gradlew spotbugsMain`을 처음 실행. 총 84건 발견 → 코드를 하나씩 대조해 오탐/실제 여부를 판단. **이 점검은 리포트 확인 목적이라 코드·설정 변경은 전부 되돌렸고, 아래는 전부 "아직 조치 안 된" 상태다.**

### 4.1 오탐으로 판단 (조치 불필요)

| 유형 | 건수 | 위치 | 오탐 판단 근거 |
| --- | --- | --- | --- |
| `SQL_INJECTION_SPRING_JDBC` | 4 | `AdminService.getMembers/getQuestions`의 동적 `WHERE`절 (95, 105, 321, 331행) | `StringBuilder`로 이어붙이는 것은 `" AND ... = ? "` 같은 정적 SQL 조각뿐이고, `keyword`/`userRole`/`era`/`category`/`isDeleted` 등 실제 사용자 입력은 전부 `?` 바인딩 파라미터(`params.toArray()`)로만 전달됨을 코드로 직접 확인. FindSecBugs가 "동적으로 조립된 SQL 문자열"이라는 형태만 보고 taint로 오인하는 전형적 패턴. |
| `XSS_SERVLET` | 2 | `CustomAccessDeniedHandler:37`, `CustomAuthenticationEntryPoint:37` | 응답 본문이 `ApiResponse.failure(고정 코드, 고정 메시지)`를 JSON 직렬화한 값뿐이며 요청 파라미터·예외 메시지 등 사용자 입력을 전혀 반영하지 않음. `Content-Type`도 `application/json`. |
| `EI_EXPOSE_REP` / `EI_EXPOSE_REP2` | 18 + 54 = 72 | 전 모듈(생성자 주입 필드, JPA 엔티티/DTO record의 list·getter 등) | Spring 생성자 주입으로 협력 객체 참조를 저장하는 표준 패턴이거나, 요청마다 새로 만들어 즉시 JSON 직렬화 후 버려지는 응답 객체. 외부 호출자가 내부 가변 상태를 되돌려주고 변조할 경로가 존재하지 않아 전부 오탐. |

### 4.2 실제 문제로 판단 — 조치 필요 (미수정)

- **`CT_CONSTRUCTOR_THROW`(1건)** — `src/main/java/com/gongsoop/global/security/JwtProvider.java:22-24`. 생성자에서 `Keys.hmacShaKeyFor(...)`가 시크릿이 짧으면 예외를 던짐. 생성자에서 예외가 나면 객체가 부분 초기화된 채로 남아 파이널라이저 공격에 노출될 수 있는 이론적 위험. **제안**: 서브클래싱을 막기 위해 클래스에 `final` 추가(비용 거의 없는 수정).
- **`NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE`(`StudyAnalysisService.getSummary()` 범위 2건)** — `src/main/java/com/gongsoop/study/service/StudyAnalysisService.java:101,106`. `jdbcTemplate.queryForObject(...)`가 이론상 `null`을 반환할 수 있는 시그니처인데 바로 `.totalSolvedCount()`/`.submittedMockExamCount()`를 호출. 실제로는 두 쿼리 모두 집계(COUNT/AVG) 결과라 행이 항상 1건 반환되므로 실질적 NPE 경로는 없지만, 방어적으로 `null` 가드를 추가해두는 편이 안전.

### 4.3 저위험 · 기존 패턴 — 우선순위 낮음

- **`NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE`(나머지 6건)**: `AdminService`, `DashboardService`, `StudySpaceWsController`에 동일 패턴이 이미 있었음(오늘 작업으로 새로 생긴 것 아님). 각 쿼리의 `RowMapper`가 항상 non-null 레코드를 반환하므로 실질적 NPE 경로는 없지만, 방어적 코딩 관점에서는 정리 여지가 있음.
- **`BX_UNBOXING_IMMEDIATELY_REBOXED`(5건)**: `x == null ? 0L : x` 형태의 null 가드 관용구가 `AdminService`/`DashboardService`/`StudyAnalysisService`에 반복 사용됨. 동작에는 문제 없고 성능 영향도 무시 가능한 수준 — 스타일 통일 차원의 이슈.
- **`VA_FORMAT_STRING_USES_NEWLINE`(1건)**: `AiService.buildSessionPrompt`에서 `String.format`에 `%n` 대신 `\n` 사용. 이식성 관련 스타일 이슈, 보안과 무관.

### 4.4 참고 — 도구 설정을 저장소에 반영하려면

이번 점검에서 쓴 `effort=max`/`reportLevel=medium`/HTML 리포트 설정과 오탐 억제용 `config/spotbugs/exclude.xml`은 로컬 실행 후 되돌려서 저장소에는 없다. 팀에서 SpotBugs를 상시 도구로 쓰기로 하면, 4.1의 오탐 근거를 그대로 `exclude.xml`에 옮기고 4.2/4.3을 이슈로 등록해 트래킹하는 것을 권장.

## 5. 참고 — 로컬 개발 환경 이슈 (보안과 무관, 참고용)

로컬에서 `cloud` 프로필로 Oracle Autonomous DB(지갑 기반 TCPS)에 연결할 때 `PKIX path building failed` 오류가 발생하면, JDK의 신뢰 인증서 문제가 아니라 **Avast 등 TLS 검사(SSL scanning) 기능이 있는 백신 프로그램이 인증서를 가로채고 있을 가능성**이 큽니다. 해당 백신에서 `java.exe`를 예외 처리하거나 HTTPS 검사를 끄면 해결됩니다.
