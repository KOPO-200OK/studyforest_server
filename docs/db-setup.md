# Oracle DB 설정 방법

개발 장소에 따라 다음 프로필을 사용한다.

| 프로필 | 사용 장소 | 연결 대상 | Wallet |
| --- | --- | --- | --- |
| `cloud` | 집·교외 | 각 개발자의 Oracle Cloud 계정 | 필요 |
| `univ` | 학교 내부망 | 학교 Oracle RAC | 불필요 |
| `test` | 자동 테스트 | 기본적으로 실제 DB를 사용하지 않음 | 불필요 |

계정, 비밀번호, 실제 프로필 설정 및 Wallet은 Git에 커밋하지 않는다.

## Cloud 환경

### 1. Wallet 준비

본인에게 제공된 Oracle Wallet을 로컬의 안전한 디렉터리에 압축 해제한다. 다른 팀원의
계정이나 Wallet을 공유하지 않는다.

Wallet 디렉터리에는 다음 파일이 있어야 한다.

```text
cwallet.sso
ewallet.p12
sqlnet.ora
tnsnames.ora
```

### 2. Cloud 프로필 파일 준비

`src/main/resources/application-example.yml`의 `cloud` 부분을 복사해
`src/main/resources/application-cloud.yml`을 만든다. 이 파일은 `.gitignore`에 등록되어
있으므로 커밋하지 않는다.

### 3. 환경변수 설정

애플리케이션을 실행할 터미널에서 다음 환경변수를 설정한다.

```bash
export CLOUD_DB_URL='jdbc:oracle:thin:@dinkdb_medium'
export CLOUD_DB_USERNAME='DA26본인번호'
export ORACLE_WALLET_LOCATION='/본인의/Wallet_디렉터리/절대경로'
read -rs 'CLOUD_DB_PASSWORD?Cloud DB 비밀번호: '
export CLOUD_DB_PASSWORD
echo
```

초기 비밀번호 규칙은 저장소에 기록하지 않고 팀 내부의 안전한 채널에서 별도로 확인한다.

### 4. 실행

```bash
./gradlew bootRun --args='--spring.profiles.active=cloud'
```

## 학교 RAC 환경

`univ` 프로필은 학교 내부망에서만 사용한다. Wi-Fi를 사용할 때는 `DA_KOPO`에
접속해야 하며, 학교 유선 내부망에서도 사용할 수 있다. 집, 일반 Wi-Fi 또는 모바일
핫스팟에서는 학교 RAC에 접속할 수 없으므로 `cloud` 프로필을 사용한다. 학교에서 별도
VPN을 제공한 경우에는 VPN 연결 후 접속할 수 있다.

Wallet이나 TNS 별칭 대신 두 RAC 서버의 주소, 포트 및 서비스명을 포함한 JDBC URL로
직접 연결한다.

### 1. Univ 프로필 파일 준비

`src/main/resources/application-example.yml`의 `univ` 부분을 복사해
`src/main/resources/application-univ.yml`을 만든다. 이 파일도 커밋하지 않는다.

### 2. 환경변수 설정

두 RAC 노드의 장애 조치와 부하 분산을 사용할 수 있도록 다음 URL을 권장한다.

```bash
export UNIV_DB_URL='jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS_LIST=(LOAD_BALANCE=ON)(FAILOVER=ON)(ADDRESS=(PROTOCOL=TCP)(HOST=192.168.217.202)(PORT=1521))(ADDRESS=(PROTOCOL=TCP)(HOST=192.168.217.206)(PORT=1521)))(CONNECT_DATA=(SERVICE_NAME=KOPODA)))'
export UNIV_DB_USERNAME='DA2610'
read -rs 'UNIV_DB_PASSWORD?학교 DB 비밀번호: '
export UNIV_DB_PASSWORD
echo
```

접속 문제를 진단할 때는 RAC 노드 하나만 지정해 각각 확인할 수 있다.

```text
jdbc:oracle:thin:@//192.168.217.202:1521/KOPODA
jdbc:oracle:thin:@//192.168.217.206:1521/KOPODA
```

학교 DB 계정과 비밀번호 규칙은 Cloud DB와 다르다. 학교에서 발급받은 값을 사용하고
저장소나 셸 기록에 직접 남기지 않는다.

### 3. 실행

```bash
./gradlew bootRun --args='--spring.profiles.active=univ'
```

## 환경변수 확인

값을 출력하지 않고 현재 프로필에 필요한 환경변수의 설정 여부만 확인한다.

Cloud:

```bash
for v in CLOUD_DB_URL CLOUD_DB_USERNAME CLOUD_DB_PASSWORD ORACLE_WALLET_LOCATION; do
  [[ -n ${(P)v} ]] && echo "$v: OK" || echo "$v: MISSING"
done
```

학교 RAC:

```bash
for v in UNIV_DB_URL UNIV_DB_USERNAME UNIV_DB_PASSWORD; do
  [[ -n ${(P)v} ]] && echo "$v: OK" || echo "$v: MISSING"
done
```

로그에 `Started StudyforestApplication`이 출력되고 Oracle 연결 오류가 없다면 정상이다.
Spring Security의 임시 비밀번호 경고는 인증 기능 구현 전 개발 단계에서는 정상이다.
실행 종료는 `Ctrl+C`를 사용한다.

## 공통 주의사항

- 환경변수는 해당 터미널 세션을 종료하면 사라진다.
- `application-cloud.yml`, `application-univ.yml`, `.env`, Wallet 및 인증정보는 커밋하지
  않는다.
- 공용 DB에서는 `spring.jpa.hibernate.ddl-auto`를 `none` 또는 `validate`로 유지한다.
- 스키마 변경은 팀에서 ERD를 확정한 뒤 마이그레이션 SQL로 관리한다.
