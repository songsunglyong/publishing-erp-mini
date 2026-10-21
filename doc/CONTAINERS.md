# Docker 구성 — 준비 완료, 실행 미검증

2026-10-20: 사용자의 기존 Docker 배포·호스팅 보류 지시에 따라 파일만 준비했다. 이 환경에는 Docker CLI가 없으며 이미지 빌드, Compose 구문 검사와 기동, ARM64 이미지 실행은 수행하지 않았다. Phase 4의 Compose 기동 통과 조건은 남아 있다. 아래 명령은 후속 실행 절차이며 실행 완료 기록이 아니다.

## 구성

- Dockerfile: Java 21 JDK 빌드 → JRE 실행의 두 단계, 실행 WAR, UID/GID 10001, 힙 768MB. 이미지 빌드는 DB가 없어 테스트를 생략하므로 먼저 네이티브/CI의 `clean verify`를 통과해야 한다.
- compose.yaml: MariaDB 11.4, named volume `erp-db`, 내부 DB 연결, DB 포트 비공개, 앱만 호스트 `127.0.0.1:8080` 공개. DB healthcheck 성공 후 앱 생성. DB 메모리 512MB, 앱 1200MB 제한.
- container 프로필: Docker 내부에서는 0.0.0.0 바인딩이 필요하다. 호스트 공개는 Compose가 루프백으로 제한한다. 로컬 HTTP 시연이라 Secure=false이며 HttpOnly/SameSite/CSRF/세션 정책은 유지한다. `prod`와 혼용하면 시작을 거부한다. 운영 배포에는 별도 HTTPS 구성이 필요하다.
- .dockerignore: .local, .env*, .git, target 및 로그를 이미지 빌드 컨텍스트에서 제외한다.

공식 근거: [Docker Compose 시작 순서](https://docs.docker.com/compose/how-tos/startup-order/), [Boot 3.5 JSP 실행 WAR](https://docs.spring.io/spring-boot/3.5/reference/web/servlet.html#web.servlet.embedded-container.jsp-limitations).

## 후속 실행 절차

1. `.env.example`을 Git 제외 `.env`로 복사한다. `COMPOSE_DB_PASSWORD`, `COMPOSE_DB_ROOT_PASSWORD`, `COMPOSE_ADMIN_PASSWORD`에 서로 다른 임의 암호를 설정한다. 관리자 암호는 12자 이상이다. 이 파일은 앱의 네이티브 실행에서 자동 로드하지 않는다.
2. 네이티브 앱을 종료하거나 `COMPOSE_PORT=8081`을 `.env`에 설정한다. 포트 충돌만 피하며 네이티브 DB를 종료할 필요는 없다.
3. `docker compose config --quiet`로 필수 환경 변수·구문을 검증한다. 비밀이 출력되는 `docker compose config` 결과를 공유하지 않는다.
4. `docker compose up --build -d`, `docker compose ps` 실행 후 로그인·전체 시연을 검증한다. 앱 준비는 DB 준비와 별도이므로 로그와 로그인 화면을 확인한다. 초기 암호는 계정이 없는 DB에만 적용된다.
5. `docker compose down`은 DB volume을 유지한다. `down -v`는 데이터를 삭제하므로 일반 종료 절차에 사용하지 않는다.

Compose의 비밀 환경 변수는 로컬 시연 편의를 위한 것이다. Docker 접근 권한자는 컨테이너 환경을 읽을 수 있다. 실제 운영의 secret 저장소·TLS·백업·취약점 스캔·이미지 digest 고정은 별도 작업이다. 태그 기반 베이스 이미지가 변경될 수 있어 실행 검증 시 버전/digest를 기록한다.
