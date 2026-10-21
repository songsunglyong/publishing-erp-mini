# 2026-10-21 배포 전 의존성 검토

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
