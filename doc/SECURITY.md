# 로그인·세션·접근 통제

## 구현 범위와 버전

2026-10-20 사용자 요청으로 Phase 3의 로그인/권한 부분을 먼저 구현했다. 주문·출고·보고서를 추가하거나 서버에 배포한 것은 아니다. 실제 버전은 Java 21, Spring Boot 3.5.16, Boot BOM이 관리하는 Spring Security 6.5.11, MyBatis Starter 3.0.5다. 별도 Security 버전을 강제로 지정하지 않는다.

현재 단일 JVM의 서버 세션 기반이다. Spring Security의 SecurityFilterChain, DaoAuthenticationProvider, UserDetailsService, DelegatingPasswordEncoder, SessionRegistry, HttpSessionEventPublisher, 메서드 보안을 사용한다. 인증 전용 코드/SQL은 security 패키지/UserMapper에 두고 도서·거래처 Service의 업무 로직과 분리했다.

공식 근거:

- [Spring Security 6.5 세션 관리](https://docs.spring.io/spring-security/reference/6.5/servlet/authentication/session-management.html)
- [DaoAuthenticationProvider](https://docs.spring.io/spring-security/reference/6.5/servlet/authentication/passwords/dao-authentication-provider.html)
- [CSRF와 JSP 폼](https://docs.spring.io/spring-security/reference/6.5/servlet/exploits/csrf.html)
- [Trivy Action 공식 사용법](https://github.com/aquasecurity/trivy-action)

## 권한 정책

기존 도서·거래처에는 직원 소유자 컬럼이 없으므로 공동 기준정보로 유지한다. 모든 데이터에 임의로 소유권을 부여하거나 기존 데이터를 삭제하지 않는다. 객체 권한을 검증할 실제 대상은 새 사용자 계정의 직원 프로필(`/employees/{id}`)로 정했다. 이 화면은 직원 계정의 공개 허용 필드(이름·아이디·권한)만 조회하며 비밀번호 해시는 조회 결과 DTO에 포함하지 않는다.

| 작업 | ADMIN | MANAGER | USER |
|---|---|---|---|
| 홈·DB 상태·도서/거래처 목록 | 허용 | 허용 | 허용 |
| 도서·거래처 등록·수정·비활성화 | 허용 | 허용 | 거부 |
| 계정 목록·등록·차단 | 허용 | 거부 | 거부 |
| 자신의 직원 프로필·비밀번호 변경 | 허용 | 허용 | 허용 |
| 다른 직원 프로필 조회 | 허용 | 거부 | 거부 |

로그인·오류 화면과 정적 파일만 익명 접근을 허용한다. 관리자 메뉴는 ADMIN에게만 표시한다. 조회 전용 사용자의 변경 버튼도 숨긴다. 숨김은 편의를 위한 것이며 실제 통제는 HTTP 규칙과 Service의 @PreAuthorize가 수행한다. 직원 정보 Service는 StaffAccess로 대상 id와 인증된 사용자 id를 비교한다. 계정 관리 Controller와 Service에 모두 ADMIN 검사를 둔다.

## 인증과 요청 흐름

1. GET /login에서 Spring Security CSRF 토큰이 들어 있는 JSP를 보여 준다. 아이디·비밀번호 원문을 모델이나 로그에 남기지 않는다.
2. POST /login은 CSRF 검사 → DB 기반 시도 제한 → UsernamePasswordAuthenticationFilter → DaoAuthenticationProvider 순서다.
3. MybatisUserDetailsService가 바인딩 SQL로 계정을 찾고 DelegatingPasswordEncoder로 검증한다. 저장 형식은 현재 {bcrypt} 접두사다. 존재하지 않는 계정·틀린 비밀번호·차단된 계정·시도 제한은 LOGIN_FAILED와 동일한 한국어 안내를 사용한다.
4. 성공 후 비밀번호를 지운 ErpPrincipal을 서버 세션에 저장한다. 인증 전 세션 ID는 changeSessionId로 교체한다. 클라이언트에는 HttpOnly 세션 쿠키만 전달한다.
5. 이후 요청은 세션 검증 → URL 권한 → Controller → Service 권한/객체 권한 → Mapper/DB → JSP다. SQL 값은 모두 #{}로 바인딩하며 사용자 값을 SQL 문자열로 이어 붙이지 않는다.

PHP 비교: password_verify와 session_regenerate_id, 세션의 로그인 사용자, 공통 접근 검사에 해당한다. 이 프로젝트는 Spring Security가 인증·컨텍스트 저장·CSRF를 담당하고, Service는 역할/객체 권한과 업무 트랜잭션을 담당한다.

## 세션 제한과 폐기

- 미사용 30분: 컨테이너 timeout=30m과 요청 필터의 lastSeen을 함께 검사한다. 활동한 경우에만 lastSeen을 갱신한다.
- 최대 8시간: 로그인 성공 시 loginAt을 고정한다. 요청 때마다 갱신하지 않으며 8시간 이상이면 활동 여부와 관계없이 폐기한다. 시간은 주입한 Clock으로 검사해 자동 테스트에서 실제로 기다리지 않는다.
- 계정당 최대 1개: SessionRegistry의 maximumSessions(1), maxSessionsPreventsLogin(false)를 사용한다. 성공 처리의 등록/이전 세션 폐기를 동기화하여 동시 로그인에도 마지막 성공 로그인을 남긴다.
- 기존 세션을 expired 표시만 하고 남겨 두지 않도록 LiveSessions의 서버 세션을 즉시 invalidate한다. 등록/폐기 이벤트로 참조를 정리한다. 로그인 성공 경쟁 중 세션이 다시 만들어진 경우에도 컨텍스트를 명시적으로 저장한다.
- 로그아웃은 CSRF가 있는 POST만 허용한다. 서버 세션을 무효화하고 인증 컨텍스트와 JSESSIONID 쿠키를 삭제한다. GET으로 로그아웃시키지 않는다.
- 비밀번호 변경은 현재 비밀번호를 확인하고, DB의 auth_version을 원자적으로 증가시킨다. 변경 트랜잭션 커밋 후 관련 세션을 모두 폐기한다. 현재 기기도 다시 로그인해야 한다.
- 계정 차단도 enabled=false와 auth_version 증가를 한 SQL로 처리한 뒤 세션을 폐기한다. 자기 관리자 계정을 차단하는 동작은 거부한다.
- 매 인증 요청에서 DB의 enabled/auth_version/role을 다시 확인한다. 세션 레지스트리 통지가 누락되어도 변경된 계정은 다음 보호 요청을 수행할 수 없다. DB 확인 자체가 실패하면 보호 작업을 실행하지 않고 503으로 닫는다.
- 계정 차단 전에 이미 실행 중이던 업무 트랜잭션을 강제로 중단시키는 기능은 없다.

## 쿠키와 프로필

| 설정 | 기본 / prod | local·demo·test |
|---|---|---|
| HttpOnly | true | true |
| Secure | true | HTTP 개발/테스트를 위해 false |
| SameSite | Lax | Lax |
| 세션 전달 | COOKIE만 | COOKIE만 |
| 세션 ID URL 재작성 | 비활성 | 비활성 |
| 미사용 제한 | 30분 | 30분 |
| 인증 절대 만료 | 8시간 | 8시간 |

prod는 HTTPS로 전환하며 local/demo와 함께 활성화하거나 Secure=false로 실행하면 시작을 거부한다. 프로덕션 설정 검사에서 HTTP→HTTPS, 실제 서블릿 검사에서 Secure/HttpOnly/SameSite 및 COOKIE만 전달하는 설정을 확인했다. 실제 인증서·리버스 프록시는 아직 배포하지 않았다. 프록시 헤더를 무조건 신뢰하는 설정도 추가하지 않았다.

## 로그인 실패 제한

- 계정 입력 문자열마다 15분 구간 최대 5회, IP마다 15분 구간 최대 30회의 로그인 시도를 허용한다. 비밀번호 검증 전에 DB에서 원자적으로 예약하여 병렬 요청의 우회를 줄인다.
- 계정 성공 시 계정 카운터만 지운다. IP 카운터는 성공해도 남긴다. 다른 아이디를 계속 보내는 우회도 제한한다.
- 버킷 키는 종류와 계정/IP를 해시한 값이다. 비밀번호·쿠키·세션 ID는 저장하지 않는다. IP 한도를 넘으면 새 계정 버킷을 만들지 않으며, 오래된 버킷은 요청 시 주기적으로 삭제한다.
- 신뢰하지 않는 X-Forwarded-For를 사용하지 않는다. 배포 시 신뢰할 프록시 목록과 실제 원격 IP 처리 정책을 정해야 한다. NAT 사용자가 많으면 IP 제한이 정상 로그인에도 영향을 줄 수 있다.
- 특정 아이디에 대한 반복 공격은 해당 아이디의 임시 이용 제한을 유발할 수 있다. 운영 앞단의 속도 제한·모니터링·추가 인증/CAPTCHA 정책은 아직 구현하지 않았다.

## 비밀번호·초기 관리자

- 원문 대신 DelegatingPasswordEncoder 해시만 DB에 저장한다. 인증 성공 후 세션 principal의 해시는 지운다. 비밀번호 정책은 12자 이상, UTF-8 72바이트 이내이며 로그인에도 바이트 제한을 적용한다.
- 저장소에 초기 비밀번호나 기본 공용 계정을 넣지 않는다. test의 합성 계정은 @TestConfiguration에서만 생성한다.
- 로컬 초기화:

```sh
./scripts/db-start.sh
./scripts/auth-init.sh
./scripts/run.sh -Dspring-boot.run.profiles=demo
```

- .local/auth.env는 권한 600, Git 제외다. 파일을 로컬 편집기에서 열어 최초 아이디/비밀번호를 확인한다. 비밀번호를 채팅·로그·명령 인자로 전달하지 않는다.
- DB에 계정이 전혀 없고 명시적으로 bootstrap을 활성화했을 때만 관리자 한 명을 생성한다. 기존 계정/비밀번호를 덮어쓰지 않는다.
- 첫 로그인 후 비밀번호를 변경하고 bootstrap 환경 변수/로컬 파일을 제거하거나 비활성화하는 것을 권장한다. 비밀번호 변경 후 auth.env의 최초 값이 자동 갱신되지는 않는다.
- 관리자 메뉴에서 합성 MANAGER/USER 계정을 등록할 수 있다. 공개 회원가입·비밀번호 찾기·차단 해제·관리자 비밀번호 재설정 UI는 이번 요청 범위에 포함하지 않았다.

## 오류·로그

기존 ErrorCode/한국어 메시지/추적 ID를 SecurityResponses에 연결했다. 필터 오류는 JSON 또는 안전한 JSP/로그인 안내로 응답한다. 비밀번호와 세션 ID를 요청·예외 메시지·principal 출력으로 로그에 남기지 않는다. Spring Security 로그는 WARN, JDBC/MyBatis 상세 SQL 로그는 OFF다. 사용자 문구는 USER_MESSAGES.md를 따른다. 비밀번호 입력은 실패해도 다시 출력하지 않는다. 계정 생성 오류에서는 아이디와 이름만 출력 허용 목록에 둔다.

## 자동 검증과 CI

최종 명령: ./scripts/db-start.sh && ./scripts/verify.sh. 74개 통과, 실패·오류·스킵 0, 실행 WAR 빌드 성공.

| 검사 | 결과 |
|---|---|
| SecurityMvcTest | 21개: 실제 DB 로그인/로그아웃, 실패 안내, ID 교체, 중복·동시 로그인, 30분/8시간, CSRF, 역할/Service/직원 객체, 차단/인증 버전/비밀번호 변경, 시도 제한 |
| ProductionSecurityMvcTest | 1개: HTTPS 전환 및 운영 세션 설정 |
| SecureCookieHttpTest | 1개: 실제 Tomcat의 Secure·HttpOnly·SameSite와 COOKIE 전달 |
| BootstrapTest | 4개: 실제 로그인 JSP, 실패 비노출, HTTP 세션 교체·중복 로그인·로그아웃, DB·홈 |
| MasterDataTest | 18개: 기존 업무 회귀 및 JSP/XSS·오류 화면을 로그인 상태로 검증 |
| UserErrorMvcTest | 19개: 공통 오류 계약. 예전 자체 인터셉터 2개 검사는 실제 SecurityMvcTest의 CSRF/세션 검사로 이전 |
| Isbn13ValidatorTest | 10개: 기존 ISBN 검증 |
| 로컬 demo 앱 | 익명 차단, 관리자 로그인/ID 교체, 도서·관리자 화면 200, 로그아웃 차단, 실제 비밀번호·세션 ID 로그 비노출 |
| actionlint 1.7.12 | 공식 릴리스 체크섬 확인 후 워크플로 구문 검사 통과 |

.github/workflows/security.yml은 push/PR/수동 실행에서 실제 MariaDB를 이용한 전체 테스트와 Trivy의 HIGH/CRITICAL 패키지 취약점 및 소스 비밀정보 검사를 실행한다. 외부 Action은 공식 태그에서 확인한 커밋 SHA로 고정하고 contents:read만 부여한다. CI의 DB 서비스 컨테이너는 일회성 합성 테스트용이며 실제 배포가 아니다.

아직 원격에 푸시/워크플로 실행하지 않았으므로 GitHub Actions 통과, 취약점 스캔 통과, 서버 배포 성공을 주장하지 않는다. 원격 실행 후 발견되는 취약점은 무시하지 말고 호환되는 의존성 변경과 회귀 검증으로 처리한다.

## 미해결 운영 범위

- 단일 JVM 세션 레지스트리/세션 핸들이다. 다중 인스턴스에서 단일 로그인 보장이 필요하면 Spring Session/공동 저장소와 분산 로그인 폐기·시도 제한 정책을 추가해야 한다.
- 운영 HTTPS 인증서, 신뢰할 프록시, 외부 로그 수집기, 문의 연락 수단은 아직 구성하지 않았다.
- 로그인 구현 당시에는 미구현이던 Phase 2 업무와 Phase 3 보고서를 이후 추가했다. 아래 Phase 2~4 업무 확장과 TEST_RESULTS.md의 최신 91개 검증을 참고한다.
- GitHub 원격 Actions 및 Trivy의 실제 취약점 데이터베이스 검사는 미실행이다.

## 학습 과제 1개와 복습

auth/login.jsp의 로그인 버튼 문구만 ‘로그인하기’로 바꾸고 저장·새로고침해 확인한다. name/action/CSRF/비밀번호 입력 속성은 바꾸지 않는다. Codex는 과제를 대신 수행하지 않는다.

1. 비밀번호 원문과 해시 중 DB에 저장되는 것은 무엇인가?
2. 계속 사용 중이어도 로그인 후 8시간이 되면 로그인 상태가 유지되는가?
3. USER가 다른 직원 번호로 URL을 바꾸면 화면에서만 숨기는가, Service에서도 접근을 거부하는가?

## 이번 요청의 생성·수정 파일 목록

다음은 이번 로그인 요청에서 다룬 파일이다. 이전 오류 시스템/사용자 JSP 과제에서 이미 변경된 파일 전체를 이번 작업으로 간주하지 않는다.


### 신규 인증·세션 코드

- `src/main/java/com/portfolio/erp/security/AccountAdminService.java`
- `src/main/java/com/portfolio/erp/security/AccountController.java`
- `src/main/java/com/portfolio/erp/security/AdminBootstrap.java`
- `src/main/java/com/portfolio/erp/security/AdminController.java`
- `src/main/java/com/portfolio/erp/security/CurrentUserAdvice.java`
- `src/main/java/com/portfolio/erp/security/ErpPrincipal.java`
- `src/main/java/com/portfolio/erp/security/LiveSessions.java`
- `src/main/java/com/portfolio/erp/security/LoginAttemptFilter.java`
- `src/main/java/com/portfolio/erp/security/LoginAttempts.java`
- `src/main/java/com/portfolio/erp/security/MybatisUserDetailsService.java`
- `src/main/java/com/portfolio/erp/security/PasswordChangeService.java`
- `src/main/java/com/portfolio/erp/security/PasswordPolicy.java`
- `src/main/java/com/portfolio/erp/security/SecurityConfiguration.java`
- `src/main/java/com/portfolio/erp/security/SecurityResponses.java`
- `src/main/java/com/portfolio/erp/security/SessionGuardFilter.java`
- `src/main/java/com/portfolio/erp/security/SessionRevocation.java`
- `src/main/java/com/portfolio/erp/security/StaffAccess.java`
- `src/main/java/com/portfolio/erp/security/StaffProfile.java`
- `src/main/java/com/portfolio/erp/security/StaffService.java`
- `src/main/java/com/portfolio/erp/security/UserAccount.java`
- `src/main/java/com/portfolio/erp/security/UserMapper.java`

### 기존 권한·오류 연결 코드

- `src/main/java/com/portfolio/erp/book/BookService.java`
- `src/main/java/com/portfolio/erp/partner/PartnerService.java`
- `src/main/java/com/portfolio/erp/common/WebConfiguration.java`
- `src/main/java/com/portfolio/erp/common/error/UserErrors.java`
- `src/main/java/com/portfolio/erp/common/error/TraceIdFilter.java`
- `src/main/java/com/portfolio/erp/common/error/ErrorCode.java`

### SQL·설정·도구

- `pom.xml`
- `src/main/resources/db/migration/V2__users_and_login_attempts.sql`
- `src/main/resources/mapper/UserMapper.xml`
- `src/main/resources/application.yml`
- `src/main/resources/application-local.yml`
- `src/main/resources/application-demo.yml`
- `src/main/resources/application-test.yml`
- `src/main/resources/application-prod.yml`
- `src/main/resources/messages.properties`
- `src/main/resources/messages_ko.properties`
- `scripts/auth-init.sh`
- `scripts/env.sh`
- `.github/workflows/security.yml`

### 화면

- `src/main/webapp/WEB-INF/jsp/auth/login.jsp`
- `src/main/webapp/WEB-INF/jsp/auth/password.jsp`
- `src/main/webapp/WEB-INF/jsp/auth/profile.jsp`
- `src/main/webapp/WEB-INF/jsp/admin/users.jsp`
- `src/main/webapp/WEB-INF/jsp/common/header.jspf`
- `src/main/webapp/WEB-INF/jsp/home.jsp`
- `src/main/webapp/WEB-INF/jsp/book/form.jsp`
- `src/main/webapp/WEB-INF/jsp/book/list.jsp`
- `src/main/webapp/WEB-INF/jsp/partner/form.jsp`
- `src/main/webapp/WEB-INF/jsp/partner/list.jsp`

### 테스트

- `src/test/java/com/portfolio/erp/SecurityMvcTest.java`
- `src/test/java/com/portfolio/erp/ProductionSecurityMvcTest.java`
- `src/test/java/com/portfolio/erp/SecureCookieHttpTest.java`
- `src/test/java/com/portfolio/erp/SecurityTestUsers.java`
- `src/test/java/com/portfolio/erp/TestHttpBrowser.java`
- `src/test/java/com/portfolio/erp/BootstrapTest.java`
- `src/test/java/com/portfolio/erp/MasterDataTest.java`
- `src/test/java/com/portfolio/erp/UserErrorMvcTest.java`

### 문서

- `AGENTS.md`
- `README.md`
- `doc/AGENTS.md`
- `doc/PROJECT.md`
- `doc/TODO.md`
- `doc/DECISIONS.md`
- `doc/CHANGELOG.md`
- `doc/LEARNING.md`
- `doc/README.md`
- `doc/SECURITY.md`
- `doc/USER_MESSAGES.md`
- `doc/TEST_RESULTS.md`
- `doc/architecture.md`
- `doc/erd.md`

## Phase 2~4 업무 확장 — 2026-10-20

- GET /orders·상세, /inventory·원장, /reports/sales는 ADMIN/MANAGER/USER 공통 조회다.
- 주문 작성/확정/출고/취소/반품과 입고는 ADMIN/MANAGER, 조정은 ADMIN만 허용한다. 새 업무에도 HTTP + Service 권한을 적용한다. 입력 값으로 actor/상태/총액을 덮어쓸 수 없도록 허용 필드를 제한하며 actor는 인증 principal에서 얻는다.
- 반품 품목의 주문 소속과 남은 수량은 서버가 검사한다. 공동 주문에 직원별 소유권은 만들지 않는다. 직원 프로필의 본인/ADMIN 객체 검사는 유지한다.
- 모든 변경은 CSRF 폼 토큰을 검사한다. requestKey는 중복 방지용이며 보안 토큰을 대체하지 않는다. 기술 예외·DB 인자·비밀번호·세션 ID는 출력/로그하지 않는다. JSP는 동적 값에 c:out/form escaping을 적용한다.
- container는 루프백 Compose HTTP 시연용이며 prod와 혼용하면 시작을 거부한다. Docker 기동·이미지 취약점은 미검증이다. 기존 CI Maven verify가 새 TransactionTest와 새 경로 보안 테스트도 실행하도록 연결돼 있으나 원격 실행은 하지 않았다.
- 현재 전체 91개 통과. 새 경로의 비로그인/USER 변경 거부, MANAGER 조정 거부, CSRF 없는 주문 등록 차단, Service 조정·주문 권한, 실제 JSP 입력 유지/XSS 및 전체 출고/반품 시나리오를 포함한다. 자세한 실행 증거는 TEST_RESULTS.md 참고.

## 2026-10-21 원격 CI 분석 범위 보완

실제 원격 테스트 91개/WAR 성공 후, 최초 Trivy 단계의 Java 분석 대상이 0개인 것을 원문 로그에서 발견했다. 단순한 단계 성공을 보안 검사 완료로 처리하지 않는다. 실행 WAR만 전용 폴더에 복사해 rootfs로 분석하고 JSON list-all-pkgs에서 spring-security-core/MyBatis/MariaDB JDBC가 포함됐는지 필수 검사한다. 비밀정보 검사는 앞선 단계 실패 시에도 실행한다. 실제 최종 결과는 RELEASE.md에 기록한다.

## Boot 4 전환 후 현재 기준 — 2026-10-21

사용자가 Boot 4 전환을 승인했다. Boot 4.1.1/Framework 7.0.9/Security 7.1.1, MyBatis Starter 4.1.0, Tomcat 11.0.27, Jackson 3.1.7을 사용한다. 보안 체인·세션·역할·직원 객체 권한·CSRF 정책은 유지하며 테스트 패키지/JSON 파서와 기본 MVC slice 제외 설정만 이관했다. 오류 비노출은 spring.web.error, 강제 UTF-8은 spring.servlet.encoding이며 실제 바인딩을 신규 테스트로 확인한다. 로컬 최종 92개/WAR 성공. 이전 6.5/Boot 3 보안 검토·실패는 이력이며 최신 원격 상태는 RELEASE.md를 따른다. 무시 목록은 추가하지 않았다.
