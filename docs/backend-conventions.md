# 백엔드 설계 규칙

이 문서는 API, DTO, 패키지, ERD 및 Oracle 스키마 설계를 위한 팀 공통 체크리스트다.
각 담당자가 작성한 모듈별 ERD는 팀 검토와 통합이 끝날 때까지 초안으로 관리한다.

## API 규칙

- 기본 경로는 `/api/v1`을 사용한다.
- 리소스명은 복수형 명사, URL은 소문자 kebab-case를 사용한다.
  예: `/api/v1/study-rooms`
- 조회는 `GET`, 생성 및 업무 동작은 `POST`, 전체 수정은 `PUT`, 일부 수정 및 상태
  변경은 `PATCH`, 삭제는 `DELETE`를 사용한다.
- 관리자 리소스는 `/api/v1/admin` 아래에 배치한다. 실제 권한은 Spring Security에서
  검사한다.
- 페이지 번호는 0부터 시작한다. 기본·최대 페이지 크기와 정렬 가능한 필드는 서버에서
  제한한다.
- HTTP 상태 코드는 `200`, `201`, `204`, `400`, `401`, `403`, `404`, `409`, `500`을
  용도에 맞게 일관되게 사용한다.
- 오류 코드는 대문자 snake_case로 작성한다.
- Stack Trace, SQL, Oracle 오류 메시지, 내부 파일 경로, 인증정보 및 Wallet 정보를
  응답에 노출하지 않는다.

스터디 공간 API 경로 예시:

```text
GET    /api/v1/study-rooms
GET    /api/v1/study-rooms/{studyRoomId}/seats
POST   /api/v1/seats/{seatId}/occupancy
DELETE /api/v1/seats/{seatId}/occupancy
POST   /api/v1/study-sessions
PATCH  /api/v1/study-sessions/{studySessionId}/status
GET    /api/v1/admin/study-rooms
```

## 공통 응답 규칙

성공 응답은 `ApiResponse<T>`를 사용한다.

```json
{
  "success": true,
  "data": {},
  "message": "요청이 정상 처리되었습니다."
}
```

오류 응답은 `ErrorResponse`와 정의된 `ErrorCode`를 사용한다.

```json
{
  "success": false,
  "code": "SEAT_ALREADY_OCCUPIED",
  "message": "이미 사용 중인 좌석입니다."
}
```

도메인 예외는 `BusinessException`을 상속한다. 예상하지 못한 예외의 상세 내용은 서버
로그에만 기록하고, 클라이언트에는 공통 `INTERNAL_SERVER_ERROR` 응답을 반환한다.

## DTO 규칙

- Entity를 API 요청이나 응답으로 직접 사용하지 않는다.
- 요청 DTO와 응답 DTO를 분리한다. JDK 21의 record를 사용할 수 있다.
- 사용자가 입력하는 모든 필드에 필요한 Bean Validation을 적용한다.
- 서버가 관리하는 필드와 민감정보는 요청 및 응답 DTO에서 제외한다.
- 생성 요청은 `Create{Domain}Request`, 수정 요청은 `Update{Domain}Request`, 상태 변경
  요청은 `Change{Domain}StatusRequest` 형식으로 작성한다.
- 응답은 `{Domain}SummaryResponse`, `{Domain}DetailResponse`, `{Domain}Response` 형식으로
  작성한다.
- DTO는 `{domain}.dto.request`와 `{domain}.dto.response` 패키지로 분리한다.
- 단순한 Entity 변환은 응답 DTO의 정적 팩토리 메서드를 사용하고, 복잡한 변환은 별도
  Mapper를 사용한다.

## 패키지 구조와 책임

```text
com.gongsoop
├── global
│   ├── config
│   ├── security
│   ├── exception
│   ├── response
│   └── converter
└── {domain}
    ├── controller
    ├── service
    ├── repository
    ├── entity
    └── dto
        ├── request
        └── response
```

- Controller: HTTP 요청·응답과 요청값 검증만 담당한다. 비즈니스 로직을 작성하거나
  Entity를 직접 반환하지 않는다.
- Service: 비즈니스 규칙, 상태 전이 및 트랜잭션 경계를 담당한다.
- Repository: 데이터 조회와 저장만 담당한다. 쿼리 파라미터는 항상 바인딩한다.
- Entity: DB 매핑과 자신의 불변식을 관리한다. 공개 Setter는 최소화한다.
- 조회 Service는 `@Transactional(readOnly = true)`를 기본으로 하고, 생성·수정·삭제에는
  `@Transactional`을 사용한다.

## ERD 및 Oracle 19c 규칙

- 테이블과 컬럼명은 영문 snake_case, 테이블명은 단수형 명사를 사용한다.
- Oracle 예약어와 따옴표로 감싼 식별자를 사용하지 않는다. 이름은 가능하면 30자 이내로
  작성한다.
- 기본키 이름은 `{table_name}_id`로 작성하고, Oracle `NUMBER(19)`와 Java `Long`으로
  매핑한다. 기본키 생성에는 Oracle Sequence를 사용한다.
- Sequence 이름은 `seq_{table}`로 작성한다. JPA `allocationSize`는 팀에서 확정한
  Sequence 증가·캐시 정책과 맞춘다.
- 외래키 컬럼명은 참조 대상 기본키명과 동일하게 작성한다. 모든 관계, 카디널리티 및
  삭제 정책을 ERD에 표시한다.
- 공통 시간 컬럼은 `created_at`, `updated_at`, 필요한 경우 `deleted_at`을 사용한다.
- 학습·풀이·감사 이력은 물리 삭제보다 논리 삭제를 우선 검토한다.
- Oracle 19c의 참·거짓 값은 `CHAR(1)`의 `Y`·`N`과 CHECK 제약조건으로 표현하고,
  공통 JPA Converter를 사용한다.
- AI 답변이나 긴 해설은 `VARCHAR2`에 억지로 담지 않고 `CLOB`을 사용한다.

제약조건 명명 형식:

```text
PK        pk_{table}
FK        fk_{table}_{reference}
UNIQUE    uk_{table}_{column}
CHECK     ck_{table}_{column}
INDEX     idx_{table}_{column}
SEQUENCE  seq_{table}
```

인조 기본키가 있더라도 업무상 중복을 막아야 하는 값에는 UNIQUE 제약조건을 적용한다.
`@ManyToOne`은 LAZY를 명시하고, 양방향 연관관계는 꼭 필요한 경우에만 사용한다.
Cascade 및 orphanRemoval은 Entity 생명주기를 검토한 후 적용한다.

## 프로필 및 스키마 변경 규칙

- `univ`: 교내망에서만 접속할 수 있는 학교 Oracle 서버 환경이다.
- `cloud`: 각 개발자의 Oracle Cloud 계정과 로컬 Wallet을 사용하는 환경이다.
- `test`: 별도 DB가 필요한 테스트가 아니라면 실제 DB 없이 자동 테스트를 실행하는
  환경이다.
- 인증정보, JDBC URL, 실제 프로필 파일 및 Wallet 파일은 Git에 커밋하지 않는다.
- 공용 DB에서는 `spring.jpa.hibernate.ddl-auto`를 `none` 또는 `validate`로 유지한다.
- 팀에서 ERD를 확정한 뒤에는 Hibernate `create`·`update`에 의존하지 않고, Oracle
  스키마 변경 사항을 마이그레이션 SQL로 버전 관리한다.

## 내일 ERD 통합 체크리스트

- 모든 모듈이 동일한 PK, FK, Sequence, 시간 및 Boolean 규칙을 사용하는지 확인한다.
- 모듈 사이의 외래키마다 관리 주체와 삭제 정책을 합의한다.
- 중복된 개념과 Oracle 예약어가 있는지 확인하고 정리한다.
- 업무상 고유한 값이 UNIQUE 제약조건으로 표현됐는지 확인한다.
- AI 답변과 긴 해설이 `CLOB`으로 설계됐는지 확인한다.
- 합의가 끝난 뒤 테이블 정의서와 마이그레이션 SQL을 함께 작성·수정한다.
