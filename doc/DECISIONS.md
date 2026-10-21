# 설계 결정

## D001 — 프로젝트 문서를 `doc/`에 모은다

- 날짜: 2026-10-17
- 상태: 채택
- 결정: 요청된 운영 문서 6종을 `doc/`에 두고 한국어로 관리한다.
- 이유: 프로젝트 배경, 작업 규칙과 진행 상태를 한곳에서 확인하기 위해서다.
- 영향: 구조·실행 방법·완료 상태 변경 시 관련 문서를 함께 수정한다.

## D002 — 루트 `AGENTS.md`는 작업 규칙 진입점으로 사용한다

- 날짜: 2026-10-17
- 상태: 채택
- 결정: 상세 규칙은 `doc/AGENTS.md`에 두고 루트 파일에서 읽도록 안내한다.
- 이유: `doc/` 외부 소스 파일 작업에서도 저장소 전체 규칙을 발견할 수 있게 한다.
- 영향: 규칙 본문을 중복 관리하지 않는다.

## D003 — 사용자 PRD의 지정 스택을 따른다

- 날짜: 2026-10-17
- 상태: 채택
- 결정: 문서 작성 중 추가된 PRD v1.0에 따라 Java 21, Spring Boot 3.5.x, Maven Wrapper, JSP/JSTL, jQuery, MariaDB, MyBatis XML을 사용한다. React·Thymeleaf·JPA로 임의 대체하지 않는다.
- 이유: Java/Spring 전환 학습과 출판 ERP 포트폴리오라는 명세의 목적을 충족한다.
- 영향: 네이티브 로컬 실행을 우선하고 정확한 의존성 호환 버전은 구현 전에 검증해 추가 기록한다. Flyway는 호환성 검증 후 채택한다.

## D004 — 비공개 GitHub 저장소와 `main` 브랜치를 사용한다

- 날짜: 2026-10-17
- 상태: 채택
- 결정: 사용자 요청에 따라 GitHub 저장소를 비공개로 생성하고 기본 브랜치는 `main`으로 둔다. 저장소 이름은 작업 폴더명 `publishing-erp-mini`를 사용한다.
- 이유: 초기 프로젝트를 버전 관리하고 비공개로 보관하기 위해서다.
- 영향: `.env`와 인증정보는 커밋에서 제외한다. 실제 연결 결과는 `CHANGELOG.md`에 기록한다.

## D005 — PRD와 현재 요청의 문서·공개 정책을 정렬한다

- 날짜: 2026-10-17
- 상태: 채택
- 결정: PRD의 `docs/` 예시는 `doc/`로 통일하고 GitHub 저장소는 현재 사용자 지시에 따라 비공개로 유지한다.
- 이유: 사용자가 직접 지정한 문서 경로와 비공개 생성 요청이 PRD 예시·공개 포트폴리오 목표보다 우선한다.
- 영향: 공개 전환은 후속 사용자 지시가 있을 때 진행한다. PRD 원본은 수정하지 않는다.

## D006 — 호환성을 확인한 Boot 3.5 / MyBatis 3.0과 실행 WAR

- 날짜: 2026-10-17
- 상태: 채택 및 실제 실행 검증
- 결정: Java 21, Spring Boot 3.5.16, MyBatis Starter 3.0.5, Maven 3.9.11 / Wrapper 3.3.4, Tomcat 10.1.55, MariaDB JDBC 3.5.8, Flyway 11.7.2, JSTL 3.0.1을 사용한다. Boot BOM이 관리하는 버전을 따른다. MyBatis core 실제 버전은 3.5.19다.
- 이유: [MyBatis 공식 호환표](https://mybatis.org/spring-boot-starter/mybatis-spring-boot-autoconfigure/)의 3.0 계열은 Boot 3.2–3.5와 Java 17 이상을 지원한다. [Boot 공식 JSP 제한](https://docs.spring.io/spring-boot/3.5/reference/web/servlet.html#web.servlet.embedded-container.jsp-limitations)에 따라 실행 JAR 대신 Tomcat 기반 실행 WAR를 사용한다.
- 검증: Java 21 aarch64에서 빌드, 실제 MariaDB 11.4.13 마이그레이션, JSP HTTP 렌더와 WAR 시작 성공. 정확한 버전은 Maven Central 메타데이터와 dependency:list로 확인했다.

## D007 — 프로젝트 전용 네이티브 DB와 작은 메모리 한도

- 날짜: 2026-10-17
- 상태: 채택
- 결정: `.local/data`의 MariaDB를 127.0.0.1:3307에 실행하고 업무/테스트 스키마·계정을 분리한다. JVM 힙은 Maven 384MB, 앱 768MB, 테스트 512MB, Hikari 풀은 앱 5/테스트 2개다.
- 이유: RAM 8GB에서 Docker 없이 반복 검증하고 기존 DB 서비스·스키마와 격리한다.
- 영향: 생성 암호·DB 파일·로그는 Git에서 제외한다. Java 경로는 스크립트에서 설정하므로 전역 시스템 링크가 필요 없다. 각 프로세스의 전체 메모리는 힙 외 메모리도 포함한다.

## D008 — 마이그레이션과 선택적 합성 시드

- 날짜: 2026-10-17
- 상태: 채택
- 결정: Flyway V1은 업무 테이블, demo/test 전용 V1.1은 합성 데이터를 생성한다. 신규 도서 재고 0행은 서비스에서 같은 트랜잭션에 추가한다. 후속 마이그레이션은 V2 이상으로 관리한다.
- 이유: 빈 DB 설치를 재현하고 local 모드에 예시 데이터를 강제로 넣지 않는다. DB 트랜잭션으로 도서·재고의 일관성을 보장한다.
- 영향: demo 적용 DB에서는 계속 demo 프로필을 사용해 이력을 유지한다. 금액은 Long/BIGINT 정수 KRW, ISBN/코드는 UNIQUE, 재고는 FK와 음수 방지 CHECK로 보호한다.

## D009 — Phase 1 폼 보호와 후속 보안 통합

- 날짜: 2026-10-17
- 상태: 채택
- 결정: Phase 1에서도 상태 변경은 POST와 세션 CSRF 토큰으로 보호하고 폼 허용 필드를 제한한다. 출력은 HTML escaping을 적용한다. 로그인·역할·Spring Security는 Phase 3에 통합한다.
- 이유: 실제 동작하는 로컬 관리 화면의 기본 입력·요청 보호를 마련하면서 현재 범위를 유지한다.
- 영향: 현재는 사용자 인증·접근 권한 분리가 없으며 루프백에서 합성 데이터로만 사용한다. Phase 2 주문 확정 시 재고를 예약하지 않고 출고 시 검사·차감한다.

## D010 — 개발자의 이해 확인을 다음 기능의 시작 조건으로 둔다

- 날짜: 2026-10-17
- 상태: 채택, 사용자 필수 규칙
- 결정: 작은 기능 하나씩 구현·설명하고 PHP 비교, 코드 작성 이유, 파일별 역할과 요청 흐름, 실행 방법, 수정 과제 1개, 이해도 질문 3개를 제공한다. 명시적인 이해 확인 전에는 다음 기능·단계로 진행하지 않는다.
- 이유: 개발자가 PHP 경험을 바탕으로 Java/Spring/JSP를 배우고 면접에서 코드·SQL·트랜잭션·오류 처리를 직접 설명하고 수정할 수 있어야 한다.
- 영향: 기존 Phase 0·1 구현 완료는 학습 완료를 의미하지 않는다. 기존 기능부터 작은 단위로 설명하며 과제는 대신 해결하지 않는다. 새 추상화는 필요한 이유를 설명할 수 있을 때만 도입한다. 학습 상태는 `LEARNING.md`에 기록한다.

## D011 — local/demo JSP 변경 검사를 명시한다

- 날짜: 2026-10-18
- 상태: 채택 및 실제 변경 반영 검증
- 결정: local/demo에서 JSP development=true, modificationTestInterval=0, recompileOnFail=true, Tomcat resource.allow-caching=false를 명시한다.
- 이유: 소스 기반 실행만으로는 Boot 기본 JSP 설정상 저장 후 변경 반영을 보장할 수 없었다. 짧은 리소스 캐시 지연도 학습 중 혼란을 주었다.
- 검증: 임시 JSP A→B 수정이 같은 앱 프로세스의 다음 HTTP 요청에서 반영됐다. 사용자 코드에 검증 마커를 삽입하지 않았다.
- 영향: 개발·시연 프로필에서만 사용한다. 실제 운영에는 별도 프로필과 캐시 정책이 필요하며, WAR 외부 파일 실행 기능을 추가한 것은 아니다.

## D012 — 표준 코드와 안전한 한국어 사용자 오류 안내

- 날짜: 2026-10-20
- 상태: 사용자 요청에 따라 구현·검증
- 결정: ErrorCode + messages_ko.properties → ErrorNotice(code/message/traceId)를 공통 계약으로 사용한다. MVC 예외와 /error를 안전한 공통 화면/JSON으로 처리한다. 폼은 BindingResult와 model을 같은 요청에 유지한다. 기존 입력/중복 오류의 폼 HTTP 200 계약은 유지하며 장애는 500/503, 인증/권한은 401/403이다.
- 이유: 기술적 원인을 사용자 문구로 노출하지 않으면서 입력 수정과 안전한 재시도·문의가 가능해야 한다. 알 수 없는 DB 제약 위반은 업무 규칙으로 추정하지 않는다. MyBatis로 감싸진 연결 장애는 예외 종류/SQLState로 판별한다.
- 로그: 서버 생성 UUID와 코드·예외 클래스·정적 발생 위치만 기록한다. 예외 메시지/Throwable 및 요청값은 기록하지 않는다. 디버그 SQL 로그를 끈다.
- 영향: 로그인/권한은 아직 없으므로 공통 코드·메시지·MockMvc 계약만 준비한다. 세션 만료 안내는 현재 비인증 화면에 맞게 다시 화면을 열도록 안내한다. 실제 보안 연결은 Phase 3에서 진행한다. 문의처를 만들어 내지 않고 운영 담당자에게 추적 ID를 전달하도록 안내한다.
- 검증: MockMvc 21개와 실제 JSP/DB·ISBN 검증을 포함한 전체 51개, WAR 빌드 성공. 실제 개발 앱 HTML/JSON 404에서도 동일 코드/추적 ID를 확인했다.

## D013 — Spring Security 인증·세션·직원 객체 권한

- 날짜: 2026-10-20
- 상태: 사용자 추가 요청으로 구현·검증. Phase 3 중 보안만 먼저 진행하며 주문/보고서는 포함하지 않음.
- 결정: Boot 3.5.16이 관리하는 Security 6.5.11, MyBatis UserDetailsService, DaoAuthenticationProvider, DelegatingPasswordEncoder, 서버 세션을 사용한다. 기존 자체 CSRF 인터셉터는 정식 Security CSRF로 교체했다.
- 권한: 공통 기준정보는 세 역할 조회, ADMIN/MANAGER 변경. 계정 관리는 ADMIN. 직원 프로필은 본인 또는 ADMIN만 조회하며 Service 객체 권한을 검사한다.
- 세션: 30분 미사용·8시간 절대 만료, ID 교체, 최대 1개, 동시 로그인 성공 직렬화 및 이전 실제 세션 invalidate. DB auth_version과 enabled/role 재검사로 폐기 통지 누락도 차단한다.
- 비밀값: 공유 초기 암호 없이 600 권한 Git 제외 파일에서 명시적 초기 관리자 생성. 로그에는 원문 비밀번호·세션 ID를 남기지 않는다. 운영 Secure/HTTPS와 loopback HTTP 개발 예외를 구분한다.
- 한계: 단일 JVM 레지스트리/세션 핸들이다. 다중 서버는 공동 세션·분산 폐기를 별도 설계한다. 이미 진행 중인 업무 트랜잭션을 강제 중단하지 않는다. 실제 TLS/프록시 배포는 하지 않았다.
- 검증: 전체 74개·실행 WAR 통과, 실제 개발 로그인/로그아웃 검증, actionlint 통과. 원격 Actions·Trivy는 미실행이다. 변경 파일과 상세 정책은 SECURITY.md를 따른다.

## 2026-10-20 — 주문·재고·반품·집계와 Phase 4 경계

- 최신 Phase 4까지 진행 요청은 이번 연속 구현 승인으로 해석한다. 학습 설명·과제/복습을 문서에 남기되 학습 이해 완료로 표시하지 않는다. 기존 Docker 배포·호스팅 보류는 유지한다.
- PRD의 STAFF는 최신 요구의 MANAGER로 대응한다. USER는 공동 업무 조회, ADMIN/MANAGER는 주문·출고·반품·입고, ADMIN만 조정한다. HTTP와 Service 둘 다 검사한다.
- 확정은 예약하지 않고 출고 때만 재고를 검사·차감한다. 주문 FOR UPDATE → 도서 ID 순 재고 잠금, 상태·잔액·원장을 하나의 트랜잭션으로 저장한다. StockLedger MANDATORY로 별도 커밋을 금지한다. 중복 상태 검사와 DB UNIQUE 처리 키를 함께 사용한다.
- 출고 완료는 직접 취소할 수 없고 품목별 부분/전체 반품을 사용한다. 주문 잠금 후 품목 FOR UPDATE로 최신 미반품 수량을 읽고 타 주문 품목을 거부한다. 반품 기록/수량/재고/원장은 함께 커밋한다.
- 주문 단가·명칭은 스냅샷이다. 총액은 서버 정수 연산·한도 검사로 계산한다. 등록/입고/반품 요청 UUID는 중복 방지에 사용하며 인증/CSRF를 대체하지 않는다.
- V3는 기존 데이터 삭제 없이 확장한다. 기존 비영 재고를 OPENING 조정 원장으로 이관해 잔액과 이력 합계를 맞춘다. 이 이관만 actor NULL을 허용한다.
- 매출은 출고일에 더하고 반품일에 차감하는 기간 흐름 지표다. 한국 시간·종료일 포함, 금액 합계 BigDecimal, 순매출 금액 순위 상위 20개. 현재 부족 재고에는 날짜 필터를 적용하지 않는다. 결제/세금/회계 지표는 범위 밖이다.
- Phase 4의 현재 소스 사본 검증과 원격 Git clone·Compose 기동 검증을 구분한다. 비공개 저장소는 공개로 바꾸지 않는다. 실행하지 않은 Docker/Trivy/원격 CI는 통과로 기록하지 않는다.
- Docker는 Java21 실행 WAR/non-root, DB 포트 비공개, 루프백 앱 공개·DB healthcheck·named volume·환경 필수 암호 구성으로 준비한다. container는 로컬 HTTP 시연 전용이며 prod 혼용을 거부한다. 실제 운영 구성으로 제시하지 않는다.

## 2026-10-21 — 사용자 승인에 따른 Boot 4 전환

- 기존 3.5 기준보다 최신 사용자 요청 `boot4 로 해줘`를 우선한다. Boot 4.1.1, MyBatis Starter 4.1.0, Security 7.1.1, Framework 7.0.9, Java 21을 기준으로 검증한다. MyBatis 공식 4.0+ 호환 표와 Maven Central 배포를 확인했다.
- JSP는 공식 지원되는 실행 WAR를 유지한다. React/Thymeleaf/JPA로 바꾸지 않는다. 기존 SQL/마이그레이션/재고·반품 규칙과 데이터는 보존한다.
- Boot 4의 webmvc/security-test/flyway starter와 새 테스트/오류 API 패키지를 사용한다. 애플리케이션의 JSON 처리는 Jackson 3으로 전환한다. Jackson 2 호환 모드를 임시 도입하지 않는다. 제3자 의존성이 사용하는 Jackson 2 BOM은 2.21.7로 패치한다.
- Boot BOM의 Tomcat 11.0.24 대신 같은 Servlet 6.1 계열의 정식 보안 패치 11.0.27을 사용한다. 기존 Tomcat 10 override를 그대로 가져오지 않는다.
- 오류 비노출은 spring.web.error, UTF-8 강제는 spring.servlet.encoding으로 이관하고 실제 바인딩을 자동 테스트로 검증한다. 오류 전용 MVC slice는 Boot 4의 ServletWebSecurityAutoConfiguration을 제외하여 기존 테스트 경계를 유지하며 실제 보안 검증은 별도 테스트에 그대로 둔다.
- 이전 CI 실패 이력과 새 전환 검증을 분리한다. 무시 목록으로 CVE를 숨기지 않는다. 서버 계정 가입/배포는 보류한다.
