# 학습 진행 상태

## 학습 원칙

PHP 웹 개발 경험을 기준으로 Java·Spring Boot·JSP를 설명한다. 상세 필수 규칙은 `AGENTS.md`를 따른다. 구현 완료, 설명 제공, 직접 수정, 이해 확인을 각각 구분한다.

## 주제 1 — 기존 홈 화면과 DB 상태 확인

- 상태: JSP 선택·JSON 응답·DB 실패 흐름 설명과 이해 확인 완료. 직접 JSP 수정 완료. 수정 반영 문제와 개발/운영 실행 차이를 논의했고, 사용자가 다음 학습 진행을 요청했다.
- 새 기능 구현: 없음. 기존 Phase 0 코드를 학습한다.
- 학습 목표: URL이 컨트롤러 메서드로 연결되고 HTML 또는 JSON 응답이 만들어지는 경로를 설명한다.
- 주요 파일: `src/main/java/com/portfolio/erp/common/HomeController.java`, `src/main/resources/application.yml`, `src/main/webapp/WEB-INF/jsp/home.jsp`. 앱 시작점은 `ErpApplication.java`다.

### 요청 흐름

- GET `/`: Spring MVC → `HomeController.home()` → 뷰 이름 `home` → 설정된 prefix/suffix 적용 → `/WEB-INF/jsp/home.jsp` → 서버에서 HTML 렌더 → 브라우저. 이 메서드는 DB를 조회하지 않는다. 앱 시작 과정의 DB 연결·Flyway 동작과는 구분한다.
- GET `/health`: Spring MVC → `HomeController.health()` → Spring이 생성자에 전달한 `JdbcTemplate` → 데이터소스 연결 → MariaDB `SELECT 1` → Java Map → JSON 응답. 이 상태 확인 요청은 MyBatis 매퍼를 거치지 않는다.

### 코드의 이유와 PHP 비교

- `@Controller`: URL 요청을 처리하고 화면을 선택할 클래스로 Spring에 등록한다. PHP에서는 라우터가 호출할 컨트롤러에 해당한다.
- `@GetMapping`: PHP 라우터의 GET 경로 등록과 같은 역할이다.
- `return "home"`: 응답 본문이 아니라 뷰 이름이다. PHP의 `require`로 템플릿을 렌더하는 흐름과 비교할 수 있으나 Java 코드에서 JSP를 직접 include하는 것은 아니다.
- 생성자 `HomeController(JdbcTemplate jdbc)`: Spring이 준비한 DB 도구를 전달받아 사용한다. PHP에서 컨트롤러 생성자에 PDO를 전달하는 방식과 비교할 수 있다. JdbcTemplate과 PDO의 API 자체가 같다는 뜻은 아니다.
- `SELECT 1`: 테이블 데이터가 필요 없는 간단한 쿼리로 실제 DB 요청이 가능한지 확인한다.
- `@ResponseBody`: JSP 선택 대신 반환값을 HTTP 응답 본문으로 변환한다. 여기서는 Map을 JSON으로 직렬화하며 PHP의 `json_encode` 응답과 비교할 수 있다.
- DB 쿼리가 실패하면 `UP` Map 반환 전에 예외가 발생한다. DB 연결 실패를 `UP`으로 처리하지 않는다.

### 직접 수정 과제 — 1개

홈 JSP의 안내 문구 `도서와 거래처를 관리하는 출판 업무 시연 시스템`을 본인이 정한 문구로 바꾸고 홈 화면에서 확인한다. 코드 위치 찾기와 화면 확인을 직접 해본다. Codex가 대신 수정하지 않는다. 개발 실행 중 변경이 안 보이면 앱을 재시작하고 새로고침한다. WAR 실행 중이라면 소스 수정만으로 기존 WAR가 바뀌지 않으므로 다시 빌드해야 한다.

### 이해도 확인 질문 — 3개

1. `home()`의 `return "home"`이 브라우저에 문자열 home을 그대로 출력하지 않는 이유는 무엇인가?
2. `/health`의 Map이 JSP 대신 JSON으로 응답되는 데 관여하는 애너테이션은 무엇인가?
3. `/` 요청과 `/health` 요청 중 어느 컨트롤러 메서드가 DB 쿼리를 실행하며, `SELECT 1`이 실패하면 `UP` 응답을 반환할 수 있는가?

## 2026-10-18 — JSP 수정이 반영되지 않는 문제

- 개발자가 홈 안내 문구 끝에 `1`을 붙여 저장했다. 사용자 수정은 그대로 보존했다.
- 원인: 앱은 `java -jar target/publishing-erp-mini-0.1.0-SNAPSHOT.war --spring.profiles.active=demo`로 실행 중이었다. WAR 내부 JSP에는 변경 문구가 없었고 실제 HTTP 응답도 이전 문구였다.
- PHP 비교: 편집한 PHP 템플릿과 서버가 사용하는 배포 사본이 다른 상황과 비슷하다. 저장만으로 배포 사본은 바뀌지 않는다.
- 해결: 해당 WAR 앱만 종료하고 `./scripts/run.sh -Dspring-boot.run.profiles=demo`로 소스 기반 개발 실행에 전환했다. JSP나 DB 데이터는 수정하지 않았다.
- 실행 흐름: 개발 앱 → HomeController → 소스 폴더의 JSP → HTML 응답. 홈 컨트롤러 요청은 DB를 조회하지 않는다.
- 검증: GET `/` HTTP 200 및 `시연 시스템1` 일치, `/health` UP. 열려 있던 브라우저를 새로고침해 변경 문구가 표시됨을 확인했다. 업무 로직 변경이 없어 전체 테스트는 재실행하지 않았다.
- 이번 실행 로그: `.local/dev-app.log`. 화면 증거: `.local/jsp-change-confirmed.png` (Git 제외).
- 남은 확인: 소스 수정과 WAR 재빌드의 차이를 이해했는지 확인한다. 다음 기능은 시작하지 않는다.

## 주제 2 — 기존 도서 목록 조회

- 진행 동의: 사용자가 “그래. 계속 가볼까?”라고 요청(2026-10-18).
- 설명 및 이해 확인 경과: `/books`와 메서드 연결, Model의 result 이름, query 검색 조건·기본 페이지, @Valid/@Min 검증, Service의 목록/전체 개수 역할을 설명했다. 사용자가 조건·기본값·목록/전체 개수를 구분해 답변했다.
- 현재 상태: Mapper 연결, 페이지 범위, 검색 패턴·조건부 SQL, 결과 객체, JSP 반복 출력까지 기본 흐름 설명 완료. 각 소단계 답변, 직접 수정 과제, 최종 복습 3개 모두 확인하여 기본 흐름 학습 완료.
- 사용자 선호: 완성 코드를 먼저 준비하고 따라가며 배우는 방식 허용. Java 문법/프레임워크 제공 기능/프로젝트 직접 작성 코드를 구분해 설명한다.
- 코드 변경: 없음. 기존 코드를 읽고 설명한다.
- 주요 파일: `src/main/java/com/portfolio/erp/book/BookController.java`, `BookService.java`, `BookMapper.java`, `src/main/resources/mapper/BookMapper.xml`.
- 새 개념: 클래스의 `@RequestMapping("/books")`는 공통 주소, 메서드의 경로 없는 `@GetMapping`은 그 주소의 GET 요청을 받는다.
- 기존 개념 연결: `return "book/list"`는 `/WEB-INF/jsp/book/list.jsp`를 선택한다.
- 설명한 기본 흐름: namespace/id 연결 → LIMIT/OFFSET → LIKE 검색 패턴 → 검색어가 없을 때 조건 생략 → resultType으로 Book 객체 목록 반환 → PageResult에 목록·개수 묶음 → Model의 result → JSP c:forEach/c:out 출력. 특수문자 이스케이프·자동 속성 매핑 등 세부 주제는 필요 시 별도로 설명한다.
- 학습 예제 PHP 비교: 조건을 받은 함수 → PDO SELECT/fetchAll → 결과 배열 → 템플릿 foreach 출력. Java에서는 역할별 객체로 나누고 MyBatis와 JSP 태그가 연결·출력을 돕는다.

### 도서 목록 직접 수정 과제 — 1개

`src/main/webapp/WEB-INF/jsp/book/list.jsp`의 `c:forEach` 안에서 도서 제목을 표시하는 `strong`에 `제목: ` 문구를 앞에 붙인다. 저장 후 `/books`를 새로고침해 모든 도서 행에 문구가 반복되는지 확인한다. 사용자 요청 전에는 대신 수정하지 않는다.

### 도서 목록 복습 질문 — 3개

1. `/books` 요청을 받고 JSP에 데이터를 전달하는 것은 Controller인가, XML SELECT인가?
2. SELECT문 자체가 작성된 파일은 BookMapper.java인가, BookMapper.xml인가?
3. JSP의 c:forEach는 DB를 다시 조회하는가, 전달받은 목록을 반복 출력하는가?

- 다음 주제는 과제·복습 또는 사용자의 명시적인 이해 확인 이후 정한다. 새 기능은 아직 추가하지 않는다.

## 후속 운영 학습 — 사용자 실제 사례로 설명

- 매일 새 이벤트 페이지 디자인 배포, 홈 화면 긴급 수정, 기능/SQL 변경을 각각 구분해 실제 반영 절차를 설명한다.
- 데이터 변경, 독립적인 화면 배포, 앱 배포를 구분하고 단일 앱 재시작과 중단을 줄이는 앱 전환을 연결해 설명한다.
- 외부 HTML과 외부 JSP를 동일하게 취급하지 않는다. 현재 프로젝트에는 외부 JSP 운영 구성이 없다. WAR 사용 자체가 변경이 적은 서비스를 뜻하지 않는다.
- 배포 단계에서 사례를 실제 구성과 연결해 설명한다. 지금 운영 구성이나 새 기능을 구현하지 않는다.

## 2026-10-18 — 도서 JSP 변경 반영 재진단

- WAR가 아닌 소스 기반 개발 앱에서도 Spring Boot의 JSP development 기본값 false로 인해 최초 컴파일 결과를 계속 사용하는 문제를 확인했다. 이전에 개발 실행 전환만으로 충분하다고 안내했던 내용을 정정했다.
- 실제 응답에는 이전 편집에서 시작 `<`가 빠진 `제목:c:out ...`가 남아 있었고, 진단 시 디스크의 사용자 JSP는 제목 출력 원래 코드와 마지막 공백 변경만 저장된 상태였다. 과제 완료로 표시하지 않았다.
- local/demo 프로필에 JSP development=true, modificationTestInterval=0, recompileOnFail=true와 Tomcat resource.allow-caching=false를 추가했다. 기본 리소스 캐시 5초가 변경 감지를 지연하는 것도 임시 JSP로 확인했다.
- 앱 설정 반영을 위해 개발 앱만 재시작했다. 사용자 JSP와 DB 데이터는 변경하지 않았다.
- 최종 검증: 임시 JSP를 A로 요청한 후 B로 바꾸고 앱 재시작 없이 다음 요청에서 B 확인. 임시 파일은 삭제했다. `/books` HTTP 200에서 깨진 태그가 사라졌고 `/health` UP 확인. 전체 앱 테스트는 재실행하지 않았다.
- 새 로그: `.local/dev-app-reload.log`. 다음으로 사용자에게 올바른 제목 출력 코드와 저장(Cmd+S) 후 새로고침을 안내한다.

### 도서 목록 수정 과제 확인

- 개발자가 직접 저장한 `<strong>제목:<c:out value="${row.title}"/></strong>`을 확인했다.
- 실제 `/books` HTTP 200 응답의 도서 3행 모두 `제목:`이 표시되어 과제를 완료로 기록했다. Codex는 사용자 JSP를 수정하지 않았다.
- 이전 미반영은 저장 실패가 아니라 JSP 변경 감지 비활성화로 이전 컴파일 결과를 계속 사용한 문제였다. 최종 복습 질문에 대한 답변 확인은 남아 있다.

### 도서 목록 최종 복습 완료

- 답변 확인: 요청·화면 데이터 전달은 Controller, 실제 SELECT는 BookMapper.xml, c:forEach는 전달받은 목록을 반복 출력.
- 직접 수정 과제와 함께 도서 목록 기본 조회 흐름 학습을 완료로 기록했다.
- 다음 후보는 기존 도서 등록 폼 GET `/books/new`이며 아직 시작하지 않았다. 등록 저장·검증·트랜잭션 전체를 한 번에 설명하지 않는다.

## 주제 3 — 기존 도서 등록 기본 흐름

- 상태: 기본 흐름, GET/POST 오답 재설명, 롤백 상황 문답과 리다이렉트 이해 확인, 라벨 직접 수정·HTTP 반영 확인 완료. 신규 구현은 없다.
- 설명한 내용: GET `/books/new`의 빈 BookForm과 JSP 표시, form 태그의 modelAttribute/path, POST `/books`의 create 연결, 입력 바인딩과 @Valid/BindingResult, 입력값 보존 및 form:errors 출력.
- 저장 흐름: Book.from(form)으로 Book 준비 → Mapper/XML INSERT 바인딩 → 생성 ID를 entity.id에 수신 → 같은 ID의 초기 재고 0행 생성. @Transactional로 도서·재고를 묶고 DB 실패 시 롤백한다.
- 오류/성공: ISBN 중복은 DuplicateKeyException을 잡아 필드 오류로 표시한다. 성공 시 flash notice와 redirect:/books → GET 목록 조회로 이동한다. 목록 새로고침은 POST 저장이 아니다.
- PHP 비교: $_POST 바인딩, 입력 검증과 오류 배열, PDO prepare/execute·lastInsertId·beginTransaction/commit/rollBack, 세션 알림과 Location 리다이렉트.
- 관련 파일: BookController.java(요청·오류·화면 전환), BookForm.java(입력·검증), Book.java(저장 객체·복사), BookService.java(도서·재고 트랜잭션), BookMapper.java/BookMapper.xml(메서드와 SQL), book/form.jsp(입력·오류 표시).

### 도서 등록 직접 수정 과제 — 1개

`src/main/webapp/WEB-INF/jsp/book/form.jsp`의 `<form:label path="title" ...>` 안 문구를 `도서명`에서 `도서명 (필수)`로 바꾸고 저장한 뒤 `/books/new`에서 확인한다. path·검증 규칙은 바꾸지 않는다. 등록·수정 공용 폼이므로 수정 화면 라벨도 함께 바뀐다. 요청 전에는 과제를 대신 수행하지 않는다.

### 도서 등록 복습 질문 — 3개

1. 등록 화면을 여는 요청과 저장 요청은 각각 GET/POST 중 무엇인가?
2. 도서 저장 후 재고 행 생성이 실패했을 때 두 작업을 함께 취소하게 하는 애너테이션은 무엇인가?
3. 성공 후 `redirect:/books`를 반환하면 JSP를 바로 그리는가, 브라우저가 목록으로 새 요청을 보내는가?

- 과제·복습 확인 전에는 다음 기능을 시작하지 않는다. 실제 CSRF 구현·검증 상세 규칙 등은 필요 시 별도 소단계로 설명한다.

### 등록 과제·복습 완료 확인

- 사용자가 직접 저장한 라벨은 `도서명(필수)`였고 `/books/new` HTTP 200 응답의 실제 label에 그대로 표시됐다. 예시와의 공백 차이는 과제 목적에 영향을 주지 않는다. 사용자 JSP는 수정하지 않았다.
- 복습은 한 문제씩 진행했다. 화면 열기 GET/저장 POST를 재설명했고 사용자가 GET이라고 정정했다. 트랜잭션은 저장 두 작업 중 실패하면 앞 작업도 취소하는 상황으로 재확인했다. 성공 후 목록 새 요청·새로고침은 조회만 하는 것도 확인했다.

## 주제 4 — 기존 도서 수정 기본 흐름

- 상태: 수정 기본 흐름 설명, 직접 링크 문구 수정·HTTP 반영, 복습 확인 완료. @PathVariable 이름은 재설명했고 번호와 대상 연결은 이해했다.
- GET `/books/{id}/edit`: @PathVariable로 id를 받고 service.find(id) 결과를 form, 번호를 entityId로 전달해 기존 book/form.jsp를 사용한다.
- POST `/books/{id}`: URL은 대상 id, BookForm은 변경할 내용이다. service.update에서 Book.from(form), entity.setId(id), mapper.update(entity)를 실행한다.
- SQL은 UPDATE books SET ... WHERE id=#{id}이며 번호로 대상을 제한한다. 대상이 없어 UPDATE 결과가 0이면 NotFoundException을 발생시킨다. GET 조회도 대상이 없으면 같은 예외다. 프로젝트의 공통 처리기가 404 화면으로 변환한다.
- 입력 검증과 ISBN 중복 처리는 등록과 같은 구조다. 정상 수정 후 flash notice와 redirect:/books로 목록 GET을 요청하게 한다.
- PHP 비교: 라우터 id와 POST 입력을 함께 받아 PDO UPDATE의 WHERE id에 바인딩하고, 없는 대상은 404 처리, 성공은 Location 리다이렉트로 목록을 요청하는 흐름.
- 관련 파일: BookController.java(수정 GET/POST), BookService.java(조회·수정·트랜잭션), BookMapper.xml(SELECT/UPDATE), book/form.jsp(등록·수정 공용 폼), book/list.jsp(수정 링크).
- 앱 코드 변경 없음. 기존 메서드와 JSP를 대조해 설명한다.

### 도서 수정 직접 수정 과제 — 1개

`src/main/webapp/WEB-INF/jsp/book/list.jsp`에서 `/books/${row.id}/edit`로 연결되는 링크의 표시 문구를 `수정`에서 `정보 수정`으로 바꾼다. href는 유지한다. 저장·새로고침 후 해당 링크를 눌러 기존 값이 채워진 수정 화면이 열리는지 확인한다. DB 값 변경은 과제에 포함하지 않는다. 요청 전에는 대신 수정하지 않는다.

### 도서 수정 복습 질문 — 3개

1. `/books/25/edit`의 25를 Java 변수로 받는 애너테이션은 무엇인가?
2. UPDATE에서 `WHERE id=#{id}`는 바꿀 내용과 바꿀 대상 중 무엇을 지정하는가?
3. 등록과 수정은 입력 JSP를 따로 사용하는가, form에 넣는 데이터를 달리해 같은 JSP를 사용하는가?

- 과제·복습 및 이해 확인 전에는 다음 기능을 시작하지 않는다.

### 도서 수정 과제·복습 확인

- 사용자가 변경한 `정보 수정` 링크가 `/books` HTTP 200 응답의 세 행 모두에 표시됐다. 주소는 `/books/{id}/edit`로 보존됐다.
- GET `/books/3/edit` HTTP 200과 기존 도서명 `시연 도서: 절판 자료`가 입력란에 표시되는 것을 확인했다. DB 값을 변경하지 않았다.
- 복습 답변: WHERE는 바꿀 대상, 등록·수정은 같은 form.jsp 사용. URL 번호/id 변수에 대한 이해는 확인했고 애너테이션 용어와 @PathVariable 이름을 보완 설명했다.
- 사용자 JSP는 수정하지 않았다. 새 기능은 추가하지 않고 기본 수정 학습 완료로 기록했다.

## 주제 5 — 기존 도서 비활성화

- 학습 방식 변경: 사용자가 진행 속도를 높여 계속하길 요청했다. 기능 하나를 연결해 설명하고 중간 단순 질문을 줄이며 마지막 복습 3개로 확인한다. 새 기능 자동 개발은 하지 않는다.
- 상태: 전체 흐름 설명, UPDATE·데이터 보존 문답, 확인 문구 저장·HTTP 반영 확인 완료. 사용 상태 선택란의 세 값과 조회 조건임을 보완 설명한 후 사용자가 다음 학습 진행에 동의했다.
- 흐름: 활성 도서의 버튼 → jQuery 확인창 → 승인 시 POST `/books/{id}/deactivate`(세션 CSRF 토큰 포함) → Controller → BookService.deactivate(id) → MyBatis `<update id="deactivate">` → books.active=FALSE 및 updated_at 갱신 → flash notice/redirect → 목록 GET.
- 삭제 대신 상태 변경 이유: 도서·재고 데이터를 보존하고 향후 주문 이력 등의 참조를 유지하기 위해서다. 주문 기능은 아직 구현되지 않았다.
- 목록 전체 필터에서는 비활성 행을 유지하고 비활성 표시, 사용 필터에서는 제외, 비활성 필터에서는 조회된다. 비활성 행에는 비활성화 버튼을 숨긴다. 정보 수정 화면의 사용 상태를 바꿔 다시 활성화할 수 있다.
- 대상이 없으면 NotFoundException→404, CSRF 토큰이 없거나 다르면 403이다. 전체 보안 설명은 별도 학습으로 남긴다.
- PHP 비교: POST id 받기 → PDO UPDATE active=0 WHERE id=:id → 알림/Location 목록 이동. 문법·프레임워크 기능·프로젝트 설계 선택을 구분한다.
- 관련 파일: BookController.java(POST·리다이렉트), BookService.java(트랜잭션·없는 대상 검사), BookMapper.xml(UPDATE와 필터), book/list.jsp(버튼·상태), static/js/app.js(data-confirm 확인창).

### 비활성화 직접 수정 과제 — 1개

`src/main/webapp/WEB-INF/jsp/book/list.jsp`의 비활성화 폼 `data-confirm` 문구를 `판매를 중단하시겠습니까? 도서 정보는 유지됩니다.`로 바꾼다. 저장·새로고침 후 사용 중인 도서의 비활성화 버튼을 눌러 새 문구를 확인하고 **취소**한다. 문구 확인이 목적이며 DB 상태는 변경하지 않는다. 요청 전에는 대신 수행하지 않는다.

### 비활성화 복습 질문 — 3개

1. 비활성화의 SQL은 DELETE인가, UPDATE인가?
2. active=FALSE가 되면 도서 행과 재고 정보가 DB에서 사라지는가?
3. 사용 상태 필터가 전체인 경우 비활성화된 도서는 목록에 남는가? 사용 필터에서는 어떻게 되는가?

### 비활성화 확인 문구 수정 검증

- 사용자 저장 문구 `판매를 중단하시겠습니까? 도서 정보는 유지됩니다.`를 확인했다.
- 8080 응답이 없어 프로젝트 전용 DB와 demo 개발 앱을 다시 실행했다. 기존 데이터는 초기화하지 않았다. 로그는 `.local/confirmation-check-app.log`다.
- GET `/books` HTTP 200 응답에서 비활성화 폼 2개 모두 새 data-confirm 문구가 반영됐다. 화면 확인창 조작이나 POST 요청은 수행하지 않았다.
- 복습 답변은 UPDATE, 데이터 보존까지 확인했다. 3번의 `비활성` 답변에는 전체 필터에서 비활성 표시, 사용 필터에서 제외라는 구분을 보완 설명했다. 해당 구분 최종 확인은 남아 있다.
- 사용자 JSP는 변경하지 않았다.

## 주제 6 — 기존 거래처 등록

- 범위: 신규 개발 없이 기존 등록 기능을 설명한다. 도서와 같은 계층·POST/검증/리다이렉트는 짧게 연결하고 거래처 코드·유형 차이에 집중한다.
- 상태: 복습 3개 정답 및 사용자 placeholder 수정 과제 검증 완료 (2026-10-20).
- 흐름: GET `/partners/new` → 빈 PartnerForm → partner/form.jsp → POST `/partners` → @Valid → PartnerService.create → PartnerMapper.insert → partners INSERT → 성공 후 GET `/partners`.
- 코드: code는 필수, 대문자/숫자/_/- 1~40자, DB UNIQUE. 중복 시 DuplicateKeyException을 code 필드 오류로 표시한다. 자동 생성 id와 업무용 code는 별개다.
- 유형: 화면 서점은 BOOKSTORE, 유통사는 DISTRIBUTOR를 보낸다. PartnerForm의 String type에 @NotNull/@Pattern으로 두 값만 허용한다. 프레임워크가 정한 업무 값이 아니라 프로젝트에서 정한 값이다.
- 도서 등록과 달리 초기 재고 행을 생성하지 않고 partners 행을 저장한다. SQL 값은 MyBatis #{} 바인딩을 사용한다.
- PHP 비교: $_POST 입력 검증 → PDO INSERT → 고유 키 예외 처리 → Location 목록 이동.
- 관련 파일: PartnerController.java, PartnerForm.java, PartnerService.java, PartnerMapper.xml, partner/form.jsp.
- 실제 검증: GET `/partners`, `/partners/new` 모두 HTTP 200과 한국어 제목 확인. 앱 소스/DB는 변경하지 않았고 전체 테스트는 재실행하지 않았다.

### 거래처 등록 직접 수정 과제 — 1개

`src/main/webapp/WEB-INF/jsp/partner/form.jsp`의 code 입력 태그에 `placeholder="STORE_01"`을 추가하고 저장한다. 새 등록 폼 `/partners/new`를 새로고침해 코드 입력란에 예시가 보이는지 확인한다. 기본값(value)을 지정하는 것이 아니며 DB 등록은 과제에 포함하지 않는다. 요청 전에는 대신 수정하지 않는다.

### 거래처 등록 복습 질문 — 3개

1. 이미 등록된 거래처 코드로 이름만 다른 거래처를 새로 등록할 수 있는가?
2. 화면에서 서점을 선택하면 서버에 `서점`과 `BOOKSTORE` 중 어떤 값이 전달되는가?
3. 거래처를 등록할 때 도서 초기 재고 행도 함께 만드는가?

### 거래처 등록 과제·복습 완료 — 2026-10-20

- 복습 답변: 같은 코드 등록 불가, 서점은 BOOKSTORE, 초기 재고는 만들지 않음. 세 답변 모두 확인했다.
- 사용자가 저장한 code 입력 태그의 placeholder="STORE_01"을 확인했다. GET `/partners/new` HTTP 200에서 해당 placeholder와 빈 value를 확인했다.
- 사용자 JSP를 대신 수정하거나 DB 등록 요청을 보내지 않았다. 전체 테스트는 재실행하지 않았다. 다음 기능은 아직 시작하지 않았다.

## 주제 7 — 기존 거래처 수정

- 사용자의 계속 진행 요청에 따라 기존 수정 기능 하나를 학습한다. 비활성화는 이후로 남긴다. 새 앱 구현은 없다.
- 흐름: GET /partners/{id}/edit → service.find(id) → form/ entityId → 공용 partner/form.jsp → POST /partners/{id} → 검증 → service.update → MyBatis UPDATE partners WHERE id → redirect:/partners.
- id는 변경 대상이며 code는 수정 가능한 업무 값이다. 자기 코드를 그대로 유지할 수 있지만 다른 행의 코드와 중복되면 필드 오류로 표시한다. 없는 대상은 404다.
- PHP 비교: 라우터 id + POST 입력 → PDO UPDATE/WHERE → 고유 키 예외 처리 → Location 리다이렉트.
- 검증: GET /partners 및 /partners/3/edit 모두 HTTP 200. 기존 code=DEMO_INACTIVE, name=시연 휴업 책방 및 form action=/partners/3, method=post 확인. DB 쓰기 및 전체 테스트 재실행 없음.
- 과제 1개: partner/list.jsp의 수정 링크 표시를 거래처 수정으로 변경. href 유지. 수정 화면 열기까지만 확인한다. 과제를 대신 수행하지 않는다.
- 복습 3개: 수정 대상은 id/code 중 무엇인가? 자기 코드 유지 가능한가? 다른 거래처와 코드 중복 시 저장되는가/입력 오류인가?
- 상태: 사용자 복습 3개 정답 및 직접 수정 과제 검증 완료 (2026-10-20).

### 거래처 수정 과제·복습 완료 — 2026-10-20

- 답변: 대상은 id, 자기 코드 유지 가능, 다른 거래처와 코드 중복 시 오류. 모두 정답이다.
- 저장된 partner/list.jsp와 GET /partners HTTP 200에서 거래처 수정 문구의 링크 3개를 확인했다. 기존 href가 유지됐다.
- 링크의 수정 화면 GET HTTP 200 및 기존 거래처명 입력값을 확인했다. 사용자 JSP는 수정하지 않았으며 DB 쓰기·전체 테스트 재실행은 없다.

## 주제 8 — 기존 거래처 비활성화

- 범위: 기존 기능 학습. 새 앱 구현 없음. 도서 비활성화와 같은 흐름을 거래처에 연결한다.
- 흐름: JSP 폼 → jQuery confirm → 확인 시 POST /partners/{id}/deactivate(CSRF 포함) → Controller → Service @Transactional → Mapper UPDATE partners SET active=FALSE,updated_at=... WHERE id → 목록 리다이렉트.
- 취소 시 preventDefault로 POST를 막는다. 상태 변경은 행 삭제가 아니며 이름·코드를 보존한다. 현재는 사용 상태와 조회 필터만 학습하며 미구현 주문 기능에 대한 차단 효과를 가정하지 않는다.
- SQL 결과 0이면 NotFoundException으로 404 처리한다. 비활성 거래처는 수정 화면 사용 상태를 사용으로 저장해 다시 활성화할 수 있다.
- PHP 비교: 확인창 승인 후 POST id → PDO UPDATE active=0 WHERE id=:id → Location 목록 이동.
- 검증: GET /partners 전체 HTTP 200 3행/비활성화 폼 2개, status=true 200 2행/폼 2개, status=false 200 1행/폼 0개. POST·브라우저 확인창 조작 및 전체 테스트 재실행 없음.
- 과제 1개: partner/list.jsp의 data-confirm을 거래처를 비활성화하시겠습니까? 정보는 보존됩니다. 로 수정하고 저장·새로고침 후 확인창을 열어 취소. 대신 수정하지 않는다.
- 복습 3개: 확인창 취소 시 POST 여부, 전체 필터에서 비활성 거래처 표시 여부, 사용 필터에서 비활성 거래처 표시 여부.
- 상태: 복습 3개 정답 및 사용자 문구 수정 과제 검증 완료 (2026-10-20).

### 거래처 비활성화 과제·복습 완료 — 2026-10-20

- 답변: 취소 시 POST 없음, 전체 필터에서는 비활성 포함, 사용 필터에서는 제외. 모두 정답이다.
- 저장된 JSP와 GET /partners HTTP 200 응답의 폼 2개에서 새 확인 문구를 확인했다. 브라우저 확인창 조작 및 POST는 수행하지 않았다.
- 최초 8080 연결 거부 후 개발 앱 재실행에서도 DB 3307 연결 거부가 발생했다. DB와 앱을 같은 실행 세션에서 시작한 뒤 HTTP 검증이 성공했다. 로그 .local/partner-confirm-app.log. 데이터 초기화 및 사용자 JSP 수정 없음. 전체 테스트는 재실행하지 않았다.

## 주제 9 — 기존 거래처 검색

- 사용자 진행 동의 후 기존 기능 학습. 앱 코드 변경 없음.
- 흐름: GET /partners?q=...&status=... → PageQuery → Controller/Service → Mapper findAll 및 count의 공통 filters → DB SELECT → result.items → JSP.
- OR는 거래처명/코드/담당자 중 하나, AND active는 선택 상태도 만족하는 조건. OR 묶음 괄호로 상태 조건이 모든 검색 대상에 적용되도록 한다.
- PageQuery.getPattern이 부분 검색 패턴을 만들며 특수문자는 !로 이스케이프한다. MyBatis #{}는 PDO 바인딩과 비교한다. 검색어 빈 값은 검색어 조건을 생략한다. 목록/개수가 같은 filters를 사용한다.
- 검증: 휴업+전체 1행, 휴업+사용 0행, 휴업+비활성 1행, NO_MATCH_LESSON_20261020+전체 0행과 결과 없음 표시. 모두 HTTP 200. DB 쓰기·전체 테스트 재실행 없음.
- 과제 1개: partner/list.jsp의 검색 결과가 없습니다. 를 조건에 맞는 거래처가 없습니다. 로 바꾸고 존재하지 않는 검색어로 확인. 대신 수정하지 않는다.
- 복습 3개: 담당자만 검색어 일치해도 조회되는가? 비활성이면서 검색어 일치하면 사용 필터에서 조회되는가? 검색은 SELECT/UPDATE 중 무엇인가?
- 상태: 복습 3개 정답과 사용자 수정 과제 검증 완료 (2026-10-20).

### 거래처 검색 과제·복습 완료 — 2026-10-20

- 답변: 담당자만 일치해도 조회됨, 사용 필터에서는 비활성 제외, 검색 SQL은 SELECT. 모두 정답이다.
- 사용자 JSP의 조건에 맞는 거래처가 없습니다. 문구와 일치하지 않는 검색어의 HTTP 200 응답(0행)에서 동일 문구를 확인했다.
- 최초 8080 연결 거부로 프로젝트 DB·개발 앱을 재실행했다. 로그 .local/partner-search-app.log. 사용자 앱 코드·DB 데이터 변경 및 전체 테스트 재실행 없음.

## 주제 10 — 사용자 친화 오류 시스템

- 사용자가 요청한 15개 오류 요구사항을 하나의 공통 기능 확장으로 구현했다. 신규 업무 단계나 로그인 기능을 추가하지 않았다.
- 흐름: TraceIdFilter → 요청/검증/Service 예외 → UserErrors/GlobalExceptionHandler → 메시지 번들 → ErrorNotice → 같은 폼 또는 공통 JSP/JSON. 컨테이너 오류는 SafeErrorController가 처리한다.
- 기술 예외 본문은 출력/로그하지 않고, 안전한 코드·종류·위치·추적 ID만 기록한다. 폼 입력은 요청 모델로 유지하며 CSRF/만료 요청과 향후 비밀번호 입력은 복원하지 않는다.
- PHP 비교: 공통 catch/메시지 배열/폼 재출력/로그 식별자를 Filter·ControllerAdvice·MessageSource·BindingResult로 나눈 구조다.
- 최종 검증: ./scripts/db-start.sh && ./scripts/verify.sh, 51개 통과(신규 MockMvc 21개 포함), WAR 빌드 성공. 실제 JSP의 오류 입력/검색 XSS escaping·404와 개발 앱 HTML/JSON 404 확인.
- 테스트 중 Mockito 동적 연결 정지 → 명시적 javaagent 설정, Mock 재설정 예외 → doThrow로 교정 후 전체 통과.
- 과제 1개: 메시지 번들 두 파일의 ui.error.title을 처리 안내로 변경하고 앱 재시작 후 없는 도서의 수정 주소에서 확인. 과제는 대신 수행하지 않았다.
- 복습 3개: 사용자 문구의 출처(예외 본문/한국어 파일), 문의에 전달할 것(비밀번호/추적 ID), 입력 유지 방식(redirect/같은 폼).
- 상태: 구현·검증·복습 3개 정답·사용자 문구 수정 과제 확인 완료 (2026-10-20). 다음 주제는 아직 시작하지 않았다.

### 메시지 파일 과제 반영 진단 — 2026-10-20

- 사용자가 두 원본 번들의 ui.error.title을 처리 안내로 저장했고 복습 3개 모두 정답이었다.
- 원본 수정 시각은 20:30:24, target/classes 사본은 20:25:45였으며 실행용 사본과 실제 오류 화면은 안내를 표시했다. 원본 저장은 정상이고 실행 리소스 복사·앱 재시작이 아직 반영되지 않은 상태였다.
- 프로젝트 cwd와 PID를 확인해 해당 개발 앱만 종료하고 db-start.sh 및 run.sh -Dspring-boot.run.profiles=demo로 재실행했다.
- 원본을 대신 수정하지 않았다. 두 target/classes 사본과 HTTP 404의 error-title에서 처리 안내 반영 확인. 로그 .local/message-refresh-app.log. 새 코드 변경이 없어 전체 테스트는 재실행하지 않았다.

## 주제 11 — 안전한 로그인·세션·접근 통제 (이전 구현 기록)

- 사용자 명시 요청으로 보안 시스템을 하나의 기능 확장으로 구현했다. 작은 함수별 문답으로 중단하지 않고 전체 흐름과 마지막 과제/복습으로 설명한다.
- 흐름: 로그인 JSP CSRF → 시도 제한 → Security 인증 필터 → MyBatis 인증 조회/DelegatingPasswordEncoder → ID 교체·기존 세션 폐기 → 보안 컨텍스트 저장 → 요청마다 시간·계정 상태 검사 → HTTP/Service/직원 객체 권한 → 기존 업무 Mapper/DB/JSP.
- PHP 비교: password_verify/session_regenerate_id/공통 세션 검사에 해당하며 프레임워크의 필터·메서드 보안으로 역할을 나눈다. 도서/거래처는 공동 기준정보이며 직원 프로필은 본인/ADMIN으로 제한한다.
- 검증: 전체 74개·WAR 성공, 실제 쿠키/HTTP로그인·중복/동시로그인·CSRF·시간/차단/비밀번호 폐기·Service/객체 검사, 실제 개발 앱 로그인/로그아웃·로그 비노출. actionlint 통과. 원격 CI/Trivy 미실행.
- 발견/수정: Mock 세션 숫자와 타임스탬프의 로그 검사 충돌, 예전 중복 로그인 허용 전제를 새 정책에 맞춤, 홈의 공통 헤더/새 CSRF 토큰 연결, HTTPS Mock 요청의 실제 scheme 설정. 사용자 시연 시스템1 및 기존 JSP 수정 문구를 보존했다.
- 과제 1개: auth/login.jsp의 버튼 표시 문구만 로그인하기로 수정해 저장·새로고침 확인. 대신 수행하지 않았다.
- 복습 3개: DB 저장은 원문/해시 중 무엇인가, 계속 활동해도 8시간 뒤 유지되는가, 타 직원 URL 접근은 Service도 차단하는가.
- 상태: 구현·실제 검증 완료. 사용자 수정 과제와 이해 확인 대기. 다음 기능은 시작하지 않는다.

## 현재 상태 — Phase 2~4 연속 구현 승인 (2026-10-20)

사용자가 `phase4 까지 작업`을 명시 요청했다. 이번 범위는 중간 이해 확인을 기다리지 않고 구현·검증했으며 Docker 실행/호스팅은 보류했다. 기존 로그인 과제/복습 답변은 아직 받지 않았다. 구현 완료를 이해 완료로 처리하지 않는다.

새 주문·출고, 재고·반품, 보고서의 파일 역할·요청→DB 흐름·핵심 코드·PHP 비교·각 과제 1개/복습 3개는 PHASE2_4.md에 있다. 전체 91개/WAR, 새 소스 사본+빈 DB, 실제 브라우저 시연 및 유지보수 실패→성공 검증을 완료했다. 다음 대화에서는 사용자가 고른 단위부터 쉽게 설명한다. 추가 기능은 아직 요청하지 않았다.

## 주문·출고 복습 및 버튼 과제 — 2026-10-21

- 사용자 답변: 1 no, 2 안됨/못함, 3 아니. 1번(확정 시 무차감), 3번(과거 주문/반품 단가 보존)은 정답이다. 2번은 출고 불가 취지로 볼 수 있으나 첫 품목의 선행 차감도 롤백되어 원래 재고로 돌아간다는 설명을 보완한다. 전체 이해 완료로 단정하지 않는다.
- order/form.jsp:13에서 버튼 문구가 작성 상태로 저장 → 작성 저장으로 바뀐 것을 확인했다. form의 POST /orders와 버튼 저장 동작은 그대로다. 사용자 변경을 대신 수정하지 않았다. 이번 확인은 저장된 소스 기준이며 실제 HTTP 반영은 확인하지 않았다.
- 설명: 확정은 주문 상태만 변경, 출고는 재고 차감. A 5권/B 0권에서 A 2권/B 3권 출고는 모두 실패하여 A 5/B 0과 확정 상태 유지. PHP PDO의 같은 트랜잭션에서 rollback하는 것과 같다. 주문 당시 unit_price를 별도 보존해 이후 현재 정가와 반품 금액을 분리한다.
- 학습 과제(주문 버튼 문구 변경)는 소스 확인 완료. 추가 구현·다음 주제 진행 없음. 앱 코드를 변경하지 않아 전체 테스트는 재실행하지 않았다.

## 현재 주제 — Boot 4 전환 (2026-10-21)

사용자가 Boot 4 전환을 승인했다. 기존 업무 코드는 유지하고 Boot 4.1.1·Security 7.1.1·Framework 7.0.9·MyBatis Starter 4.1.0·Tomcat 11로 전환했다. 초기 92개/WAR와 실제 브라우저 검증을 통과했다. 원격에서 이전 Spring CVE는 사라졌으나 기본 Jackson 3.1.5의 보안 항목을 발견해 3.1.7 패치를 검증 중이다. 최종 상태는 RELEASE.md/BOOT4_MIGRATION.md에 기록한다.

PHP 비교는 Composer 프레임워크/관련 패키지 갱신과 기존 SQL 보존으로 설명한다. 새 MVC/보안/Flyway 스타터, Jackson 3, Boot 4 설정 키·테스트 패키지 역할을 문서에 정리했다. 과제 1개는 pom.xml의 name 표시만 변경해 Maven 출력을 확인하는 것이다. 대신 수행하지 않았다. 복습 3개는 MyBatis 호환성, JSP WAR, 기능 테스트와 보안 검사 구분이다. 이해 확인을 받지 않았으며 서버 가입은 사용자가 더 생각하기로 해 보류한다.

Boot 4 최종 결과: Jackson 3.1.7까지 적용한 로컬 92개/WAR, 실제 최신 GitHub clone+빈 DB 92개/WAR, 원격 92개·Java 71개 분석·HIGH/CRITICAL/secret 검사 모두 성공. 이전 Spring/Jackson 발견 항목은 사라졌다. 코드·데이터·사용자 문구를 보존하고 서버 가입은 보류한다. BOOT4_MIGRATION.md의 과제/복습 답변은 아직 받지 않았다.
