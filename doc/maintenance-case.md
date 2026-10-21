# 유지보수 사례: 일부 재고만 차감되는 출고 버그

2026-10-20, 합성 데이터·임시 소스 사본·별도 테스트 DB에서 실제 재현했다. 현재 업무 소스와 업무 DB에 버그를 적용하지 않았다. 실제 고객 장애를 해결한 사례로 주장하지 않는다. PRD의 docs 경로는 이 저장소의 기존 문서 위치인 doc으로 통일했다.

## 증상과 기대 결과

도서 A 재고 5권, B 재고 0권에서 A 2권·B 3권 주문을 확정하고 출고한다. B 재고가 부족하므로 출고 전체가 실패해야 한다. 기대값은 A 5권, B 0권, 출고 원장 0건, 주문 확정 상태 유지다.

## 안전한 재현 준비

실험은 업무 앱을 멈추거나 업무 DB를 바꾸지 않는다. 현재 소스를 비밀/target 없이 임시 사본으로 복사하고 Java 21과 **별도 `_test` 스키마/전용 계정**의 TEST_DB_URL/TEST_DB_USERNAME/TEST_DB_PASSWORD를 환경으로 지정한다. 테스트 DB에는 DDL·trigger 생성 권한이 필요하다. 먼저 원본으로 아래 테스트가 통과하는지 확인한다. 임시 사본의 StockLedger에서 아래 한 줄만 바꿔 같은 명령을 실행하고, finally에 해당하는 복구 절차로 원본 한 줄을 되돌려 다시 실행한다. 실행 로그를 분리해 실패/성공을 비교한다. 비밀번호는 명령 인자나 출력에 넣지 않는다.

## 원인 재현

`StockLedger.apply`의 트랜잭션 설정 한 줄을 임시 복사본에서만 변경했다.

```java
// 의도적으로 심은 버그: 품목마다 별도 트랜잭션으로 커밋한다.
@Transactional(propagation = Propagation.REQUIRES_NEW)
```

A의 재고 변경·원장이 먼저 커밋되고 B에서 예외가 나면, 외부 출고 트랜잭션의 롤백으로 A를 되돌릴 수 없다. “각 품목 처리에 트랜잭션이 있다”는 사실만으로 전체 출고의 원자성이 보장되지 않는다.

## 실제 실패한 회귀 테스트

```sh
./scripts/verify.sh -Dtest=TransactionTest#insufficientSecondItemRollsBackAllBalancesLedgerAndStatus
```

```text
expected: 5
 but was: 3
Tests run: 1, Failures: 1, Errors: 0, Skipped: 0
BUILD FAILURE
```

실제 실행 로그는 Git 제외 `.local/maintenance-before.log`다. 임시 사본은 `/private/tmp/erp-portfolio-4ttpsgva`였으며 환경 파일과 target을 복사하지 않았다.

## 수정과 이유

```java
@Transactional(propagation = Propagation.MANDATORY)
```

`OrderService.fulfill`의 기존 트랜잭션에 반드시 참여하게 했다. 트랜잭션 없이 원장 기능을 호출하면 실패하도록 하여 재고와 원장을 별도로 저장하는 실수를 조기에 발견한다. `StockLedger`는 별도 Spring Bean이므로 같은 클래스 내부 호출로 프록시를 우회하지 않는다.

PHP/PDO로 비교하면 하나의 `beginTransaction()` 안에서 A 변경 → B 검사 → 상태 변경을 실행하고 예외 시 `rollBack()`하는 구조다. A만 별도 연결에서 `commit()`하면 외부 `rollBack()`으로 되돌릴 수 없는 것과 같다.

수정 후 같은 명령 결과:

```text
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

`.local/maintenance-after.log`에 결과를 기록했다. 현재 소스의 전체 검증은 `TEST_RESULTS.md` 참고. 재현용 변경은 임시 사본에서 복구했다.

## 관련 방어

- 주문 행 FOR UPDATE로 중복 출고를 직렬화한다.
- 도서 ID 순으로 재고 행을 잠그고 잔액 음수를 거부한다.
- 모든 품목 재고·원장·주문 상태를 같은 트랜잭션에 저장한다.
- 출고 원장 operation_key의 UNIQUE 제약으로 중복 저장도 차단한다.
- 반품도 주문 행과 최신 품목을 잠그고 출고 수량 한도를 검사한다.
- 별도 회귀 테스트에서 동시 주문 출고, 중복 출고, 동시 반품을 실제 MariaDB 연결 2개로 실행한다.

근거: [Spring 트랜잭션 전파](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/tx-propagation.html), [@Transactional 프록시 호출](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html).
