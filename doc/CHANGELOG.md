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
- 작업 중 추가된 사용자 PRD v1.0을 읽고 목적·지정 기술 스택·단계별 할 일·문서 경로 정책을 반영했다. PRD 원본은 당시 문서 커밋에 포함하지 않았고 이후 Phase 0 개발 커밋에서 원본 그대로 추적했다.
- 필수 문서와 내부 링크 존재 여부 및 Git 공백 오류 검사를 수행했다.

### Phase 0 — 실행 기반 구현 및 검증

- Java 21.0.12.1 aarch64, RAM 8GB, MariaDB 11.4.13을 확인했다. Spring Boot 3.5.16, MyBatis Starter 3.0.5, Maven 3.9.11 / Wrapper 3.3.4를 사용한다.
- 실행 WAR, JSP/JSTL, 로컬 jQuery/Bootstrap, 프로필(local/test/demo), Hikari 풀 5개, JVM 메모리 한도 및 전용 네이티브 DB 스크립트를 추가했다.
- `./scripts/verify.sh` (`./mvnw clean verify`): 테스트 2개, 실패·오류·건너뜀 0, BUILD SUCCESS. 실제 MariaDB 연결과 HTTP JSP 컴파일을 확인했다.
- `java -Xmx768m -jar target/publishing-erp-mini-0.1.0-SNAPSHOT.war`: localhost:8080 홈 HTML 및 `/health`의 MariaDB UP 응답 확인.
- 의존성 부모 POM의 스냅샷 저장소 지연을 피하기 위해 Maven Central 전용 미러를 설정했다.
- 다음 작업: Phase 1 도서·거래처와 마이그레이션·테스트. Docker 및 호스팅은 진행하지 않았다.

### Phase 1 — 도서·거래처 구현 및 검증

- Controller/Service/Form/Mapper/XML 구조로 도서·거래처 등록·조회·수정·비활성화와 한국어 JSP 화면을 구현했다.
- 제목·ISBN·거래처명·코드·담당자 검색, 상태 필터, 최신순 10건 페이지네이션을 추가했다.
- Flyway V1 업무 스키마와 demo/test 전용 V1.1 합성 시드, 도서 등록 시 재고 0행 생성 트랜잭션을 추가했다.
- ISBN-13 체크섬·선택 값 정규화·고유 키 오류, 정수 금액·날짜 범위·이메일·유형 검증, CSRF 토큰·허용 필드·HTML escaping을 적용했다.
- JSP UTF-8 포함 파일 처리와 기존 도서 이후 demo 시드 적용의 재고 중복 문제를 테스트로 발견·보완했다.
- 빈 전용 테스트 DB에서 `./scripts/verify.sh` (`./mvnw clean verify`) 최종 실행: **29개 테스트, 실패 0, 오류 0, 건너뜀 0, BUILD SUCCESS**, 8.698초.
- 도서·재고 원자성, 고유 키·FK·CHECK, CRUD, 검색·페이지, 한국어 오류, 중복 수정 보존, CSRF·XSS 및 HTTP 오류 응답을 실제 MariaDB/Tomcat에서 검증했다.
- 실행 WAR와 브라우저에서 도서·거래처 한글 화면, demo 데이터와 DB 상태를 확인했다. 테스트 전용 H2·Docker를 사용하지 않았다.
- 루트/상세 실행 README, 구조·ERD·설계 결정·단계별 검증 결과와 TODO를 갱신했다.
- Phase 0·1만 구현했다. 주문·출고·반품·보고서·로그인·권한은 남아 있으며 Docker 배포·호스팅은 진행하지 않았다. 구현은 로컬 Git 커밋으로 기록하고 이번 작업에서는 원격 푸시하지 않는다.
