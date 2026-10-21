# 사용자 메시지와 오류 처리 지침

## 범위와 현재 상태

2026-10-20 사용자 요청으로 Phase 0·1의 공통 오류 처리를 확장했다. 주문이나 로그인 기능을 새로 구현한 것은 아니다. 2026-10-20 후속 요청으로 실제 Spring Security 로그인·권한·세션/CSRF 처리기에 공통 오류 계약을 연결했다. SECURITY.md 참고.

## 문구 작성

- 사용자를 탓하지 않고 짧고 자연스러운 한국어로 문제와 다음 행동을 안내한다.
- 사실을 모르면 원인이나 해결책을 단정하지 않는다. 알 수 없는 DB 오류를 업무 규칙 위반으로 추정하지 않는다.
- 로그인 실패는 존재하지 않는 계정과 틀린 비밀번호에 동일한 LOGIN_FAILED 메시지를 사용한다. 계정 잠김/탈퇴/미등록 등의 상태를 공개하지 않는다.
- 장애 후 저장 재시도는 자동으로 실행하지 않는다. 먼저 목록에서 반영 여부를 확인하도록 안내한다. 통신 장애가 저장 취소를 보장하지 않기 때문이다.
- 문의 경로가 아직 없으므로 가짜 전화번호·메일·지원 링크를 만들지 않는다. 현재 안내는 ‘운영 담당자에게 오류 코드·추적 ID 전달’이다. 실제 운영 전 담당자와 연락 수단을 정해야 한다.
- 사용자에게 보내는 텍스트는 messages_ko.properties에서 관리한다. messages.properties는 동일한 한국어 기본 번들이다. 고정 한국어 LocaleResolver를 사용하며 두 번들을 함께 갱신한다.

## 공통 계약

ErrorCode의 이름은 공개 식별자다. 이름을 임의로 변경하거나 기존 코드를 다른 의미로 재사용하지 않는다. ErrorNotice에는 code, message, traceId 세 필드만 둔다. HTML은 공통 error-notice.jspf, JSON은 동일 ErrorNotice로 응답한다. /health는 오류도 JSON이다. 도서·거래처 저장 폼은 HTML 응답 계약을 유지한다.

| 코드 | 의미 / 기본 HTTP 상태 |
|---|---|
| VALIDATION_FAILED | 입력 검증 / 400. 기존 저장 폼 재표시는 200 유지 |
| BAD_REQUEST | 잘못된 번호·요청 / 400 |
| NOT_FOUND | 없는 데이터·주소 / 404 |
| METHOD_NOT_ALLOWED | 지원하지 않는 요청 방식 / 405 |
| NOT_ACCEPTABLE / UNSUPPORTED_MEDIA_TYPE | 지원하지 않는 응답/입력 형식 / 406, 415 |
| DUPLICATE_ISBN / DUPLICATE_PARTNER_CODE | 도서 ISBN / 거래처 코드 중복 / 409. 기존 폼 재표시는 200 |
| DUPLICATE_DATA | 필드가 특정되지 않은 중복 / 409 |
| BUSINESS_RULE | 명시적으로 식별한 업무 상태 위반 / 409. 폼에서는 200 |
| LOGIN_FAILED / LOGIN_REQUIRED | 공통 인증 실패 / 로그인 필요 / 401. 실제 인증 연결 완료 |
| SESSION_EXPIRED | 미사용/절대 시간 초과·교체·폐기된 세션의 보호 요청 / 401 |
| ACCESS_DENIED | 권한 부족 / 403. 실제 역할 검사 연결 완료 |
| CSRF_INVALID | 세션 토큰 누락·불일치 / 403 |
| SERVICE_UNAVAILABLE | DB 연결·일시적 자원 장애 / 503 |
| SERVER_ERROR | 분류되지 않은 서버·DB 오류 / 500 |

오류 코드는 전역 의미를 나타내며 필드별 수정 안내는 validation.* 및 typeMismatch.* 메시지 키로 세분화한다. 입력 검증의 필드 메시지 키는 공개 에러 코드와 별개다.

## 요청 흐름과 파일 역할

1. TraceIdFilter가 서버에서 UUID를 생성하고 요청·X-Trace-Id 응답 헤더·MDC에 연결한다. 외부 추적 헤더는 신뢰하지 않는다.
2. Form 검증은 메시지 키로 한국어 안내를 가져온다. Controller는 BindingResult가 있으면 같은 JSP와 입력을 반환한다.
3. 중복/업무/장애 발생 시 UserErrors가 코드와 안전한 메시지를 선택한다. 도서·거래처 폼은 기존 입력과 수정 대상 번호를 유지한다. DB 연결 장애와 예상치 못한 오류도 폼에서 503/500으로 보여 준다.
4. GlobalExceptionHandler는 기타 MVC 예외를 표준화한다. 잘못된 검색 조건은 검색 폼을 유지하고 DB 조회 없이 400으로 반환한다.
5. SafeErrorController는 컨테이너 /error dispatch를 처리하며 ERROR_MESSAGE나 예외 본문을 출력하지 않는다. 프로세스가 실행되지 않는 장애는 앱이 화면을 줄 수 없으므로 브라우저의 연결 오류로 표시된다.
6. 공통 header의 error-notice.jspf가 코드·메시지·문의용 ID를 출력한다. 필드별 오류는 Spring form:errors가 안내한다.

PHP 비교: try/catch + 공통 오류 코드 배열 + 번역 파일 + 동일 폼 재출력 + 로그용 요청 번호를 Spring ControllerAdvice, MessageSource, BindingResult, Filter로 나눈 구조다. SQL은 계속 MyBatis가 실행한다.

## 입력 보존과 보안

- 일반 입력은 검증/중복/저장 실패 시 같은 요청의 model과 BindingResult로 보존한다. 잘못된 숫자·날짜의 원문도 필드 오류를 고칠 수 있도록 유지한다.
- 비밀번호·인증 토큰·민감한 인증정보는 재표시하거나 세션/flash/localStorage에 저장하지 않는다. 도서·거래처 폼에는 비밀번호 필드가 없다. 새 로그인·비밀번호 변경·계정 생성 폼도 비밀번호를 되돌려 출력하지 않는다. 계정 생성 오류는 아이디/이름만 허용 목록으로 보존한다.
- CSRF·세션 만료처럼 요청 신뢰를 확인할 수 없는 경우 원문 요청을 다시 폼에 복구하지 않는다. 새로고침 전 사용자가 내용을 따로 보관하도록 안내한다.
- HTML 텍스트·속성은 c:out 및 Spring form 태그의 htmlEscape=true/defaultHtmlEscape=true로 출력한다. 메시지에 HTML을 넣거나 escapeXml=false로 바꾸지 않는다. 사용자 값을 JavaScript 문자열이나 HTML 조각으로 연결하지 않는다.
- SQL·Stack Trace·예외 메시지·바인딩 내부 오류·계정 존재 여부는 사용자에게 보내지 않는다. Boot 기본 오류 상세 노출 설정도 모두 끈다.
- 로그는 오류 코드, 서버 생성 추적 ID, 예외 클래스, 정적 발생 위치만 허용한다. Throwable 자체나 getMessage(), request parameter/body/header/query string, form 객체를 기록하지 않는다. SQL/바인딩 값을 출력하는 DB·MyBatis 로그는 꺼 둔다. 운영에서 DEBUG, HTTP 덤프, SQL 프록시, access log query string을 임의로 켜지 않는다.
- 비밀번호·쿠키·세션/CSRF 토큰·연락처·메일·주소·본문·폼 입력값은 로그 금지다. 에러 응답에는 Cache-Control: no-store를 사용한다.
- 시작 실패·JSP 엔진 등 외부 라이브러리의 자체 로그까지 안전하다고 가정하지 않는다. 합성 데이터만 사용하고 운영 로그 수집기·컨테이너 로그 정책은 실제 배포 단계에서 별도 점검한다.

## 확장 방법

- 새 업무 규칙에는 해당 상황을 설명할 수 있는 별도 ErrorCode와 한국어 메시지를 추가한다. 원인을 모르는 예외를 BUSINESS_RULE로 덮지 않는다.
- 실제 인증 처리기는 없는 계정·틀린 비밀번호·차단·시도 제한을 LOGIN_FAILED로 통일한다. 권한/세션 오류는 같은 ErrorNotice 계약을 쓴다. 실제 인증/세션 테스트는 SecurityMvcTest와 HTTP 검사에서 수행한다.
- MockMvc로 상태·코드·입력 보존·로그 비노출을 검사한다. MockMvc는 JSP를 실행하지 않으므로 실제 HTTP 테스트로 JSP 컴파일·HTML escaping도 검증한다.
- 로컬 명령: ./scripts/db-start.sh 후 ./scripts/verify.sh. DB를 띄운 세션을 유지한다. 단일 MockMvc 검사: ./scripts/verify.sh -Dtest=UserErrorMvcTest.

## 학습 과제와 이해 확인

과제 1개: ui.error.title의 한국어 문구를 ‘처리 안내’로 바꾸고 존재하지 않는 도서 수정 주소에서 표시를 확인한다. messages_ko.properties와 기본 번들을 함께 바꾼다. properties 변경은 개발 앱 재시작이 필요하다. Codex는 과제를 대신 수행하지 않는다.

복습 질문 3개:

1. 사용자 안내 문구는 예외의 getMessage()와 messages_ko.properties 중 어디에서 가져오는가?
2. 장애 문의 시 비밀번호와 추적 ID 중 무엇을 전달하는가?
3. 중복 등록 오류가 났을 때 입력값을 유지하려면 리다이렉트하는가, 같은 폼을 다시 보여 주는가?

## 메시지 파일 수정이 반영되지 않을 때

현재 개발 실행은 src/main/resources의 메시지를 Maven이 target/classes로 복사하고, Spring이 그 사본에서 읽는다. JSP의 개발 재컴파일 설정과는 별개다. 파일을 저장하고 브라우저만 새로고침하면 메시지가 이전 값으로 남을 수 있다.

1. 원본 messages_ko.properties와 messages.properties의 저장 내용을 확인한다.
2. 앱을 실행한 터미널에서 Ctrl+C로 앱을 종료한다. 다른 실행 프로세스는 종료하지 않는다.
3. 프로젝트 루트에서 아래 명령으로 다시 실행한다. Maven이 메시지 사본을 갱신하고 새 앱이 읽는다.

```sh
./scripts/db-start.sh
./scripts/run.sh -Dspring-boot.run.profiles=demo
```

4. 앱 시작 후 브라우저를 새로고침한다. java -jar로 기존 WAR만 재시작하면 원본 변경은 포함되지 않으므로 WAR 재빌드가 필요하다.

2026-10-20 사용자 과제에서 원본의 처리 안내와 실행 사본의 안내가 다른 것을 확인했고, 위 개발 재실행 후 실제 오류 응답에 처리 안내가 반영됐다. 현재는 메시지 파일 자동 재로딩 구성을 추가하지 않았다.

## 로그인 확장 메시지

PASSWORD_POLICY는 새 비밀번호 길이/바이트 안내, PASSWORD_CHANGE_FAILED는 현재 비밀번호 확인 실패, DUPLICATE_USERNAME은 관리자 계정 생성 중 아이디 중복에 사용한다. 공개 로그인에서는 DUPLICATE_USERNAME이나 계정 차단 여부를 노출하지 않는다. SESSION_EXPIRED는 다시 로그인하도록 안내한다. 로그인 시도 제한 안내는 존재하지 않는 아이디에도 동일하게 적용한다.

## 주문·재고·보고서 오류 확장 — 2026-10-20

| 코드 | 상황·대응 원칙 |
| --- | --- |
| INACTIVE_MASTER | 새 주문/입고에서 사용 중인 도서·거래처를 선택하도록 안내 |
| INSUFFICIENT_STOCK | 전체 출고/조정은 반영하지 않고 현재 재고 확인 안내 |
| QUANTITY_LIMIT, AMOUNT_LIMIT | 수량/금액 한도 확인 안내. 값의 임의 절삭·반올림 금지 |
| ORDER_ITEMS_INVALID | 빈 품목·중복 도서·다른 주문 반품 품목을 구분한 일반 안내 |
| ORDER_STATE | 현재 상태에서 가능한 작업 확인. 출고 완료 직접 취소 금지 |
| RETURN_LIMIT | 미반품 수량 안에서 1권 이상 입력 안내 |
| DUPLICATE_OPERATION | 이미 처리된 요청은 내역 확인 후 새 작업으로 시작. 자동 재전송 금지 |
| VALIDATION_FAILED | 수량·사유·날짜 제약을 한국어로 표시, 가능한 같은 폼 보존 |

새 주문/입고/반품 폼은 검증·업무 오류에서 입력을 유지한다. 오류 코드와 추적 ID는 기존 공통 컴포넌트를 사용한다. 주문 메모·반품 사유·명칭은 JSP에서 이스케이프한다. 실제 HTTP 테스트에서 script 입력이 실행 태그가 아닌 글자로 출력되고 수량 초과 반품이 저장되지 않음을 검증했다. 보고서의 잘못된 기간은 입력 날짜를 유지한 같은 검색 폼에 HTTP 400으로 안내한다.
