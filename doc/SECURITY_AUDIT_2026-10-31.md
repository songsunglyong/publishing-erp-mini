# 보안 점검 — 2026-10-30 시작 / 2026-10-31 정리

## 후속 조치 — 프로젝트 Java 패치 완료

사용자 승인 후 Temurin 21.0.13+9 ARM64를 별도 설치하고 실행 스크립트를 연결했다. 관련 공개 문제 체인 16개 모두 거부·정상 HTTPS 2곳 200·전체 93개/WAR 성공(39.213초)을 확인했다. [JAVA_PATCH.md](JAVA_PATCH.md)에 설치·검증 증거와 범위를 기록한다. 아래 A01의 미조치/공격 미재현 표시는 초기 점검 당시 이력이다. 기존 Homebrew/다른 앱/IDE/서버의 Java는 그대로이며 아래 나머지 운영 보완은 미해결이다.

## 범위와 결론

로컬 작업 사본, 실제 실행 JDK, 새로 빌드한 WAR, 전용 MariaDB 권한, 기존 CI를 확인했다. 기준 커밋은 61ec991이며 기존 2026-10-29 운영 설계 문서 변경을 보존했다. 이번 변경은 SQL Injection 회귀 테스트 1개와 점검 문서다. 애플리케이션 버전·인증 로직·DB 권한·인증서·Docker 태그는 변경하지 않았다.

**우선 조치는 로컬 JDK 인증서 신뢰 문제다.** Maven 라이브러리 스캔 통과와 JDK 인증서 안전성은 다른 검사다. 아직 운영 서버가 없으므로 실제 인터넷 서비스에 대한 침투 점검·TLS/방화벽 배포 검증을 완료했다고 주장하지 않는다.

## 1. 버전과 실제 빌드

| 대상 | 실제 확인 값 | 근거 |
|---|---|---|
| 로컬 Java 배포판 | Homebrew OpenJDK, ARM64/aarch64 | java.vendor=Homebrew, java.home 및 java -version |
| 로컬 Java 빌드 | **21.0.12.1**, version.date 2026-08-18 | OpenJDK Runtime Environment Homebrew (build 21.0.12.1) |
| Java 경로 | /opt/homebrew/Cellar/openjdk@21/21.0.12.1/libexec/openjdk.jdk/Contents/Home | scripts/env.sh가 기본 선택하는 경로의 실제 값 |
| Spring Boot | 4.1.1 | POM과 WAR의 spring-boot JAR |
| Spring Framework | 7.0.9 | WAR의 spring-webmvc JAR |
| Spring Security | 7.1.1 | WAR의 spring-security-core JAR |
| Tomcat | 11.0.27 | WAR lib-provided의 tomcat-embed-core 및 실제 HTTP 테스트 |
| MyBatis Starter / MyBatis-Spring | 4.1.0 / 4.1.0 | WAR의 Starter·autoconfigure·spring JAR |
| MyBatis core | 3.5.19 | WAR의 mybatis JAR |
| MariaDB JDBC | 3.5.10 | WAR의 mariadb-java-client JAR |
| Jackson / Flyway | 3.1.7 / 12.4.0 | WAR JAR 목록 |
| 네이티브 MariaDB | 11.4.13-MariaDB | SELECT VERSION() |
| 이번 원격 CI Java | **Eclipse Temurin 21.0.13+9**, Linux x64 | Actions setup-java 원문 로그 |
| 현재 Docker JDK/JRE 태그 | 양쪽 아키텍처 모두 jdk-21.0.13+9 | Docker Hub amd64/arm64 config 메타데이터 |

POM의 java.version=21은 언어/컴파일 대상이며 보안 패치 빌드까지 고정하지 않는다. 로컬 Homebrew와 CI/Docker의 Temurin 배포판·빌드도 별개다.

WAR SHA256: `c76e2bf74d42ccf3388d5bc759f0f93352ab403442fa96847234b7787cb956fa`.

## 2. 심각도순 발견 사항

점검 우선순위와 공급자가 발표한 CVSS를 구분한다. 조건부 배포 위험은 현재 공격 성공이나 공개 노출을 의미하지 않는다.

| ID | 우선순위 | 발견·근거 | 조치 / 승인 경계 |
|---|---|---|---|
| A01 | **높음** (CVE 공급자 CVSS 9.1, Critical) | 로컬 JDK 21.0.12.1의 기본 TrustManager에 문제와 관련된 G3·AAA 루트가 남아 있다. 공식 CheckCerts 검사도 실패한다. | 같은 Java 21의 공급자 수정 빌드를 별도 설치·프로젝트 JDK 전환 후 전체 검증. 인증서 신뢰 변경은 기존 TLS 연결을 끊을 수 있어 사용자 승인 후 진행. |
| A02 | **높음, 운영에 그대로 사용하면** | compose.yaml은 container 프로필, Secure=false, demo 시드, bootstrap=true다. 현재는 앱을 127.0.0.1에만 노출하므로 공개 유출로 판정하지 않는다. | 별도 prod/TLS·신뢰 프록시·bootstrap 해제 구성. 현재 로컬 Compose를 인터넷 운영 설정으로 사용하지 않는다. |
| A03 | **중간** | 실제 erp_app에 해당 스키마의 DROP/ALTER/CREATE/TRIGGER/EVENT 등 권한이 있다. scripts/db-start.sh:29의 GRANT ALL과 일치한다. 다른 스키마/전역 관리 권한은 발견하지 않았다. | 마이그레이션 계정과 런타임 DML 계정 분리. 현재 기동 Flyway가 앱 계정을 사용하므로 권한을 즉시 회수하면 기동/마이그레이션이 깨질 수 있어 별도 승인·검증. |
| A04 | **중간, 운영 보완** | Docker OS 검사에 MEDIUM/LOW가 남아 있고 3개 이미지가 digest 없는 가변 태그다. 환경변수의 DB/초기 관리자 비밀번호는 Docker 관리자에게 보일 수 있으나 인터넷 공개 증거는 없다. | 하단 OS 항목별 도달 가능성·공급자 패치 추적, 검사한 digest 기록·고정과 재검사, 운영 secret 주입/권한 제한, 최초 설치 후 bootstrap 비밀 제거. 이미지/DB 시작 동작 검증 후 변경. |
| A05 | **중간, 다중 앱 전환 전 조건** | SessionRegistryImpl, synchronized(registry), LiveSessions, SessionGuardFilter가 JVM 한 개를 전제로 한다. | 현 단일 앱을 우회 가능하다고 판정하지 않는다. 두 앱 무정지 전환 전 공유 세션·원자적 단일 로그인·분산 폐기를 별도 구현/승인·검증. |
| A06 | **낮음, 방어 보강** | 명시적인 Content-Security-Policy가 없다. 출력 이스케이프는 실제 테스트를 통과한다. | JSP/정적 자원과 맞는 CSP를 Report-Only로 먼저 확인하고 강제 적용. CSP 추가만으로 XSS 방어를 대체하지 않는다. |

환경 제약: 첫 점검 당시 Mac 디스크 여유가 약 139MB로 테스트 컴파일과 Trivy DB 저장이 실패했다. 이후 여유가 약 4.3GB로 늘어난 것을 확인하여 테스트·검사를 재개했다. 업무 DB나 사용자 파일을 삭제하지 않았고 이번에 받은 검증 완료 Trivy 압축파일 48MB만 정리했다. 이는 앱 코드의 취약점과 별도 운영 위험이다.

## 3. CVE-2026-91403 — 인증서 신뢰 확인

[Oracle 공식 경보](https://www.oracle.com/security-alerts/alert-cve-2026-91403.html)는 eMudhra 관련 인증서 신뢰 변경과 Java 패치를 안내한다. CVSS 9.1은 공급자 점수이며 이 프로젝트에 인증 없이 침입하는 공격을 재현했다는 뜻이 아니다.

실행한 절차:

1. 실제 프로젝트 JDK의 배포판·전체 runtime.version·java.home 확인.
2. [Oracle 안내](https://ops.java/emudhra)에서 연결한 [OpenJDK CheckCerts.java](https://cr.openjdk.org/~mr/CheckCerts.java)를 임시 폴더에 내려받아 코드 확인 후 실행. 해당 코드는 기본 TrustManager의 승인 CA 목록만 읽으며 인증서를 수정하지 않는다.
3. 현재 로컬 JDK에서 공식 검사 exit=1에 해당하는 BAD ROOT 출력 확인. G3·AAA·G1·G2 네 이름이 표시됐다. 기본 cacerts SHA256은 `2675a0aa6e7c158bbccf1181383ee794319e362df9a6bc88e1a5f8469a4c1dd7`.

해석 시 주의: [Adoptium 공식 안내](https://adoptium.net/de/news/2026/10/temurin-october-2026-emudhra)는 G1이 Temurin/Mozilla 정책에서 계속 신뢰되는 것과 단순 검사 경고를 구분한다. 따라서 네 이름이 전부 같은 의미로 손상됐다고 단정하지 않는다. 현재 로컬 JDK에는 관련 G3와 AAA도 남아 있고 수정 빌드도 아니므로 패치 필요로 판단한다. 실제 악성 인증서 체인을 사용한 MITM 재현이나 서명 JAR 공격은 실행하지 않았다. blocked.certs/폐기 검사 등으로 모든 경로가 방어된다는 검증도 없다.

영향 경로: Maven의 외부 HTTPS 의존성 다운로드와 향후 Java HTTPS 외부 연동이다. 현재 앱의 업무 코드에는 임의 외부 HTTPS 호출·사용자 지정 TrustManager·인증서 검증 해제 코드를 발견하지 않았다. MariaDB는 로컬 TCP/컨테이너 내부 연결이며 이번 CVE를 웹 화면 수신 TLS 자체의 결함으로 해석하지 않는다. Maven WAR 라이브러리 검사는 JDK 신뢰 저장소를 분석하지 않는다.

수정 후보는 공식적으로 대응을 안내한 **Temurin 21.0.13+9** 등 같은 Java 21의 공급자 수정 빌드다. 공급자별 수정 빌드/후속 우회 수정·CA 정책을 다시 확인해야 하며 버전 숫자만으로 동일하다고 가정하지 않는다. 로컬 JDK 교체, CA 삭제, 검증 무시, 전역 JAVA_HOME 수정은 이번에 하지 않았다. CI는 이미 Temurin 21.0.13+9에서 기존 92개 테스트를 통과했지만 전체 실제 외부 연동의 인증서 호환성까지 보장하지 않는다.

## 4. Maven 의존성 검사

- 로컬 실제 Surefire 해석 classpath에서 공개 Maven 좌표/버전 **113개(런타임·테스트)**를 추출해 [OSV querybatch API](https://google.github.io/osv.dev/api/#tag/api/paths/~1v1~1querybatch/post)에 조회했다. 응답에 매칭 취약점 **0건**. 소스·비밀번호는 전송하지 않았다. 빌드 플러그인의 전체 의존성 폐쇄를 검사한 것은 아니다.
- 기존 원격 main 61ec991에서 [Actions 41075646210](https://github.com/songsunglyong/publishing-erp-mini/actions/runs/41075646210)을 새로 실행했다. **92개/WAR, 고유 Java 패키지 71개 분석 범위, HIGH/CRITICAL gate, 소스 비밀정보 검사 모두 성공**. 이번 로컬 추가 테스트는 아직 푸시하지 않아 원격은 93개가 아니다.
- 로컬 Trivy v0.76.1은 공식 배포 파일 SHA256과 대조 후 실행했다. 최초 DB 다운로드는 디스크 부족으로 실패하여 성공 처리하지 않았고 공간 확인 후 전체 심각도 WAR·베이스 이미지 검사를 다시 시작했다. 최종 수치는 아래 후속 결과에 기록한다.
- 검사 도구와 DB는 알려진 취약점 범위다. 새로운 취약점이 없다는 보장이나 모든 코드 경로의 안전 증명으로 해석하지 않는다. 취약점 무시 목록을 추가하지 않았다.

## 5. 애플리케이션 보안

| 항목 | 실제 코드·검사 | 결과와 범위 |
|---|---|---|
| SQL Injection | mapper XML 전체에서 사용자 값 #{} 바인딩, ${} 직접 치환 없음. 동적 정렬/식별자 사용자 조합 없음. JdbcTemplate은 고정 SELECT 1만 사용 | 신규 sqlInjectionTextRemainsLiteralSearchData가 도서·거래처의 공격 구문을 포함한 한 행만 반환하고 전체 건수도 1임을 실제 MariaDB에서 검증. 대표 경로이며 모든 입력의 침투 테스트를 의미하지 않음 |
| XSS | c:out, defaultHtmlEscape=true, form htmlEscape, 사용자 값을 .html()/innerHTML/eval로 넣는 경로 미발견 | 실제 Tomcat/JSP에서 script·img/onerror 및 입력 오류·주문 표시 이스케이프 테스트 통과. 이번 검사는 HTTP 출력 기반이며 브라우저에서 모든 문맥의 실행 시도를 재현한 것은 아님 |
| CSRF | Security 기본 CSRF 유지, 모든 변경 POST, JSP hidden 토큰·jQuery 헤더 | 토큰 없음/타 세션 토큰 POST 거부, GET 비활성화 거부, 로그아웃 POST 검사 통과 |
| 인증 | MyBatis UserDetailsService/DaoAuthenticationProvider/DelegatingPasswordEncoder, 원문 해시 비교 위임 | 정상/실패/없는 계정 동일 안내·실패 제한·비밀번호 지움 테스트 통과 |
| 인가 | URL + @PreAuthorize Service 검사, StaffAccess 본인/ADMIN | USER 관리자 접근·업무 변경 거부, MANAGER 재고 조정 거부, 직원 타인 접근 거부 통과. 공동 도서/거래처/주문에 임의 직원 소유권을 만들지 않음 |
| 세션 | ID 변경, COOKIE만, 30분/8시간, max 1, 로그인 직렬화, DB auth_version 확인 | 단일 JVM 동시 로그인·기존 세션 폐기·차단·비밀번호 변경·만료 통과. 다중 JVM은 A05 |
| 오류·로그 | ErrorNotice/추적 ID, 예외 클래스만 허용, SQL/Throwable/폼 입력 비노출 | 오류 메시지·입력 보존·인증정보 비노출 자동 테스트 통과. 운영 외부 로그 수집기·프록시는 미배포 |

PHP 비교: #{}는 PDO의 바인딩 파라미터, JSP escaping은 htmlspecialchars에 대응한다. CSRF·권한·세션 검사는 각각 따로 필요하다. Maven/Composer 패키지가 안전해도 PHP/OpenSSL 또는 Java 런타임의 인증서 저장소는 별도로 패치해야 하는 것과 같다.

## 6. 비밀정보와 DB 권한

- Trivy 소스 비밀정보 검사: 로컬 현재 작업 사본(.git/target/.local 제외) 매칭 0건, 원격 소스 검사도 성공.
- 현재 생성된 DB/초기 관리자 비밀값 3개를 메모리에서 읽어 값 자체를 출력하지 않고 추적 파일·도달 가능한 Git 이력 blob **274개**와 대조했다. 매칭 0건. 과거의 알 수 없는 모든 비밀을 검출한 것으로 주장하지 않는다.
- 현재 비밀값의 WAR 비라이브러리 엔트리·이번 점검 로그 8개 대조도 매칭 0건. 소스 스캔·이미지 검사는 도구별 범위를 구분한다.
- .local/db.env 및 auth.env는 실제 0600, Git 제외. 민감 경로가 추적되는 경우는 발견하지 않았다. Dockerfile은 .local/.env를 복사하지 않고 .dockerignore도 제외한다.
- 환경변수는 암호화 저장소가 아니다. Docker 관리 권한자·같은 사용자 권한의 프로세스가 읽을 수 있으므로 운영 권한과 secret 수명 관리를 별도로 설계한다.
- DB 메타데이터에서 erp_app은 업무 스키마에 넓은 권한, erp_test는 테스트 스키마에 넓은 권한이다. 글로벌 USER_PRIVILEGES는 USAGE이며 전역 관리 권한·GRANT OPTION은 확인하지 않았다. 비밀번호 해시나 업무 테이블은 이 권한 조회에서 읽지 않았다.

## 7. Docker 이미지·포트

- Docker CLI/daemon이 없어 compose config/up, 실제 앱 컨테이너 사용자·포트 바인딩 검증은 미실행이다. Docker 레지스트리 읽기 전용 조회와 이미지 분석을 실제 기동과 구분한다.
- Dockerfile 앱 실행은 USER 10001:10001, 다단계 JDK→JRE, WAR 포함, no-new-privileges. 빌드는 root지만 최종 앱 실행은 non-root다. MariaDB 이미지의 config User가 비어 있다는 사실만으로 DB 프로세스가 계속 root라고 판정하지 않는다.
- compose의 앱 publish는 127.0.0.1:8080이고 DB는 ports 항목이 없다. EXPOSE 8080/3306은 인터넷 공개 설정이 아니다. Docker 내부 0.0.0.0 바인딩과 호스트 publish 범위를 구분한다.
- 실제 lsof에서 이번에 시작한 네이티브 DB가 **127.0.0.1:3307**에 리슨하는 것을 확인했다. 점검 시작 당시 앱/DB는 실행 중이지 않았다. 실제 외부 서버 포트·클라우드 방화벽은 아직 없다.
- 현재 태그를 amd64/arm64 각각 조회했다. JDK/JRE JAVA_VERSION은 모두 jdk-21.0.13+9이며 MariaDB는 11.4.13이다. 로컬 캐시나 예전 배포 이미지가 자동 갱신됐다는 뜻은 아니다.

조회한 multi-platform index digest:

| 이미지 태그 | index digest |
|---|---|
| eclipse-temurin:21-jdk-jammy | sha256:4b1a74fbffbe6b4e7b6f0171a472478282fb463790a51edf10e465d578f22b69 |
| eclipse-temurin:21-jre-jammy | sha256:1eac8b8fee11ca7d6725a48bc5d41992ed1561504ebae25490d2d062847e8269 |
| mariadb:11.4 | sha256:d1891ede5a559f1086f55053bb8cc1569b0298338c4d72d71725a7f74f6ba620 |

베이스 이미지 digest/레이어 검사 결과와 최종 앱 이미지의 결과는 별개다. 실제 완성 앱 이미지 빌드·스캔·TLS 기동은 후속 검증 대상이다.

## 8. 수정 전후 검증 계획

| 대상 | 수정 전 이번 확인 | 수정 후 합격 기준 |
|---|---|---|
| JDK/인증서 A01 | Homebrew 21.0.12.1, 관련 CA 존재, 공식 검사 경고 | 공급자 수정 빌드/CA 정책 확인. 관련 악성 체인 거부와 정상 Maven/실제 외부 HTTPS 성공. G1 등 공급자 정책 차이를 고려하며 단순 네 이름 검사만으로 결론 내리지 않음 |
| Java 전환 | 로컬 기존 JDK의 93개 통과, 원격 Temurin 패치 JDK의 기존 92개 통과 | 새 로컬 JDK로 같은 93개/WAR·실제 JSP/쿠키 회귀, 비밀/SQL 비노출 유지 |
| DB 최소 권한 A03 | 스키마 DROP/ALTER 등 허용 | 전용 복원 DB에서 마이그레이션 계정으로 초기화·업그레이드. 런타임 계정 SELECT/INSERT/UPDATE/DELETE는 성공, DROP/ALTER/다른 DB 접근은 거부 |
| 운영 프로필 A02 | 로컬 container의 Secure=false·demo 시드 | prod 실제 HTTPS에서 Secure/HttpOnly/SameSite·CSRF·리다이렉트·신뢰 프록시 검증. demo/운영 혼용 실패, bootstrap 비활성화 |
| 이미지 A04 | 태그 메타데이터와 베이스 분석 | 승인된 digest로 빌드·최종 이미지 OS/JAR·비밀정보 검사, 실행 UID 확인, 호스트 3306 비공개·앱 루프백 제한·로그/inspect 비밀 취급 확인 |
| 세션 A05 | 단일 JVM 검증 통과 | A→B 로그인 유지, 두 앱 동시 로그인은 하나만 유효, 차단/암호 변경 즉시 폐기, 30분/8시간·CSRF·공유 저장소 장애 거부 |
| CSP A06 | 출력 이스케이프 검사 통과 | Report-Only 검토 후 위반·실제 기능 회귀 없이 강제, 정상 jQuery/JSP 동작 유지 |

호환성에 영향을 줄 수 있는 버전·CA·권한·세션·CSP 변경은 승인 전 적용하지 않는다. 취약점을 숨기기 위해 인증서 검증·CSRF·보안 테스트를 끄지 않는다.

재현 실행은 프로젝트 전용 DB가 필요하다. scripts/verify.sh가 전용 테스트 스키마를 사용하며 테스트 시작 시 _test 접미사를 확인한다. 이번 전체 테스트는 로컬 **93개, 실패/오류/스킵 0, WAR 성공(03:03)**. 초기 두 번은 DB 연결이 유지되지 않아 실패했고, 전경 DB 세션 유지 후 디스크 부족에 의한 컴파일 실패도 구분했다. 최종 성공 전에 실패 결과를 숨기지 않는다. 원격 검사는 앞의 CI 링크 기준 **92개**다.

이번에 dependency:tree 별도 호출은 플러그인 해석에 실패했으므로 성공했다고 기록하지 않는다. 대신 실제 생성 WAR JAR 목록과 성공한 Surefire classpath를 버전·좌표 확인 근거로 사용했다.

## 9. 증거와 변경 파일

Git 제외 .local/security-audit-20261030-* 파일: tests-final.log, test-summary.json, certificates.log, ci.json/ci.log, osv.json, coordinates.json, exact-secrets.json, secrets.json, db-grants.txt, image-metadata.json, 각 Trivy 결과/실행 로그. 증거 이름은 점검 시작일을 유지한다. 원문 비밀값을 보고서에 복사하지 않는다. Trivy 실행 파일·DB 캐시는 저장소 밖의 임시 폴더를 사용했다. 캐시는 Java 패치 검증 중 정리했으며 비밀 없는 결과만 보관한다.

- src/test/java/com/portfolio/erp/MasterDataTest.java: SQL Injection 회귀 1개. 공격 구문을 DB 값으로 저장·검색하여 파라미터 바인딩을 검증한다. @Transactional로 테스트 데이터를 되돌린다.
- 이 보고서: 버전, 발견 사항·근거·영향 조건·수정 전후 검증.
- TODO/CHANGELOG/LEARNING/TEST_RESULTS/SECURITY_REVIEW: 이번 결과와 승인 대기 상태 연결. 기존 운영 설계 이력은 보존한다.

직접 수정 과제 **1개**: 보고서의 A01을 읽고 본인이 사용할 Java의 배포판·전체 빌드 번호를 별도 메모로 적는다. 아직 설치/CA 삭제는 하지 않는다.

이해도 확인 **3개**:

1. Maven 취약점 스캔이 통과해도 Java 인증서 저장소를 따로 검사해야 하는 이유는 무엇인가?
2. 앱 DB 계정에서 DROP 권한을 바로 제거하면 현재 Flyway 기동에 어떤 영향을 줄 수 있는가?
3. Dockerfile의 EXPOSE와 Compose의 ports 중 실제 호스트 공개 범위를 정하는 것은 무엇인가?

## 전체 심각도 Trivy 후속 결과

실제 Trivy v0.76.1 원격 이미지 분석을 **컨테이너 실행 없이 완료**했다. OS 패키지 전체 심각도 기준이며 수치는 패키지×취약점 항목 수다. 하나의 CVE가 여러 패키지에 해당하면 중복 집계된다. 취약점이 있어도 검사 자체의 성공 코드는 0일 수 있으므로 JSON 내용을 판독했다.

| 대상(linux/amd64) | CRITICAL | HIGH | MEDIUM | LOW | 고유 CVE |
|---|---:|---:|---:|---:|---:|
| eclipse-temurin:21-jre-jammy | 0 | 0 | 27 | 16 | 29 |
| mariadb:11.4 | 0 | 0 | 21 | 7 | 24 |
| eclipse-temurin:21-jdk-jammy(빌드 단계) | 0 | 0 | 45 | 46 | 36 |

원문에서 공개 필드만 추린 [전체 162개 항목 CSV](SECURITY_AUDIT_2026-10-31_IMAGES.csv)에 패키지·버전·심각도·CVE·수정 버전·상태를 보관했다. 실행 JRE/DB와 빌드 JDK의 노출 경로가 다르며 동일 CVE를 합쳐 162개의 독립 공격으로 해석하지 않는다. MEDIUM 예시는 dash의 CVE-2026-102473/102474와 libc 계열의 CVE-2026-18374/89092다. 도구의 영향 버전 매칭 결과이며 우리 앱에서 악용을 재현한 것은 아니다. 검사 DB의 FixedVersion은 162항목 모두 비어 있어 단순 패키지 업데이트로 전부 해결된다고 안내하지 않는다. 공급자 미수정 상태도 포함하고 무시/ignore-unfixed로 숨기지 않았다. 패치 제공 여부와 실제 도달 가능성을 검토한 뒤 베이스 변경·재빌드/검증을 진행한다.

디스크 제약으로 로컬 전체 Java 인덱스 DB 추출은 재시도에서도 실패했다. 로컬 WAR Trivy 전체 심각도 검사는 **미완료**다. Maven은 실제 동일 버전의 원격 Trivy HIGH/CRITICAL gate와 로컬 OSV 113좌표 매칭 0건으로 확인했다. Docker 분석은 pkg-types=os와 JAR 파일 제외로 OS 범위에 한정해 완료했다. Java 런타임 CA와 Maven/OS 세 범위를 혼동하지 않는다.

검사 명령 형태: `trivy image --image-src remote --platform linux/amd64 --scanners vuln --pkg-types os --skip-files '**/*.jar' --list-all-pkgs --parallel 1 --format json IMAGE`. 공식 실행 파일 체크섬, DB metadata와 결과는 Git 제외 임시/로컬 증거에 남겼다. arm64는 이미지 메타데이터만 확인했으며 OS 취약점 분석은 amd64다. 완성 앱 이미지 빌드·레이어 비밀정보·기동/TLS/외부 포트 검증은 아직 미실행이다.
