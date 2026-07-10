# 시큐어 코딩 점검 결과 (2026-07-10)

## 1. 적용된 도구

- **SpotBugs + FindSecBugs**: 기존에 설정되어 있었음 (`build.gradle`). `effort=max`, `reportLevel=medium`으로 강화하고 HTML 리포트 활성화, `config/spotbugs/exclude.xml` 추가.
- **OWASP Dependency-Check**: 신규 추가 (`org.owasp.dependencycheck` 플러그인). `config/dependency-check/suppressions.xml` 추가. `CVSS >= 9`면 빌드 실패하도록 설정.
- 실행: `./gradlew spotbugsMain` / `./gradlew dependencyCheckAnalyze` (리포트는 `build/reports/`)

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

## 4. 참고 — 로컬 개발 환경 이슈 (보안과 무관, 참고용)

로컬에서 `cloud` 프로필로 Oracle Autonomous DB(지갑 기반 TCPS)에 연결할 때 `PKIX path building failed` 오류가 발생하면, JDK의 신뢰 인증서 문제가 아니라 **Avast 등 TLS 검사(SSL scanning) 기능이 있는 백신 프로그램이 인증서를 가로채고 있을 가능성**이 큽니다. 해당 백신에서 `java.exe`를 예외 처리하거나 HTTPS 검사를 끄면 해결됩니다.
