# Phase 2~4 구현·학습 안내

2026-10-21 후속: GitHub 업로드·실제 fresh clone 91개/WAR 검증 완료. 원격 CI와 무료 우선 배포 준비의 최신 상태는 RELEASE.md를 참고한다. 아래 단계별 기록은 2026-10-20 당시 결과다.


2026-10-20 사용자 요청으로 Phase 4 산출물까지 연속 구현했다. 구현 완료와 학습 이해 완료는 다르다. 마지막 로그인 과제/복습은 아직 확인하지 않았으며 아래 내용도 이후 작은 단위로 학습한다. Docker 실행·서버 호스팅·GitHub 푸시는 수행하지 않았다.

## 단계별 결과

| 단계 | 구현·변경 파일 | 실제 검증 | 남은 것 |
| --- | --- | --- | --- |
| Phase 2 | order/, inventory/, OrderMapper.xml, InventoryMapper.xml, V3 SQL, order·inventory JSP, order-form.js | 주문/재고/반품 통합 16개, 다품목 실제 JSP 시나리오, 동시 출고/반품·롤백 | 예약/배정, 주문 수정, 고급 물류는 범위 밖 |
| Phase 3 | report/, ReportMapper.xml, report JSP, SecurityConfiguration 및 공통 메뉴·오류 메시지 확장 | 월 경계 집계 대사, 기존 보안 포함 전체 91개 | 분산 세션, 외부 인증, 운영 HTTPS는 별도 |
| Phase 4 | Dockerfile, compose.yaml, .dockerignore, container 프로필, README·ERD·화면 5개·DEMO·유지보수 사례 | 새 소스 사본+빈 DB 전체 91개/WAR, 실제 Chromium 시연, 유지보수 실패→성공 재현 | 실제 원격 fresh clone·Compose 기동·원격 Actions/Trivy 미검증 |

새 사본 검증은 현재 작업 트리를 비밀/target 없이 복사한 것이다. 아직 푸시하지 않은 변경이 있으므로 “현재 GitHub에서 clone하면 동일 소스가 온다”고 보장하지 않는다. PRD Phase 4의 엄격한 전체 통과 조건은 후속 검증이 남아 있다.

## 학습 단위 A: 주문·출고

이번 기능은 여러 도서를 한 주문에 저장하고 작성→확정→출고/취소를 검사한다. 폼의 합계를 믿지 않고 서버가 단가×수량을 계산하며 주문 당시 값을 보존한다.

파일: OrderForm은 입력 제약, OrderController는 입력/뷰/리다이렉트, OrderService는 업무·트랜잭션, OrderMapper.java는 SQL 호출 계약, OrderMapper.xml은 바인딩 SQL, order/*.jsp는 출력이다. GET /orders/new → JSP → POST /orders → Form 검증 → Service.create → 헤더/품목 INSERT → 상세 GET이다.

핵심: `@Transactional`은 헤더와 모든 품목을 함께 저장/취소한다. `FOR UPDATE`는 출고 요청끼리 같은 주문/재고를 동시에 바꾸지 못하게 한다. 확정은 예약이 아니므로 확정 후라도 출고 시 재고 부족이 날 수 있다. 도서 ID 순 잠금으로 반대 품목 순서의 교착 가능성을 줄인다. 예외를 무시하고 성공처럼 반환하지 않는다.

PHP 비교: $_POST를 담는 폼 객체, 라우터의 POST 핸들러인 Controller, PDO beginTransaction/commit/rollBack에 대응하는 Service 경계다. MyBatis #{}는 PDO 바인딩과 같다. @Transactional은 Spring이 관리하는 객체의 외부 호출에 적용되므로 재고 처리는 별도 Bean으로 분리한다.

실행: 공통 빠른 시작 후 `/orders`. 검증: TransactionTest의 create/confirm/cancel/fulfill/rollback/concurrent 테스트 및 실제 HTTP.

직접 수정 과제 1개: order/form.jsp의 작성 저장 버튼 문구만 원하는 한국어로 바꿔 새로고침한다. method/action은 유지한다. 대신 수행하지 않았다.

복습 3개:
1. 확정 버튼을 누르면 바로 재고가 차감되는가?
2. 두 번째 품목 재고가 부족하면 첫 번째 품목의 차감은 어떻게 되는가?
3. 브라우저가 전송한 총액을 그대로 DB에 저장해도 되는가?

## 학습 단위 B: 재고 원장·부분 반품

InventoryService는 입고·ADMIN 조정, StockLedger는 잔액 검사/수정과 원장 저장을 책임진다. StockForm은 수량·사유·요청 UUID를 검증한다. ReturnForm과 OrderService.returnItems는 출고 완료 주문의 미반품 수량만 허용한다. inventory JSP는 목록/입고/원장, order/detail.jsp는 반품 폼과 이력을 표시한다.

흐름: 반품 POST → CSRF/역할/폼 검증 → 주문/품목 잠금 → 소속·남은 수량 확인 → 반품 헤더/품목 저장 → 반환 수량 누적 → 재고/원장 저장 → 상세 GET. UUID는 CSRF나 권한 검사의 대체물이 아니며 같은 저장 요청의 중복 처리 방지용이다.

핵심: StockLedger의 MANDATORY는 호출자의 트랜잭션이 없으면 거부하고 있으면 참여한다. 새 트랜잭션으로 품목마다 먼저 커밋하면 부분 저장 버그가 난다. maintenance-case.md에 실제 재현/회귀 테스트가 있다. 반품 금액은 현재 정가가 아닌 주문 단가를 사용한다.

PHP 비교: 같은 PDO 연결의 트랜잭션 안에서 재고 UPDATE와 이력 INSERT를 호출하는 것이다. 반품 가능 수량은 `출고 수량 - 누적 반품 수량`이며 PHP에서도 같은 계산·잠금이 필요하다.

실행: `/inventory/receipts`, `/inventory/movements`, 출고 주문 상세. 검증: 입고 재요청·조정 권한·전체/부분/초과/타 주문 품목 반품·동시 반품.

직접 수정 과제 1개: inventory/form.jsp의 입고 사유 안내 문구만 구체적으로 바꾼다. 필수 검증과 필드명은 유지한다.

복습 3개:
1. 출고 3권 중 1권을 반품했다면 추가로 반품 가능한 수량은?
2. 재고만 저장하고 원장 저장이 실패하면 재고 변경을 유지해도 되는가?
3. 일반 USER가 재고 조정을 요청하면 어느 계층에서 차단해야 하는가?

## 학습 단위 C: 보고서·포트폴리오

ReportQuery는 날짜/부족 재고 기준, ReportController는 폼/모델, ReportService는 읽기 트랜잭션, ReportMapper.xml은 출고/반품 이벤트 UNION ALL와 SUM/GROUP BY, report JSP는 집계 표시를 담당한다. 조회 요청→검증→SQL→ReportRow(BigDecimal 합계)→Model→JSP 순이다. 기간 오류는 같은 검색 폼을 보여준다.

출고일에 매출을 더하고 반품일에 차감한다. 1월 700원 출고, 2월 300원 반품이면 1월 700원/2월 -300원/전체 400원이다. 현재 부족 재고는 현재 잔액이므로 날짜와 무관하다. 순매출 상위 20개는 전체 합계와 구분한다.

PHP 비교: 기간을 바인딩한 PDO 집계 SELECT를 실행해 템플릿에 넘기는 것과 같다. Java의 BigDecimal은 SQL SUM의 정밀도를 잃지 않기 위해 사용하며 금액을 double로 바꾸지 않는다. 읽기 트랜잭션은 여러 표가 같은 DB 스냅샷을 사용하게 한다.

실행: `/reports/sales`, `/dashboard`는 보고서로 이동. 검증: 월 경계 출고/반품 금액 대사 및 실제 JSP/브라우저. 시연은 DEMO.md, 파일/한계는 TEST_RESULTS.md/CONTAINERS.md 참고.

직접 수정 과제 1개: 보고서에서 이번 달의 시작일·종료일을 직접 선택해 조회하고 반품 차감과 순매출을 비교한다. 코드는 수정하지 않아도 된다.

복습 3개:
1. 1월 출고 도서를 2월에 반품하면 어느 달 반품 차감에 잡히는가?
2. 주문 후 현재 정가를 바꾸면 과거 반품 단가도 바뀌는가?
3. 소스 사본 테스트 통과만으로 Compose 기동 성공을 말할 수 있는가?
