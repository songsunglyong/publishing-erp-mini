# Boot 4 전환 — 2026-10-21

사용자의 `boot4 로 해줘` 요청에 따라 Java 21·JSP 실행 WAR·MyBatis XML·jQuery·MariaDB와 기존 데이터/업무 규칙을 유지하며 전환한다. 서버 가입·결제·호스팅은 보류한다. 최종 원격 보안 검사도 통과했으며 상세 이력은 RELEASE.md에 기록한다.

## 실제 구성

| 항목 | 이전 | 전환 후 |
| --- | --- | --- |
| Spring Boot | 3.5.16 | 4.1.1 |
| Framework / Security | 6.2.19 / 6.5.11 | 7.0.9 / 7.1.1 |
| MyBatis Starter / MyBatis-Spring | 3.0.5 / 3.0.5 | 4.1.0 / 4.1.0 |
| MyBatis core | 3.5.19 | 3.5.19 |
| Tomcat | 10.1.61 | 11.0.27 |
| Jackson core/databind | 2.21.7 | 3.1.7 |
| Flyway / MariaDB JDBC | 11.7.2 / 3.5.8 | 12.4.0 / 3.5.10 |

Boot BOM의 Tomcat 11.0.24 대신 동일 Servlet 6.1 계열의 정식 패치 11.0.27을 지정했다. 기본 Jackson 3.1.5에서 발견된 보안 항목은 같은 3.1 계열의 3.1.7 패치로 수정했다. JSON은 Jackson 3을 사용한다. 제3자 Jackson 2 BOM도 2.21.7로 지정하지만 실행 WAR에는 Jackson 2 core/databind가 없다. Jackson 3과 함께 쓰는 jackson-annotations 2.21은 이전 JSON 처리기로 되돌린 것이 아니다.

## 파일과 변경 이유

- pom.xml: Boot/MyBatis 버전, webmvc·security-test·flyway·webmvc-test 스타터로 모듈 구성을 맞췄다.
- common/error/SafeErrorController.java: Boot 4 ErrorController의 새 import. 안전한 /error 동작은 유지한다.
- security/SecurityResponses.java: tools.jackson.databind.ObjectMapper 사용. code/message/traceId 계약을 보존한다.
- application.yml: 오류 정보 비노출은 spring.web.error, UTF-8 강제는 spring.servlet.encoding으로 옮겼다.
- BootstrapTest.java: 기존 HTTP 도구로 정적 리소스를 검증하고 실제 오류/UTF-8 설정 바인딩 테스트 1개를 추가했다.
- ProductionSecurityMvcTest.java: ServerProperties와 MockMvc의 새 패키지 경로.
- SecurityMvcTest.java: MockMvc와 JSON 3 파서 경로. 기존 보안 검증을 유지한다.
- UserErrorMvcTest.java: MVC slice·보안 자동 설정 패키지. 분리된 기본 Servlet 보안 설정은 오류 전용 slice에서만 제외한다. 앱의 보안/CSRF를 끄지 않는다.

업무 Controller/Service/Mapper/XML/JSP와 V1~V3 SQL은 변경하지 않았다. SpringBootServletInitializer와 ServletListenerRegistrationBean은 실제 JAR에서 기존 위치를 확인해 유지했다.

요청 흐름도 그대로다: 브라우저 → Security/CSRF → Controller 검증 → Service 권한·트랜잭션 → MyBatis → DB → JSP. JSON 오류 응답은 Jackson 3이 변환한다. PHP로 비교하면 업무 SQL을 보존한 채 Composer 프레임워크·관련 패키지를 갱신하고 바뀐 API/설정을 맞춘 과정이다.

## 실행과 실제 검증

```sh
./scripts/db-start.sh
./scripts/verify.sh
./scripts/run.sh -Dspring-boot.run.profiles=demo
```

- 초기 전환 92개/WAR 성공(28.761초), 최종 Jackson 3.1.7 패치 후에도 92개(실패/오류/건너뜀 0)/WAR 성공(28.917초). .local/boot4-final-tests.log.
- 기존 91개 회귀 검증과 실제 WebProperties.getError()/CharacterEncodingFilter 설정 바인딩 1개를 포함한다.
- 실행 WAR 실제 로그인과 주문·재고·보고서 JSP 5개 HTTP 200. 기존 DB 이력 검증 성공, 초기화 없음.
- Chromium에서 jQuery 품목 추가/삭제·업무 화면·안전한 404 안내 확인, JS 오류 0개. 새 캡처 06~08 저장, 06·08을 직접 열어 확인했다. 사용자 `작성 저장` 문구 유지. 업무 DB 변경 요청 없음.
- 검증용 8081 WAR는 종료하고 8080 개발 앱을 Boot 4로 재시작했다. 재시작 후 다시 로그인해야 한다.
- 소스 커밋 5667988. 원격 최종 보안 결과는 RELEASE.md 참고. Docker/Compose 기동·호스팅은 미실행이다.

## 전환 중 오류와 해결

WAR initializer의 이동 경로 추정이 틀려 컴파일 실패 → 실제 JAR의 기존 경로로 복구했다. SecurityFilterAutoConfiguration의 이동 경로도 JAR에서 확인해 고쳤다. 오류 전용 테스트는 Boot 4에서 기본 보안 체인이 분리돼 GET 401/POST 403으로 막혔으므로 해당 slice의 제외 설정만 보완했다. 새 설정 테스트는 독립 ErrorProperties 빈 대신 실제 WebProperties.getError() 하위 객체를 검증하도록 고쳤다. 기대값을 낮추거나 실제 보안을 끄지 않았다.

## 직접 수정 과제 — 1개

pom.xml의 `<name>Publishing ERP Mini</name>`을 `Publishing ERP Mini Boot 4 학습`으로 바꾸고 `./scripts/verify.sh`의 프로젝트 표시 이름을 확인한다. artifactId/version·의존성 버전은 유지한다. 대신 수행하지 않았다.

## 이해 확인 — 3개

1. Boot를 바꿀 때 MyBatis Starter 호환 버전도 확인하는 이유는 무엇인가?
2. JSP를 위해 실행 JAR와 실행 WAR 중 무엇을 유지했는가?
3. 기능 테스트 성공만으로 의존성 취약점 검사까지 통과했다고 말해도 되는가?

## 공식 근거

[Boot 4 전환 가이드](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide), [MyBatis 호환표](https://mybatis.org/spring-boot-starter/mybatis-spring-boot-autoconfigure/), [JSP/실행 WAR](https://docs.spring.io/spring-boot/reference/web/servlet.html#web.servlet.embedded-container.jsp-limitations), [Boot 4.1.1 BOM](https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-dependencies/4.1.1/spring-boot-dependencies-4.1.1.pom).

## 최종 원격·설치 재현 결과

25f7f46의 Actions 39544325071 전체 성공: 92개/WAR, Java 고유 71개 분석 범위, HIGH/CRITICAL gate, source secret 검사 단계 성공. 무시 규칙 없음. 실제 GitHub clone+빈 DB에서도 92개/WAR 성공(32.085초), 임시 DB/계정 정리 완료. 초기 Spring/Jackson 발견 항목은 최종 재검사에서 보고되지 않았다.

최종 Jackson 3.1.7 WAR도 8081에서 직접 실행해 로그인/JSP 5개 HTTP 200을 재확인했다. 검증용 PID/포트는 lsof로 확인한 뒤 종료하며 8080 개발 앱은 유지한다.
