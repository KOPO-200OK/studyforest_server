# GitHub Actions 기반 EC2 자동 배포

## 구성 개요

StudyForest는 포트폴리오 공개를 위해 서비스 저장소를 Public으로 유지하면서 `dev` 브랜치를 하나의 EC2 인스턴스에 자동 배포한다. 각 저장소가 자기 서비스의 워크플로를 관리한다.

- `studyforest_server`: Compose의 `server` 서비스 배포
- `studyforest_client`: Compose의 `client` 서비스 배포
- `studyforest_AI`: Compose의 `ai` 서비스 배포
- Redis: 인프라 서비스이므로 애플리케이션 배포 시 재시작하지 않음

운영 `compose.yaml`과 `.env`는 EC2의 `/home/ubuntu/StudyForest`에 둔다. 배포 시 해당 저장소만 fast-forward 방식으로 갱신하고, 변경된 서비스의 이미지만 빌드해 컨테이너를 교체한다.

## 일반 PEM 대신 제한된 전용 키를 사용한 이유

Public 저장소라도 GitHub Actions Secrets의 값이 공개되지는 않는다. 하지만 저장소의 워크플로 수정 권한을 탈취한 공격자는 Secret 사용을 시도할 수 있다. 일반 EC2 PEM을 Secret으로 사용하면 배포에 필요한 수준보다 훨씬 큰 권한을 부여하게 된다.

따라서 인프라 담당자의 PEM과 별개인 Actions 전용 ED25519 키를 생성했다. EC2의 `authorized_keys`에는 다음과 같이 OpenSSH 제한과 강제 명령을 적용했다.

```text
restrict,command="/usr/local/sbin/studyforest-deploy" ssh-ed25519 <public-key> studyforest-github-actions
```

`restrict`는 PTY, TCP 포워딩, SSH agent 포워딩, X11 포워딩과 사용자 시작 스크립트를 차단한다. `command=...`는 SSH 클라이언트가 요청한 명령을 직접 실행하지 않고, root 소유의 배포 디스패처만 실행한다. 이에 따라 대화형 셸, 임의 명령, SCP/SFTP 및 SSH 터널링이 차단된다.

배포 디스패처는 `SSH_ORIGINAL_COMMAND`를 검사해 다음 세 값만 허용한다.

```text
deploy server
deploy client
deploy ai
```

그 외 값은 종료 코드 126으로 거부한다. `/usr/local/sbin/studyforest-deploy`는 `root:root`, 권한 `755`로 설치하여 `ubuntu` 배포 계정이 내용을 변경하지 못하게 했다.

## 배포 흐름

1. 각 저장소의 `dev` 브랜치에 커밋이 push된다.
2. GitHub Actions가 암호화된 EC2 주소, 사용자명, 호스트 키, 제한된 개인키를 불러온다.
3. `known_hosts`에 고정한 EC2 ED25519 호스트 키로 서버 신원을 확인한다.
4. 워크플로가 서비스별 고정 배포 명령을 SSH로 요청한다.
5. OpenSSH가 `/usr/local/sbin/studyforest-deploy` 실행을 강제한다.
6. 호스트 공용 `flock`으로 세 저장소의 동시 배포 충돌을 방지한다.
7. `origin/dev`에서 fast-forward가 가능한 경우에만 소스를 갱신한다.
8. Docker Compose가 요청된 서비스만 빌드하고 교체한다.

## GitHub Actions Secrets

세 저장소에 동일한 이름의 Secret을 설정한다.

- `EC2_HOST`: EC2 Public IP 또는 DNS
- `EC2_USER`: 제한된 SSH 계정인 `ubuntu`
- `EC2_KNOWN_HOSTS`: 고정된 EC2 ED25519 호스트 키
- `EC2_SSH_KEY`: Actions 전용 제한 개인키

Secret 값, `.env`, 인프라 PEM, DB 자격증명 및 Oracle Wallet은 저장소에 커밋하지 않는다.

## 남아 있는 위험과 보완책

제한 키는 키 유출 시 피해 범위를 크게 줄이지만, `dev`의 애플리케이션 코드를 변경할 수 있는 관리자는 악성 코드도 배포할 수 있다. 따라서 `dev` 브랜치 보호, PR 리뷰 의무화, 쓰기 권한 최소화, 2단계 인증 및 워크플로 변경 검토가 필요하다.

향후에는 GitHub OIDC로 최소 권한 AWS IAM Role을 임시로 위임받아 AWS Systems Manager Run Command를 호출할 수 있다. 이 구조는 GitHub에 장기 SSH 개인키를 보관하지 않는다는 장점이 있다.

## 폐기 및 복구

Actions 접근을 즉시 폐기하려면 `/home/ubuntu/.ssh/authorized_keys`에서 `studyforest-github-actions`로 끝나는 줄을 삭제한다. 인프라 PEM은 이 키와 독립되어 있으므로 복구 접속에 계속 사용할 수 있다. 키 교체 시 새 키 쌍을 생성하고, EC2 공개키와 `EC2_SSH_KEY`를 교체한 뒤 기존 키를 삭제한다.
