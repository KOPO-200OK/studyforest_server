# Oracle Cloud DB 설정 방법

팀원이 집이나 교외에서 개발할 때는 `cloud` 프로필과 본인에게 발급된 Oracle Cloud
계정을 사용한다. 다른 팀원의 계정이나 Wallet을 공유하지 않는다.

`univ` 프로필은 학교 Oracle 접속정보가 확정될 때까지 사용을 보류한다.

## 1. Wallet 준비

본인에게 제공된 Oracle Wallet을 로컬의 안전한 디렉터리에 압축 해제한다. Wallet은
절대 저장소 내부에 넣거나 Git에 커밋하지 않는다.

Wallet 디렉터리에는 다음 파일이 있어야 한다.

```text
cwallet.sso
ewallet.p12
sqlnet.ora
tnsnames.ora
```

## 2. Cloud 프로필 파일 준비

`src/main/resources/application-example.yml`의 `cloud` 부분을 복사해
`src/main/resources/application-cloud.yml`을 만든다. 이 파일은 `.gitignore`에 등록되어
있으므로 커밋하지 않는다.

## 3. 환경변수 설정

애플리케이션을 실행할 터미널에서 다음 환경변수를 설정한다.

```bash
export CLOUD_DB_URL='jdbc:oracle:thin:@dinkdb_medium'
export CLOUD_DB_USERNAME='DA26본인번호'
export ORACLE_WALLET_LOCATION='/본인의/Wallet_디렉터리/절대경로'
read -rs 'CLOUD_DB_PASSWORD?Cloud DB 비밀번호: '
export CLOUD_DB_PASSWORD
echo
```

비밀번호는 저장소, 문서, 메신저 또는 셸 기록에 직접 입력하지 않는다. 초기 비밀번호
규칙은 팀 내부의 안전한 채널에서 별도로 확인한다.

환경변수 설정 여부는 값을 출력하지 않고 다음과 같이 확인한다.

```bash
for v in CLOUD_DB_URL CLOUD_DB_USERNAME CLOUD_DB_PASSWORD ORACLE_WALLET_LOCATION; do
  [[ -n ${(P)v} ]] && echo "$v: OK" || echo "$v: MISSING"
done
```

## 4. 실행 및 연결 확인

```bash
./gradlew bootRun --args='--spring.profiles.active=cloud'
```

로그에 `Started StudyforestApplication`이 출력되고 Oracle 연결 오류가 없다면 정상이다.
Spring Security의 임시 비밀번호 경고는 인증 기능 구현 전 개발 단계에서는 정상이다.

실행을 종료할 때는 `Ctrl+C`를 누른다.

## 주의사항

- 환경변수는 해당 터미널 세션을 종료하면 사라진다.
- `application-cloud.yml`, `.env`, Wallet 및 인증정보는 커밋하지 않는다.
- 공용 DB에서는 `spring.jpa.hibernate.ddl-auto`를 `none` 또는 `validate`로 유지한다.
- 스키마 변경은 팀에서 ERD를 확정한 뒤 마이그레이션 SQL로 관리한다.
