# Phase 0 · Phase 1 검증 결과

- 검증일: 2026-10-17 (Asia/Seoul)
- 환경: macOS ARM64, 메모리 8,589,934,592바이트(8GB), OpenJDK 21.0.12.1 aarch64, MariaDB 11.4.13.
- 실행 명령: `./scripts/db-start.sh`, `./scripts/verify.sh` → `./mvnw clean verify`.
- 최종 결과: **29 tests, 0 failures, 0 errors, 0 skipped, BUILD SUCCESS**.
- 최종 빌드 시간: 8.698초(이미 의존성을 내려받은 환경). 전체 성능 보장은 아니다.

## Phase 0

변경 파일: `pom.xml`, `mvnw`, `mvnw.cmd`, `.mvn/*`, `.env.example`, `.gitignore`, `scripts/*.sh`, `ErpApplication.java`, `common/HomeController.java`, `application*.yml`, `home.jsp`, `BootstrapTest.java`.

- Java 21 ARM64 및 실제 8GB 메모리 확인.
- 실제 MariaDB `SELECT VERSION()`, `/health` UP 확인.
- RANDOM_PORT Tomcat의 실제 HTTP 요청으로 JSP 컴파일·한국어 HTML 확인. 로컬 jQuery 정적 자산도 200.
- Phase 0 최초 검증: `BootstrapTest` **2개 통과**.
- 실행 WAR를 `java -Xmx768m -jar ...war`로 시작하고 localhost:8080 홈 및 상태 확인 성공.
- 남은 작업이던 Phase 1 도메인·마이그레이션은 아래 단계에서 완료했다.

## Phase 1

변경 파일: `book/*.java`, `partner/*.java`, `common/`의 페이지·CSRF·검증·오류 클래스, `mapper/*Mapper.xml`, `db/migration/V1__master_data.sql`, `db/demo/V1_1__synthetic_master_data.sql`, `messages.properties`, 도서·거래처 JSP와 공통 JSPF, `static/css/app.css`, `static/js/app.js`, `MasterDataTest.java`, `Isbn13ValidatorTest.java`, 실행·설계 문서.

| 테스트 클래스 | 개수 | 검증 내용 |
| --- | ---: | --- |
| BootstrapTest | 2 | 실제 MariaDB 연결·상태 확인, HTTP JSP·jQuery 렌더 |
| Isbn13ValidatorTest | 10 | 선택 ISBN, 978/979 접두어, 체크섬, 잘못된 길이·문자 |
| MasterDataTest | 17 | 아래 실제 DB·HTTP 시나리오 |

실제 DB 검증: 빈 테스트 DB에 Flyway V1·V1.1 자동 적용, 합성 시드 조회, 기존 도서가 있는 상태에서 시드 적용, 도서·재고 원자적 생성, 재고 INSERT 강제 실패 시 전체 롤백, ISBN/코드 유일성, NULL ISBN 다건 허용, DB FK·음수 정가/재고 제약, 거래처 유형 수정, 비활성화 후 행 보존, 제목/ISBN/거래처/담당자 검색, 상태 필터, 10건 페이지 경계, 리터럴 와일드카드와 SQL 주입 문자열.

실제 HTTP 검증: 도서·거래처 등록→목록 조회→수정→비활성화, 한국어 오류, 중복 생성·수정 시 기존 값 보존, 잘못된 날짜·정수·ISBN·이메일·유형, ISBN 체크섬, DB 지원 날짜 범위, 세션 간 CSRF 차단, GET 변경 차단, ID 과도 바인딩 방지, XSS 출력 이스케이프, 400/403/404/405 응답.

## 발견 후 수정한 문제

- JSP 포함 파일의 contentType만으로는 부모 파일의 소스 인코딩이 보장되지 않아 한글이 깨졌다. 각 JSP/JSPF에 `pageEncoding=UTF-8`을 명시하고 실제 HTTP 응답에서 한국어를 검증한다.
- demo 시드가 기존 도서의 재고 행을 중복 생성할 수 있었다. 누락된 재고 행만 추가하도록 수정하고 기존 데이터 이후 시드 적용 회귀 테스트를 추가했다.
- MariaDB DATE의 저장 범위를 서버에서 검증해 입력이 DB 오류로 이어지지 않도록 했다.

## 브라우저·패키지 확인

최종 실행 WAR의 demo 프로필로 도서·거래처 각 3개가 표시되는지 확인했다. 실제 브라우저에서 도서 목록·등록 화면과 거래처 목록의 한글, 정가, 상태, 폼·테이블 배치를 확인했다. 기능 흐름은 위 실제 HTTP 테스트로 검증했다.

세부 JUnit XML은 `target/surefire-reports/`, 이번 실행 로그는 `.local/phase0-verify.log`, `.local/phase1-final-verify.log`, `.local/app.log`에 있다. 이 파일들은 로컬 산출물이며 커밋하지 않는다.

## 남은 작업

Phase 2 주문·출고·반품·재고 원장 및 동시성 테스트, Phase 3 로그인·권한·보고서, Phase 4 포트폴리오·배포 구성. Docker 배포·호스팅·공개 서비스 운영 검증은 수행하지 않았다. 외부 DB 운영·백업 복구, 다중 사용자 권한, 컨테이너 JSP 시작은 이번 범위가 아니다.
