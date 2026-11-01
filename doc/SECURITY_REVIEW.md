# 2026-10-21 배포 전 의존성 검토

## Java 패치 후속 — 2026-10-31

사용자 승인으로 프로젝트 스크립트를 Temurin 21.0.13+9로 전환하고 관련 문제 체인 16개 거부·정상 HTTPS 2곳 200·93개/WAR 성공을 확인했다. [JAVA_PATCH.md](JAVA_PATCH.md). 아래 초기 점검의 로컬 JDK 미조치 표시는 당시 기록이다. 기존 Homebrew/다른 프로그램·IDE·서버·Docker를 일괄 패치한 것은 아니며 운영 권한/프로필·공유 세션·이미지 보완은 남아 있다.

## 최신 보안 점검 — 2026-10-31

[SECURITY_AUDIT_2026-10-31.md](SECURITY_AUDIT_2026-10-31.md)에 현재 코드·JDK·CA·라이브러리·DB 권한·Docker 점검을 갱신했다. 로컬 93개/WAR, 원격 main 92개·Java 71개 HIGH/CRITICAL/secret gate와 OSV 113좌표 검사는 성공했지만 **로컬 JDK의 CVE-2026-91403 관련 신뢰 저장소는 패치가 필요**하다. 운영 프로필/DB 최소 권한·이미지/다중 세션 보완도 별개다. 이전 Maven 검사 성공을 전체 공개 배포 보안 완료로 해석하지 않는다.

## 2026-10-21 당시 결론 — Boot 4 최종 검사 성공

최종 25f7f46/Actions 39544325071에서 92개/WAR·Java 71개 분석·HIGH/CRITICAL gate·source secret 검사 단계가 모두 성공했다. Framework 7.0.9와 Jackson 3.1.7/Tomcat 11.0.27로 전환했고 이전 보고 항목은 사라졌다. 무시/예외 규칙은 없다. 아래 Boot 3.5 미해결 표시는 해결 전 이력이다. 모든 잠재 결함의 부재를 보장하지 않으며 실제 호스팅은 아직 하지 않았다.


## 최신 전환 상태 — 사용자 Boot 4 승인

사용자 승인 후 Boot 4.1.1/Framework 7.0.9로 전환해 이전 Spring 항목 2개가 원격 분석에서 사라졌다. Boot 4 기본 Jackson 3.1.5에서 같은 계열 패치가 필요한 항목이 보고돼 3.1.7을 적용했다. 로컬 최종 92개/WAR 성공. 원격 최종 결과는 RELEASE.md를 참고한다. 아래 Boot 3.5 상태와 미결정 표시는 이전 검토 이력이다.


## 발견과 검사 범위

최초 fs 방식 Trivy는 Java 분석 대상이 0개였고 단계 성공을 보안 완료로 인정하지 않았다. rootfs로 실행 WAR를 분석하고 전체 패키지 JSON에서 필수 라이브러리 존재를 검사하도록 고쳤다. 이후 60개 고유 Java 패키지가 확인됐다.

실제 CI 39539226588, 커밋 246e38f에서 HIGH 5개/CRITICAL 5개 의존성 항목으로 실패했다. 이는 스캐너 분류이며 개발사 등급·실제 노출 조건과 구분한다. 검사 기준을 낮추거나 CVE를 무시 목록에 넣지 않았다.

## 패치 대상

| 라이브러리 | 기존 | 적용한 패치 | 발견 항목 |
| --- | --- | --- | --- |
| Jackson core/databind | 2.21.4 | BOM 2.21.7 | CVE-2026-89407, 89425, 68497, 91776, 91777 |
| Tomcat embed core/관련 모듈 | 10.1.55 | 10.1.61 | CVE-2026-65182, 65905, 68525 |

Boot 3.5.16은 유지하고 tomcat.version/jackson-bom.version만 명시적으로 패치한다. Tomcat 10.1.58은 릴리스 투표 미통과여서 정식 배포가 없음을 확인했고, 실제 Maven Central에 있는 10.1.61을 선택했다. Jackson BOM 2.21.7도 공식 배포를 확인했다. 패치 후 로컬 전체 91개(실패/오류/건너뜀 0)와 실행 WAR 빌드가 성공했다(25.877초). WAR 내부 Tomcat 10.1.61 및 Jackson core/databind 2.21.7 포함을 확인했다. 원격 최종 스캔 결과는 RELEASE.md에 기록한다.

근거: [Tomcat 공식 보안 안내](https://tomcat.apache.org/security-10), [Jackson BOM 2.21.7](https://repo.maven.apache.org/maven2/com/fasterxml/jackson/jackson-bom/2.21.7/jackson-bom-2.21.7.pom), [Tomcat embed 10.1.61](https://repo.maven.apache.org/maven2/org/apache/tomcat/embed/tomcat-embed-core/10.1.61/tomcat-embed-core-10.1.61.pom).

## 남은 Spring 항목과 배포 판단

| CVE | 공식 노출 조건 | 현재 코드 검토 |
| --- | --- | --- |
| CVE-2026-47884 | XsltView, /** 매핑, 명시하지 않은 뷰 이름 | JSP를 사용하며 XsltView 설정과 해당 렌더 매핑이 없음. 이 검토를 버전 수정으로 표현하지 않음. |
| CVE-2026-47890 | SSE로 뷰 조각 전송, 공격자가 다른 사용자에게 보낼 데이터를 제어 | SSE/뷰 조각 스트리밍 기능이 없음. 이 검토를 버전 수정으로 표현하지 않음. |

두 항목은 실제 사용하는 spring-webmvc 6.2.19에 남아 있다. 공식 수정 안내는 OSS 7.0.9 또는 Enterprise 전용 6.2.20이다. Maven Central의 6.2.20은 제공되지 않음을 확인했다. Boot 3.5에서 Spring 7 JAR만 억지로 바꾸지 않는다. 무료 수정 계열 사용은 Boot 4/MyBatis·Security 호환성·전체 회귀 검증을 포함한 별도 전환 작업으로 다룬다.

근거: [Spring XsltView 안내](https://spring.io/security/cve-2026-47884/), [Spring SSE 안내](https://spring.io/security/cve-2026-47890/).

현재 기능에서 해당 조건을 찾지 못했다는 검토와 보안 패치 완료는 다르다. 두 CVE에 대한 무시 규칙·임시 예외를 추가하지 않았다. 모든 취약점이 해결됐다고 보고하거나 바로 인터넷 공개하지 않는다. 사용자의 기존 Boot 3.5 학습 기준을 유지할지, Boot 4로 전환할지 확인 후 진행한다.

## 패치 재검사 결과

커밋 605d6eb, Actions 39539817704에서 전체 91개/WAR와 Java 60개 범위·secret 검사 단계가 성공했다. Jackson/Tomcat 8개 발견 항목은 사라졌고 spring-webmvc의 두 CVE가 남아 취약점 gate는 실패다. 패치가 전혀 적용되지 않은 상태나 분석 대상 0개 성공과 구분한다.
