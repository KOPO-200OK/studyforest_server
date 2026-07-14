# SpotBugs 점검 결과 (2026-07-13)

`./gradlew spotbugsMain` (SpotBugs 4.8.6 + FindSecBugs) 실행 결과 정리.
리포트 원본: `build/reports/spotbugs/main.html`

## 1. 요약

- 분석 대상: 5,892 LOC, 206 클래스, 79 패키지
- 총 경고: **88건** (전부 Medium priority, High priority 0건)

| 카테고리 | 건수 | 실제 위험 | 비고 |
|---|---|---|---|
| Security | 6 | **오탐** | SQL 인젝션 4 + XSS 2, 전부 오탐으로 판정 |
| Malicious code vulnerability | 67 | **오탐(설계상 허용)** | 전부 `EI_EXPOSE_REP`/`EI_EXPOSE_REP2` (응답 DTO의 컬렉션 필드 노출) |
| Dodgy code | 8 | 6건 오탐 / 2건 이론상 가능 | 전부 `NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE` |
| Performance | 5 | 실제(경미) | `BX_UNBOXING_IMMEDIATELY_REBOXED`, 기능엔 무해 |
| Bad practice | 2 | 1건 오탐급 / 1건 경미 | `CT_CONSTRUCTOR_THROW`, `VA_FORMAT_STRING_USES_NEWLINE` |

**결론: 88건 중 실제 조치가 필요한 보안/기능 결함은 없음.** 대부분 SpotBugs/FindSecBugs의 패턴 매칭 한계로 인한 오탐이며, 근거는 아래에 항목별로 정리.

---

## 2. Security (6건) — 전부 오탐

### SQL_INJECTION_SPRING_JDBC (4건)
- 위치: `AdminService.java` — `getMembers()` (95, 105행), `getQuestions()` (321, 331행)
- 경고 이유: `jdbcTemplate.queryForObject`/`query`에 넘기는 SQL 문자열이 `StringBuilder`로 동적 조립되어(관리자 목록 검색의 동적 `WHERE`절), 컴파일타임 상수가 아니라는 이유로 FindSecBugs가 기계적으로 플래그함.
- **오탐인 이유**: 실제 코드를 확인하면 동적으로 붙이는 부분은 `AND`, `WHERE`, 컬럼명 등 고정 SQL 키워드뿐이고, 사용자 입력값(`keyword`, `userRole`, `isDeleted` 등)은 전부 `?` 플레이스홀더 + `params.add(...)`로 바인딩됨. 문자열 리터럴 결합 여부만 보고 플래그하는 규칙의 한계 — [2026-07-10 시큐어 코딩 점검](./security-review-20260710.md)에서도 "SQL 접근 전부 파라미터 바인딩(AdminService의 동적 WHERE절 포함) — 인젝션 없음"으로 이미 검증됨.

### XSS_SERVLET (2건)
- 위치: `CustomAccessDeniedHandler.java:37`, `CustomAuthenticationEntryPoint.java:37`
- 경고 이유: `ObjectMapper.writeValueAsString(...)` 결과를 `response.getWriter().write(...)`로 직접 쓰는 패턴을 XSS 싱크로 일괄 플래그함.
- **오탐인 이유**: 두 핸들러 모두 쓰기 전에 `response.setContentType(MediaType.APPLICATION_JSON_VALUE)`를 명시적으로 설정하므로 브라우저가 HTML로 해석하지 않음. 또한 응답 본문은 `"접근 권한이 없습니다"` 같은 고정 문자열이며 사용자 입력이 반영되지 않음. 분석기가 Content-Type 설정이나 문자열이 상수인지까지는 추적하지 못해서 생기는 오탐.

---

## 3. Malicious code vulnerability (67건) — 오탐(설계상 허용)

- 전부 `EI_EXPOSE_REP`(18건, getter가 내부 필드 참조 반환) / `EI_EXPOSE_REP2`(49건, 생성자가 파라미터를 방어적 복사 없이 필드에 저장) 패턴.
- 대상은 전부 응답(response) DTO/record 클래스(예: `AdminQuestionDetailResponse.choices()`, `AiGeneratedQuestionSetDetailResponse.questions()` 등) — API 응답으로 한 번 나가는 읽기 전용 객체이며, 여러 신뢰 경계를 넘나들며 공유되는 내부 가변 상태가 아님.
- **오탐/허용 가능 판단 이유**: 이 규칙은 원래 "신뢰할 수 없는 코드가 객체의 내부 컬렉션을 직접 들고 있다가 몰래 수정해서 불변식을 깨뜨리는" 시나리오를 막기 위한 것. 여기서는 DTO가 컨트롤러 → Jackson 직렬화 → HTTP 응답으로 한 방향으로만 흘러가고, 호출자가 그 리스트 레퍼런스를 다시 서버 내부 상태에 연결해 재사용하는 구조가 아니므로 실질적 위험이 없음. Java에서 모든 record/DTO에 방어적 복사를 강제하면 보일러플레이트만 늘어나 실익이 적은 전형적 케이스.

---

## 4. Dodgy code — NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE (8건)

### 서비스 레이어 6건 — 오탐
- 위치: `AdminService.getMemberDetail()`, `DashboardService.getSummary()` (3건), `StudyAnalysisService.getSummary()` (2건)
- 경고 이유: `jdbcTemplate.queryForObject(sql, RowMapper, ...)`의 일반 시그니처는 이론상 `null`을 반환할 수 있어, 결과값을 바로 `.method()`로 역참조하는 걸 SpotBugs가 잠재적 NPE로 플래그함.
- **오탐인 이유**: 실제 넘기는 `RowMapper`/`ResultSetExtractor` 람다를 보면 매번 `new XxxStats(...)`를 새로 생성해서 반환하거나(집계 쿼리라 결과가 항상 1행 보장), `!rs.next()`면 `BusinessException`을 던지도록 되어 있어 — 해당 메서드가 실제로 `null`을 반환하는 경로가 코드상 존재하지 않음. SpotBugs가 람다 내부까지 흐름 분석을 하지 못해 `JdbcTemplate.query*`의 일반 계약(null 가능)만 보고 판단하는 데서 오는 오탐.

### WS 컨트롤러 2건 — 이론상 가능(저위험)
- 위치: `StudySpaceWsController.join()` (51행), `.heartbeat()` (64행) — `accessor.getSessionId()`
- `SimpMessageHeaderAccessor.getSessionId()`는 시그니처상 `null`을 반환할 수 있음. 다만 STOMP 프레임 처리 경로에서는 세션 ID 헤더가 항상 채워져 있어 실무적으로는 발생하지 않는 경로. 완전한 오탐이라기보단 "이론상 가능하지만 이 컨텍스트에선 사실상 도달 불가"에 가까움 — 필요시 방어적 null 체크를 추가해도 되지만 우선순위는 낮음.

---

## 5. Performance — BX_UNBOXING_IMMEDIATELY_REBOXED (5건)

- 위치: `AdminService.count()`(444행)/`getMembers()`(136행)/`getQuestions()`(370행), `DashboardService.getSummary()`(144-146행), `StudyAnalysisService.getSummary()`(93행)
- `long` 값을 `Long.valueOf(...)`로 감쌌다가 즉시 다시 언박싱하는 패턴 — 실제로 존재하는 비효율이지만 카운트 쿼리 결과 반환값 정도라 성능 영향은 미미함. 기능적 결함 아님, 원하면 정리 가능한 스타일 이슈.

---

## 6. Bad practice (2건)

### CT_CONSTRUCTOR_THROW — 오탐급
- 위치: `JwtProvider.java:22` — 생성자에서 `Keys.hmacShaKeyFor(secret.getBytes())` 호출 시 `JWT_SECRET`이 너무 짧으면 예외 발생 가능.
- 규칙 취지는 "생성자 예외 + 커스텀 `finalize()`" 조합으로 인한 Finalizer Attack 방어인데, `JwtProvider`는 `finalize()`를 오버라이드하지 않고 Spring이 관리하는 싱글턴 빈이라 공격자가 임의로 서브클래싱/재생성할 수 있는 경로가 없음. 시크릿이 잘못됐을 때 앱이 기동 시점에 fail-fast 하는 건 오히려 바람직한 동작.

### VA_FORMAT_STRING_USES_NEWLINE — 경미, 실제
- 위치: `AiService.buildSessionPrompt()`(496행) — 포맷 문자열에 `%n` 대신 `\n` 사용.
- 실제 스타일 이슈지만 AI 프롬프트 텍스트 생성용이라 플랫폼별 줄바꿈 차이가 문제 되지 않음(파일에 쓰거나 콘솔 출력하는 게 아님). 기능적 영향 없음.

---

## 7. 권고

- 지금 시점에서 즉시 조치가 필요한 항목은 없음.
- 다만 SQL 인젝션(4건)·XSS(2건) 오탐은 반복적으로 리포트에 노이즈를 남기므로, `config/spotbugs/exclude.xml`에 해당 규칙(`SQL_INJECTION_SPRING_JDBC`, `XSS_SERVLET`)을 클래스/메서드 단위로 명시적 suppress 처리해두면 다음 점검부터 진짜 이슈만 눈에 띌 것.
- `EI_EXPOSE_REP*`(67건)는 프로젝트 전반의 DTO 설계 패턴이라 개별 처리보다는 응답 DTO 패키지 단위로 룰 자체를 exclude하는 편이 합리적.
