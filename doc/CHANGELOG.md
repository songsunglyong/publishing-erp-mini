# 변경 기록

## 2026-10-17

### 완료

- 빈 작업 폴더에서 프로젝트 초기 상태를 확인했다. 사전에 읽을 기존 `AGENTS.md`, `PROJECT.md`, `TODO.md`, `DECISIONS.md`는 없었다.
- `doc/`에 `PROJECT.md`, `AGENTS.md`, `TODO.md`, `CHANGELOG.md`, `DECISIONS.md`, `README.md`를 작성했다.
- 저장소 전체에서 작업 규칙을 찾을 수 있도록 루트 `AGENTS.md`에 `doc/` 문서 진입점을 추가했다.
- 기본 브랜치를 `main`으로 로컬 Git 저장소를 초기화했다.
- `.gitignore`에 로컬 환경 파일, 비밀정보 파일, 로그 제외 규칙을 추가했다.

### 확인 사항

- 애플리케이션과 실행·테스트 설정은 아직 없어 코드 실행 테스트 대상이 없다.
- GitHub CLI 인증을 네트워크 접근 환경에서 확인했으며 `songsunglyong` 계정 인증이 정상이다.
- `songsunglyong/publishing-erp-mini` 비공개 저장소를 생성하고 `origin`으로 연결했다. 최초 문서 커밋 `e1cf0e8`을 푸시하고 `main`의 `origin/main` 추적을 설정했다.
- GitHub API로 `PRIVATE` 공개 설정과 기본 브랜치 `main`을 확인했다.
- 작업 중 추가된 사용자 PRD v1.0을 읽고 목적·지정 기술 스택·단계별 할 일·문서 경로 정책을 반영했다. PRD 원본은 수정하거나 이번 문서 커밋에 포함하지 않았다.
- 필수 문서와 내부 링크 존재 여부 및 Git 공백 오류 검사를 수행했다.

### Phase 0 — 실행 기반 구현 및 검증

- Java 21.0.12.1 aarch64, RAM 8GB, MariaDB 11.4.13을 확인했다. Spring Boot 3.5.16, MyBatis Starter 3.0.5, Maven 3.9.11 / Wrapper 3.3.4를 사용한다.
- 실행 WAR, JSP/JSTL, 로컬 jQuery/Bootstrap, 프로필(local/test/demo), Hikari 풀 5개, JVM 메모리 한도 및 전용 네이티브 DB 스크립트를 추가했다.
- `./scripts/verify.sh` (`./mvnw clean verify`): 테스트 2개, 실패·오류·건너뜀 0, BUILD SUCCESS. 실제 MariaDB 연결과 HTTP JSP 컴파일을 확인했다.
- `java -Xmx768m -jar target/publishing-erp-mini-0.1.0-SNAPSHOT.war`: localhost:8080 홈 HTML 및 `/health`의 MariaDB UP 응답 확인.
- 의존성 부모 POM의 스냅샷 저장소 지연을 피하기 위해 Maven Central 전용 미러를 설정했다.
- 다음 작업: Phase 1 도서·거래처와 마이그레이션·테스트. Docker 및 호스팅은 진행하지 않았다.
