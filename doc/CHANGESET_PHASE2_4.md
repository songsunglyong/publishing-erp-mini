# Phase 2~4 변경 파일 목록

이번 연속 구현에서 생성·수정한 경로다. 기존 오류/보안 작업과 사용자 JSP 변경은 작업 트리에 함께 보존돼 있으며 이번 범위와 구분한다. 파일별 책임과 요청 흐름은 PHASE2_4.md/architecture.md에 설명한다.

## 주문·출고·반품 Java

- [src/main/java/com/portfolio/erp/order/OrderController.java](../src/main/java/com/portfolio/erp/order/OrderController.java)
- [src/main/java/com/portfolio/erp/order/OrderForm.java](../src/main/java/com/portfolio/erp/order/OrderForm.java)
- [src/main/java/com/portfolio/erp/order/OrderItem.java](../src/main/java/com/portfolio/erp/order/OrderItem.java)
- [src/main/java/com/portfolio/erp/order/OrderMapper.java](../src/main/java/com/portfolio/erp/order/OrderMapper.java)
- [src/main/java/com/portfolio/erp/order/OrderQuery.java](../src/main/java/com/portfolio/erp/order/OrderQuery.java)
- [src/main/java/com/portfolio/erp/order/OrderService.java](../src/main/java/com/portfolio/erp/order/OrderService.java)
- [src/main/java/com/portfolio/erp/order/ReturnForm.java](../src/main/java/com/portfolio/erp/order/ReturnForm.java)
- [src/main/java/com/portfolio/erp/order/ReturnRecord.java](../src/main/java/com/portfolio/erp/order/ReturnRecord.java)
- [src/main/java/com/portfolio/erp/order/SalesOrder.java](../src/main/java/com/portfolio/erp/order/SalesOrder.java)

## 재고·원장 Java

- [src/main/java/com/portfolio/erp/inventory/InventoryController.java](../src/main/java/com/portfolio/erp/inventory/InventoryController.java)
- [src/main/java/com/portfolio/erp/inventory/InventoryMapper.java](../src/main/java/com/portfolio/erp/inventory/InventoryMapper.java)
- [src/main/java/com/portfolio/erp/inventory/InventoryQuery.java](../src/main/java/com/portfolio/erp/inventory/InventoryQuery.java)
- [src/main/java/com/portfolio/erp/inventory/InventoryService.java](../src/main/java/com/portfolio/erp/inventory/InventoryService.java)
- [src/main/java/com/portfolio/erp/inventory/StockForm.java](../src/main/java/com/portfolio/erp/inventory/StockForm.java)
- [src/main/java/com/portfolio/erp/inventory/StockLedger.java](../src/main/java/com/portfolio/erp/inventory/StockLedger.java)
- [src/main/java/com/portfolio/erp/inventory/StockMovement.java](../src/main/java/com/portfolio/erp/inventory/StockMovement.java)
- [src/main/java/com/portfolio/erp/inventory/StockRow.java](../src/main/java/com/portfolio/erp/inventory/StockRow.java)

## 보고서 Java

- [src/main/java/com/portfolio/erp/report/ReportController.java](../src/main/java/com/portfolio/erp/report/ReportController.java)
- [src/main/java/com/portfolio/erp/report/ReportMapper.java](../src/main/java/com/portfolio/erp/report/ReportMapper.java)
- [src/main/java/com/portfolio/erp/report/ReportQuery.java](../src/main/java/com/portfolio/erp/report/ReportQuery.java)
- [src/main/java/com/portfolio/erp/report/ReportRow.java](../src/main/java/com/portfolio/erp/report/ReportRow.java)
- [src/main/java/com/portfolio/erp/report/ReportService.java](../src/main/java/com/portfolio/erp/report/ReportService.java)

## SQL·JSP·정적 자산

- [src/main/resources/db/migration/V3__orders_inventory_and_returns.sql](../src/main/resources/db/migration/V3__orders_inventory_and_returns.sql)
- [src/main/resources/mapper/OrderMapper.xml](../src/main/resources/mapper/OrderMapper.xml)
- [src/main/resources/mapper/InventoryMapper.xml](../src/main/resources/mapper/InventoryMapper.xml)
- [src/main/resources/mapper/ReportMapper.xml](../src/main/resources/mapper/ReportMapper.xml)
- [src/main/resources/static/js/order-form.js](../src/main/resources/static/js/order-form.js)
- [src/main/webapp/WEB-INF/jsp/order/detail.jsp](../src/main/webapp/WEB-INF/jsp/order/detail.jsp)
- [src/main/webapp/WEB-INF/jsp/order/form.jsp](../src/main/webapp/WEB-INF/jsp/order/form.jsp)
- [src/main/webapp/WEB-INF/jsp/order/list.jsp](../src/main/webapp/WEB-INF/jsp/order/list.jsp)
- [src/main/webapp/WEB-INF/jsp/inventory/form.jsp](../src/main/webapp/WEB-INF/jsp/inventory/form.jsp)
- [src/main/webapp/WEB-INF/jsp/inventory/list.jsp](../src/main/webapp/WEB-INF/jsp/inventory/list.jsp)
- [src/main/webapp/WEB-INF/jsp/inventory/movements.jsp](../src/main/webapp/WEB-INF/jsp/inventory/movements.jsp)
- [src/main/webapp/WEB-INF/jsp/report/sales.jsp](../src/main/webapp/WEB-INF/jsp/report/sales.jsp)
- [src/main/webapp/WEB-INF/jsp/report/table.jspf](../src/main/webapp/WEB-INF/jsp/report/table.jspf)

## 공통 통합·테스트

- [src/main/java/com/portfolio/erp/common/CurrentActor.java](../src/main/java/com/portfolio/erp/common/CurrentActor.java)
- [src/main/java/com/portfolio/erp/common/GlobalExceptionHandler.java](../src/main/java/com/portfolio/erp/common/GlobalExceptionHandler.java)
- [src/main/java/com/portfolio/erp/common/error/ErrorCode.java](../src/main/java/com/portfolio/erp/common/error/ErrorCode.java)
- [src/main/java/com/portfolio/erp/security/SecurityConfiguration.java](../src/main/java/com/portfolio/erp/security/SecurityConfiguration.java)
- [src/main/resources/messages.properties](../src/main/resources/messages.properties)
- [src/main/resources/messages_ko.properties](../src/main/resources/messages_ko.properties)
- [src/main/webapp/WEB-INF/jsp/common/header.jspf](../src/main/webapp/WEB-INF/jsp/common/header.jspf)
- [src/main/webapp/WEB-INF/jsp/common/pagination.jspf](../src/main/webapp/WEB-INF/jsp/common/pagination.jspf)
- [src/test/java/com/portfolio/erp/TransactionTest.java](../src/test/java/com/portfolio/erp/TransactionTest.java)
- [src/test/java/com/portfolio/erp/SecurityMvcTest.java](../src/test/java/com/portfolio/erp/SecurityMvcTest.java)
- [src/test/java/com/portfolio/erp/MasterDataTest.java](../src/test/java/com/portfolio/erp/MasterDataTest.java)

## Docker 준비

- [Dockerfile](../Dockerfile)
- [compose.yaml](../compose.yaml)
- [.dockerignore](../.dockerignore)
- [.env.example](../.env.example)
- [src/main/resources/application-container.yml](../src/main/resources/application-container.yml)

## 문서·증거

- [README.md](../README.md)
- [doc/AGENTS.md](../doc/AGENTS.md)
- [doc/PROJECT.md](../doc/PROJECT.md)
- [doc/TODO.md](../doc/TODO.md)
- [doc/CHANGELOG.md](../doc/CHANGELOG.md)
- [doc/DECISIONS.md](../doc/DECISIONS.md)
- [doc/LEARNING.md](../doc/LEARNING.md)
- [doc/README.md](../doc/README.md)
- [doc/architecture.md](../doc/architecture.md)
- [doc/erd.md](../doc/erd.md)
- [doc/TEST_RESULTS.md](../doc/TEST_RESULTS.md)
- [doc/SECURITY.md](../doc/SECURITY.md)
- [doc/USER_MESSAGES.md](../doc/USER_MESSAGES.md)
- [doc/PHASE2_4.md](../doc/PHASE2_4.md)
- [doc/CONTAINERS.md](../doc/CONTAINERS.md)
- [doc/DEMO.md](../doc/DEMO.md)
- [doc/maintenance-case.md](../doc/maintenance-case.md)
- [doc/test-output.txt](../doc/test-output.txt)
- [doc/CHANGESET_PHASE2_4.md](../doc/CHANGESET_PHASE2_4.md)
- [doc/screenshots/01-login.png](../doc/screenshots/01-login.png)
- [doc/screenshots/02-order-form.png](../doc/screenshots/02-order-form.png)
- [doc/screenshots/03-order-return.png](../doc/screenshots/03-order-return.png)
- [doc/screenshots/04-stock-ledger.png](../doc/screenshots/04-stock-ledger.png)
- [doc/screenshots/05-sales-report.png](../doc/screenshots/05-sales-report.png)
