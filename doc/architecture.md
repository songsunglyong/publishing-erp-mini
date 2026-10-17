# 현재 구조와 요청 흐름

`Controller → Service → Mapper interface → MyBatis XML → MariaDB`로 요청을 처리한다. Controller는 폼 바인딩과 화면 전환만 담당하고 SQL은 XML에 둔다. Service가 트랜잭션 경계다. C#/ASP.NET에 비유하면 Controller는 MVC Controller, Form은 검증 가능한 ViewModel, Service의 `@Transactional`은 업무 단위의 트랜잭션 경계에 해당한다.

## 도서 등록

1. GET `/books/new`에서 세션에 묶인 CSRF 토큰과 JSP 폼을 반환한다.
2. POST `/books`에서 토큰, 허용 필드, Jakarta Validation을 확인한다.
3. `BookService.create`가 `books`와 초기 재고 0의 `inventory`를 같은 트랜잭션에 저장한다. 재고 행 실패 시 도서 INSERT도 롤백한다.
4. ISBN 유일성은 DB UNIQUE가 최종 보장한다. 중복 키 예외는 폼의 한국어 오류로 변환한다.
5. 성공하면 목록으로 리다이렉트한다. 조회는 `PageQuery`의 검증된 페이지 값과 바인딩 매개변수를 사용한다.

거래처도 같은 구조로 처리한다. 두 도메인은 각각 명시적인 서비스와 매퍼를 사용한다. 비활성화는 UPDATE이며 물리 삭제 API는 없다. 정가는 `Long`/BIGINT 정수 KRW다. 수정 화면에서 사용 상태를 다시 바꿀 수 있다.

## 현재 HTTP 경로

| 메서드 | 경로 | 동작 |
| --- | --- | --- |
| GET | `/`, `/health` | 홈, 실제 DB 연결 상태 |
| GET | `/books`, `/partners` | 검색·필터·페이지 목록 |
| GET | `/books/new`, `/partners/new` | 등록 폼 |
| GET | `/books/{id}/edit`, `/partners/{id}/edit` | 수정 폼 |
| POST | `/books`, `/partners` | 등록 |
| POST | `/books/{id}`, `/partners/{id}` | 수정 |
| POST | `/books/{id}/deactivate`, `/partners/{id}/deactivate` | 비활성화 |

검색 매개변수는 `q`, `status`(빈 값/true/false), `page`(1부터)다. 최신 등록 순, 페이지당 10건이다. 검색의 `%`, `_`, `!`는 리터럴로 이스케이프하고 모든 SQL 값은 `#{}`로 바인딩한다. 존재하지 않는 수정 대상은 404, 잘못된 페이지 매개변수는 400, CSRF 누락·불일치는 403이다.

JSP의 동적 출력은 `c:out`과 HTML escaping이 켜진 Spring form 태그를 사용한다. 각 JSP·JSPF에 UTF-8 pageEncoding을 명시했다. 현재 세션 CSRF 구현은 로그인 기능을 제공하지 않는다. Phase 3에서는 Spring Security로 권한·로그인과 CSRF를 통합한다.
