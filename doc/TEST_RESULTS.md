# 검증 결과

## 2026-10-21 보안 패치 후 검증

Tomcat 10.1.61/Jackson BOM 2.21.7 적용 후 전체 91개(실패/오류/건너뜀 0), WAR 빌드 성공(25.877초). 패키지 내 버전 확인과 java -jar WAR의 실제 로그인/JSP 5개 HTTP 200을 확인했다. Spring webmvc의 2개 보안 항목은 여전히 검토가 필요하며 최종 CI 상태를 RELEASE.md에 기록한다. `91개 통과`는 모든 의존성 취약점 해결을 의미하지 않는다.


## GitHub 릴리스 검증 — 2026-10-21

로컬 재검증 91개/WAR 성공(33.431초). GitHub 5310b72를 실제 새 폴더로 clone하고 빈 전용 테스트 DB에서 다시 91개/WAR 성공(25.053초), 임시 DB/계정 정리 완료. 원격 첫 실행도 91개/WAR 성공했으나 Trivy의 Java 분석 대상이 0개여서 유효한 취약점 검사 완료로 인정하지 않았다. CI를 1c8a8a2에서 보완했다. 최종 원격 결과는 RELEASE.md 참고. 아래 2026-10-20 결과는 당시 이력이며 원격 푸시/검증 미실행 표기는 이번 기록으로 대체한다.


## 최신 결과 — Phase 2~4 (2026-10-20)

현재 작업 트리의 `./scripts/verify.sh` → **91 tests, 0 failures, 0 errors, 0 skipped, BUILD SUCCESS**, 39.499초. 실행 WAR 생성 성공. 이전 날짜 결과는 아래에 이력으로 보존한다.

| 테스트 | 개수 | 검증 |
| --- | ---: | --- |
| BootstrapTest | 4 | JSP·DB·로컬 정적 리소스 |
| Isbn13ValidatorTest | 10 | ISBN 체크섬 |
| MasterDataTest | 18 | 기준정보·SQL·제약·JSP·회귀 |
| ProductionSecurityMvcTest | 1 | 운영 HTTPS·쿠키 정책 |
| SecureCookieHttpTest | 1 | 실제 세션 쿠키 속성 |
| SecurityMvcTest | 22 | 로그인·세션·권한·직원 객체·새 업무 경로·CSRF |
| TransactionTest | 16 | 주문·재고·반품·월 경계 집계·동시성·롤백·실제 JSP 전체 시나리오 |
| UserErrorMvcTest | 19 | 오류 코드·한국어·입력 보존·민감값 비노출 |

### 단계별 확인

- Phase 2: 확정 시 무예약, 부족 재고 전체 롤백, 중복 출고, 입고 중복, 부분/전체/초과/타 주문 품목 반품, 별도 DB 연결 2개를 사용한 출고·반품 경쟁 검증. 헤더/첫 품목 저장 후 두 번째 INSERT를 실제 DB trigger로 실패시켜 등록 원자성도 검사했다.
- Phase 3: 1월 출고 700원/2월 반품 300원 → 기간별 700/-300, 전체 400원. 주문 단가와 월 경계, 도서/거래처 대사, 접근 차단 검증.
- Phase 4: 현재 Git 대상 소스를 비밀·target 없이 `/private/tmp/erp-portfolio-4ttpsgva`에 복사하고 새 전용 DB에 4개 마이그레이션 적용 후 **91개/WAR 성공(33.957초)**. 실제 원격 clone과 구분한다. 현재 main 소스가 검증 사본과 일치함을 확인했고 임시 검증 DB·계정은 종료 후 정리했다.
- Chromium 실제 화면: 새 합성 도서 2종/거래처/각 20권 입고/3·2권 주문/출고/첫 품목 1권 반품. 출고 75,000원/반품 15,000원/순매출 60,000원. 원장·보고서 포함 화면 5개, JS 오류 0개. 주문 상세와 보고서 PNG를 직접 열어 표시를 확인했다.
- 패키징한 WAR를 8081에서 직접 시작, 실제 로그인 후 주문·주문 등록·재고·원장·보고서 JSP 5개 HTTP 200. 검증 후 해당 WAR 프로세스는 종료하고 8080 개발 앱을 유지했다.
- 유지보수: 임시 사본만 MANDATORY→REQUIRES_NEW 변경, 기대 재고 5/실제 3으로 회귀 테스트 실패. 복구 후 같은 테스트 1개 통과. [사례](maintenance-case.md).
- actionlint, shell 구문, MyBatis XML 및 YAML 파싱 통과. Compose의 루프백 공개/DB 비공개/healthcheck 의존성 검사, 현재 로컬 생성 암호가 Git 대상 파일에 포함되지 않음을 검사했다. 이는 Docker 검증이나 포괄적 secret 스캔을 대신하지 않는다.

### 검증 중 수정한 문제

실제 HTTP 시나리오의 Location이 전체 주소인데 테스트 도구가 서버 주소를 중복 연결해 실패했다. URI의 path를 추출하도록 테스트를 수정한 뒤 전체 91개를 통과했다. 반품은 잠금 대기 후 최신 누적 수량을 읽도록 품목 FOR UPDATE를 사용한다. V3 FK 추가 후 기존 시드 재실행 테스트는 해당 테스트 트랜잭션 안에서 원장을 먼저 정리하며 테스트 종료 시 롤백한다. YAML 보조 검사는 Python 패키지 부재로 실패해 기존 Java SnakeYAML로 변경하고 통과했다.

### 남은 검증·위험

Docker CLI가 없고 기존 사용자 보류 지시가 있어 Docker 빌드·Compose config/기동은 미실행이다. ARM64 이미지/컨테이너 내부 JSP·volume 재기동을 보장하지 않는다. 원격 소스 푸시/실제 fresh clone·Actions·Trivy도 미실행이다. **Phase 4의 엄격한 전체 gate는 남아 있다.** Flyway의 MariaDB 11.4 지원 범위 경고, 태그 기반 이미지, 백업 복원·운영 TLS 미검증 및 단일 JVM 세션 제약은 알려진 한계다.

실행 출력 요약은 [test-output.txt](test-output.txt), 변경 파일·학습 설명은 [PHASE2_4.md](PHASE2_4.md), 전체 경로 목록은 [CHANGESET_PHASE2_4.md](CHANGESET_PHASE2_4.md). 기존 사용자 JSP 수정 및 이전 오류/보안 작업을 보존했고 이번 작업에서 커밋·푸시하지 않았다. `git diff --check`는 기존 사용자 book/list.jsp 끝의 공백 1건을 보고한다. 해당 문구·공백은 임의 수정하지 않았다.

---

## 아래는 이전 검증 이력


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

## 2026-10-20 — 공통 오류 시스템 확장 최종 검증

- 최종 명령: ./scripts/db-start.sh && ./scripts/verify.sh. 로그: .local/error-full-verify.log (Git 제외).
- 전체 51개, 실패 0, 오류 0, 스킵 0. BootstrapTest 2, MasterDataTest 18, Isbn13ValidatorTest 10, UserErrorMvcTest 21. WAR 패키징 성공.
- 신규 MockMvc: 입력·형변환 오류와 원문 보존, 중복 ISBN/거래처 코드, 업무 규칙, 404/400/405, CSRF·만료 세션, 로그인/권한 메시지 계약, DB 장애 및 MyBatis 래핑, 알 수 없는 DB 제약, 500/503 폼 보존, JSON /health, /error fallback, 한국어 강제, 추적 ID와 민감 로그 비노출.
- 실제 MariaDB/JSP HTTP: 오류 폼의 입력 및 HTML 태그 escaping, 잘못된 검색 파라미터의 한국어 수정 안내·검색어 보존, 공통 404 안내와 추적 ID. 기존 CRUD/롤백/중복/CSRF 테스트도 유지.
- 재시작한 demo 개발 앱: /health 정상, HTML/JSON의 없는 도서 수정 요청 404와 동일 code/traceId 확인, 거래처 목록 200 및 사용자 링크 문구 유지.
- 초기 Mockito self-attach 정지와 신규 테스트의 Mock 재설정 오류를 수정 후 최종 전체 명령으로 재검증했다. 로그인 기능/실제 권한 검사는 아직 없으며 메시지 계약 테스트를 인증 구현 검증으로 해석하지 않는다.

## 2026-10-20 — 실제 로그인·세션·접근 통제 최종 검증

- 최종 전체 명령: ./scripts/db-start.sh && ./scripts/verify.sh. .local/security-full-verify.log에 기록. 전체 74개, 실패 0, 오류 0, 스킵 0, 실행 WAR 빌드 성공.
- SecurityMvcTest 21, ProductionSecurityMvcTest 1, SecureCookieHttpTest 1, BootstrapTest 4, MasterDataTest 18, UserErrorMvcTest 19, Isbn13ValidatorTest 10.
- 사용자 요구 10개 시나리오를 실제 MariaDB 계정과 Security 필터 체인의 MockMvc에서 검증했다. 비사용 30분과 절대 8시간은 테스트 Clock으로 경계 검증했다. 동시 로그인, Service/객체 권한, 인증 버전·비밀번호 변경·계정/IP 제한 검사도 포함한다.
- 실제 Tomcat/JSP HTTP에서 쿠키 Secure/HttpOnly/SameSite와 COOKIE 전달, 로그인 ID 교체/중복 세션/로그아웃, 한국어 로그인 실패와 비밀번호 비노출을 확인했다.
- 기존 자체 CSRF/만료 인터셉터의 오류 MockMvc 2개는 실제 SecurityMvcTest 검사로 이전했다. 기존 업무 회귀의 GET 변경 불가/CSRF/XSS/트랜잭션 검사는 로그인 후 유지했다.
- 초기 로그 검사에서 Mock 세션 숫자가 시간과 겹친 검사 오류, 중복 로그인 후 이전 브라우저를 계속 쓰던 테스트 전제, 홈에서 새 CSRF 토큰을 읽지 못한 실제 문제, HTTPS 테스트의 scheme 설정을 수정한 후 최종 명령으로 통과했다.
- 실제 개발 앱: 원본 DB 초기화 없이 V2와 최초 관리자 적용. 익명 ERP/health 차단, 관리자 로그인/세션 교체, 도서·관리자 화면 200, 로그아웃 후 차단 및 실제 비밀번호·세션 ID 로그 비노출 확인. .local/auth.env 600 및 Git 제외 확인.
- 공식 actionlint 1.7.12 체크섬 검증 후 .github/workflows/security.yml 구문 검사 통과. GitHub 원격 Actions·Trivy·실제 HTTPS 배포는 미실행이다.

## 2026-10-21 원격 최종 검사

커밋 605d6eb의 Actions 39539817704: 91개(실패/오류/건너뜀 0)/WAR 성공, Java 60개 범위 검사와 source secret 검사 단계 성공. 의존성 gate는 Spring CVE-2026-47884/47890 두 항목으로 실패. 앞선 Jackson/Tomcat 8개 항목은 패치 재검사에서 사라짐. 전체 CI 통과로 보고하지 않는다. 실제 배포는 없다.
