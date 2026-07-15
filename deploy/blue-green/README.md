# StudyForest 단일 EC2 Blue-Green 준비안

이 디렉터리는 운영 서버에 바로 덮어쓰기 위한 완성본이 아니라, 현재 EC2 설정을 확인한 뒤 병합할 기준안이다. `.env`, 인증서, Oracle Wallet은 복사하거나 커밋하지 않는다.

## 사전 확인

1. EC2 메모리가 Blue와 Green 전체 스택을 동시에 실행할 수 있는지 확인한다.
2. `/home/ubuntu/StudyForest/compose.yaml`, `.env`의 **키 이름만**, 현재 Nginx 설정을 백업한다.
3. 현재 컨테이너의 CPU/메모리 제한, 볼륨, Oracle Wallet mount를 새 Compose에 병합한다.
4. 8001, 8002, 8081, 8082, 5173, 5174는 모두 `127.0.0.1`에만 bind한다.

## 설치 순서

```bash
install -m 644 compose.blue-green.yaml /home/ubuntu/StudyForest/compose.blue-green.yaml
sudo install -m 755 studyforest-deploy /usr/local/sbin/studyforest-deploy
sudo install -d -m 755 /etc/nginx/studyforest
sudo install -m 644 nginx/upstreams-blue.conf /etc/nginx/studyforest/
sudo install -m 644 nginx/upstreams-green.conf /etc/nginx/studyforest/
sudo ln -sfn /etc/nginx/studyforest/upstreams-blue.conf /etc/nginx/studyforest/active-upstreams.conf
sudo install -m 644 nginx/studyforest.conf /etc/nginx/sites-available/studyforest
```

Nginx 설정은 현재 운영 도메인 `studyforest.site`와 Certbot 인증서 경로를 기준으로 작성했다. 인증서 경로가 달라졌다면 운영값에 맞게 수정하며, `nginx -t`를 통과하기 전에는 reload하지 않는다.

Compose 검증:

```bash
docker compose -f compose.blue-green.yaml config --quiet
docker compose -f compose.blue-green.yaml build ai-blue server-blue client-blue
docker compose -f compose.blue-green.yaml up -d ai-blue server-blue client-blue
curl -fsS http://127.0.0.1:8001/health
curl -fsS http://127.0.0.1:8081/api/v1/health
curl -fsS http://127.0.0.1:5173/
```

Blue가 정상인 것을 확인한 뒤에만 Nginx를 Blue로 연결한다. 그 다음 Green 배포와 전환을 수동 검증하고, 마지막 단계에서 `/usr/local/sbin/studyforest-deploy`를 교체한다.

## 아직 병합해야 하는 운영 정보

- 현재 도메인과 TLS 인증서 설정
- 업로드/로그 영속 볼륨
- EC2 메모리에 맞춘 JVM 및 컨테이너 메모리 제한
- 기존 CORS 허용 도메인

## 2026-07-15 운영 점검 결과

- EC2 메모리: 1.9GiB, 가용 약 477MiB, Swap 없음
- 현재 컨테이너 메모리: Spring 약 560MiB, AI 약 121MiB, Client 약 156MiB
- 이 사양에서 전체 스택 두 벌을 동시에 실행하면 메모리가 부족하므로 실제 전환은 보류한다.
- 실제 적용 전 4GiB 이상 인스턴스로 확장하는 것을 권장한다. Swap만 추가해 강행하는 방식은 장애 시 성능 저하와 OOM 위험 때문에 운영 권장안으로 보지 않는다.
- 현재 Compose의 `.env` 경로, Oracle Wallet mount, timezone, Spring cloud profile, Vite proxy 설정은 초안에 반영했다.

컨테이너를 변경하지 않는 산출물 검증은 배포기에서 `plan` 명령으로 수행한다. 이 명령은 활성/대상 슬롯을 출력하고 Compose 문법만 검사한다. 실제 Health Check는 `deploy stack` 실행 때만 수행하며, 응답하지 않으면 Nginx를 전환하지 않고 기존 슬롯을 유지한다.

## 알려진 후속 개선

- Spring `/api/v1/health`를 DB와 Redis까지 확인하는 readiness endpoint로 분리
- 클라이언트 Dockerfile을 Vite 개발 서버가 아닌 정적 production build로 변경
- WebSocket 자동 재연결 검증 후 draining 시간 확정
- DB 변경은 구버전과 신버전이 동시에 실행 가능한 expand/contract 방식 사용
