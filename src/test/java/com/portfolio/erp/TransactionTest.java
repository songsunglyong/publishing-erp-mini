package com.portfolio.erp;

import static org.assertj.core.api.Assertions.*;

import com.portfolio.erp.book.*;
import com.portfolio.erp.common.error.*;
import com.portfolio.erp.inventory.*;
import com.portfolio.erp.order.*;
import com.portfolio.erp.partner.*;
import com.portfolio.erp.report.*;
import com.portfolio.erp.security.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import({SecurityTestUsers.class, TransactionTest.TimeConfig.class})
class TransactionTest {
  @Autowired OrderService orders;
  @Autowired InventoryService inventory;
  @Autowired ReportService reports;
  @Autowired BookService books;
  @Autowired PartnerService partners;
  @Autowired UserMapper users;
  @Autowired JdbcTemplate jdbc;
  @Autowired BusinessClock clock;
  @LocalServerPort int port;
  long a, b, partner;
  String marker;

  @BeforeEach
  void prepare() {
    assertThat(jdbc.queryForObject("SELECT DATABASE()", String.class)).endsWith("_test");
    authenticate("tst_admin");
    clock.time.set(Instant.parse("2026-01-31T14:50:00Z"));
    marker = "TXN_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    var p = new PartnerForm();
    p.setCode(marker);
    p.setName(marker + " 책방");
    p.setType("BOOKSTORE");
    partner = partners.create(p);
    a = book(marker + " 도서 A", 100);
    b = book(marker + " 도서 B", 200);
    jdbc.update("DELETE FROM login_attempts");
  }

  @AfterEach
  void cleanup() {
    SecurityContextHolder.clearContext();
    jdbc.update(
        "DELETE ri FROM return_items ri JOIN order_returns r ON r.id=ri.return_id JOIN sales_orders"
            + " o ON o.id=r.order_id WHERE o.partner_id=?",
        partner);
    jdbc.update(
        "DELETE r FROM order_returns r JOIN sales_orders o ON o.id=r.order_id WHERE o.partner_id=?",
        partner);
    jdbc.update(
        "DELETE m FROM stock_movements m JOIN books b ON b.id=m.book_id WHERE b.title LIKE ?",
        marker + "%");
    jdbc.update(
        "DELETE i FROM order_items i JOIN sales_orders o ON o.id=i.order_id WHERE o.partner_id=?",
        partner);
    jdbc.update("DELETE FROM sales_orders WHERE partner_id=?", partner);
    jdbc.update(
        "DELETE i FROM inventory i JOIN books b ON b.id=i.book_id WHERE b.title LIKE ?",
        marker + "%");
    jdbc.update("DELETE FROM books WHERE title LIKE ?", marker + "%");
    jdbc.update("DELETE FROM partners WHERE id=?", partner);
  }

  void authenticate(String username) {
    var p = new ErpPrincipal(users.findByUsername(username));
    p.eraseCredentials();
    SecurityContextHolder.getContext()
        .setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(p, null, p.getAuthorities()));
  }

  long book(String title, long price) {
    var form = new BookForm();
    form.setTitle(title);
    form.setListPrice(price);
    return books.create(form);
  }

  StockForm stock(long id, int qty) {
    var form = new StockForm();
    form.setBookId(id);
    form.setQuantity(qty);
    form.setNote("합성 테스트 사유");
    form.setRequestKey(UUID.randomUUID().toString());
    return form;
  }

  OrderForm form(int qa, int qb) {
    var f = new OrderForm();
    f.setPartnerId(partner);
    f.setRequestKey(UUID.randomUUID().toString());
    f.setItems(new ArrayList<>());
    for (var pair : new long[][] {{a, qa}, {b, qb}}) {
      if (pair[1] == 0) continue;
      var line = new OrderForm.Line();
      line.setBookId(pair[0]);
      line.setQuantity((int) pair[1]);
      f.getItems().add(line);
    }
    return f;
  }

  long order(int qa, int qb) {
    return orders.create(form(qa, qb));
  }

  int balance(long id) {
    return jdbc.queryForObject(
        "SELECT quantity_on_hand FROM inventory WHERE book_id=?", Integer.class, id);
  }

  long movements(long id) {
    return jdbc.queryForObject(
        "SELECT COUNT(*) FROM stock_movements WHERE book_id=?", Long.class, id);
  }

  ReturnForm returns(long orderId, int qa, int qb) {
    var f = new ReturnForm();
    f.setRequestKey(UUID.randomUUID().toString());
    f.setReason("합성 부분 반품");
    for (var item : orders.find(orderId).getItems()) {
      var line = new ReturnForm.Line();
      line.setItemId(item.getId());
      line.setQuantity(item.getBookId() == a ? qa : qb);
      f.getItems().add(line);
    }
    return f;
  }

  void code(Runnable task, ErrorCode expected) {
    assertThatThrownBy(task::run)
        .isInstanceOfSatisfying(
            UserFacingException.class, ex -> assertThat(ex.getCode()).isEqualTo(expected));
  }

  @Test
  void createSnapshotsServerPricesAndAtomicItems() {
    var f = form(3, 2);
    f.getItems().get(1).setUnitPrice(150L);
    long id = orders.create(f);
    assertThat(orders.find(id).getTotalAmount()).isEqualTo(600L);
    assertThat(orders.find(id).getItems()).hasSize(2);
    var changed = new BookForm();
    changed.setTitle(marker + " 변경 후 제목");
    changed.setListPrice(9999L);
    books.update(a, changed);
    assertThat(orders.find(id).getItems().getFirst().getBookTitle()).contains(marker);
    assertThat(orders.find(id).getItems().getFirst().getUnitPrice()).isEqualTo(100L);
    code(() -> orders.create(f), ErrorCode.DUPLICATE_OPERATION);
  }

  @Test
  void inactiveMastersDuplicateBooksAndMissingKeysAreRejected() {
    partners.deactivate(partner);
    code(() -> order(1, 0), ErrorCode.INACTIVE_MASTER);
    jdbc.update("UPDATE partners SET active=TRUE WHERE id=?", partner);
    books.deactivate(a);
    code(() -> order(1, 0), ErrorCode.INACTIVE_MASTER);
    jdbc.update("UPDATE books SET active=TRUE WHERE id=?", a);
    var f = form(1, 0);
    f.getItems().add(f.getItems().getFirst());
    code(() -> orders.create(f), ErrorCode.ORDER_ITEMS_INVALID);
    var missing = form(1, 0);
    missing.setRequestKey(null);
    assertThatThrownBy(() -> orders.create(missing))
        .isInstanceOf(jakarta.validation.ConstraintViolationException.class);
  }

  @Test
  void invalidSecondLineNeverLeavesPartialOrder() {
    long before = jdbc.queryForObject("SELECT COUNT(*) FROM sales_orders", Long.class);
    var f = form(1, 1);
    f.getItems().get(1).setBookId(Long.MAX_VALUE);
    code(() -> orders.create(f), ErrorCode.INACTIVE_MASTER);
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sales_orders", Long.class))
        .isEqualTo(before);
  }

  @Test
  void databaseFailureInSecondItemRollsBackHeaderAndFirstItem() {
    long before = jdbc.queryForObject("SELECT COUNT(*) FROM sales_orders", Long.class);
    jdbc.execute(
        "CREATE TRIGGER test_order_item_failure BEFORE INSERT ON order_items FOR EACH ROW BEGIN IF"
            + " NEW.book_id="
            + b
            + " THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='synthetic rollback'; END IF; END");
    try {
      assertThatThrownBy(() -> order(1, 1))
          .isInstanceOf(org.springframework.dao.DataAccessException.class);
    } finally {
      jdbc.execute("DROP TRIGGER test_order_item_failure");
    }
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sales_orders", Long.class))
        .isEqualTo(before);
  }

  @Test
  void confirmAndCancelDoNotReserveOrChangeStock() {
    inventory.receive(stock(a, 10));
    long id = order(4, 0);
    orders.confirm(id);
    assertThat(balance(a)).isEqualTo(10);
    orders.cancel(id);
    assertThat(balance(a)).isEqualTo(10);
    assertThat(orders.find(id).getStatus()).isEqualTo("CANCELLED");
    code(() -> orders.confirm(id), ErrorCode.ORDER_STATE);
    long draft = order(1, 0);
    orders.cancel(draft);
    assertThat(orders.find(draft).getStatus()).isEqualTo("CANCELLED");
  }

  @Test
  void fulfillOnceAndPreventDirectCancellation() {
    inventory.receive(stock(a, 10));
    long id = order(4, 0);
    code(() -> orders.fulfill(id), ErrorCode.ORDER_STATE);
    orders.confirm(id);
    orders.fulfill(id);
    assertThat(balance(a)).isEqualTo(6);
    assertThat(movements(a)).isEqualTo(2);
    code(() -> orders.fulfill(id), ErrorCode.ORDER_STATE);
    code(() -> orders.cancel(id), ErrorCode.ORDER_STATE);
    assertThat(balance(a)).isEqualTo(6);
    assertThat(orders.find(id).getFulfilledAt()).isNotNull();
  }

  @Test
  void insufficientSecondItemRollsBackAllBalancesLedgerAndStatus() {
    inventory.receive(stock(a, 5));
    long id = order(2, 3);
    orders.confirm(id);
    code(() -> orders.fulfill(id), ErrorCode.INSUFFICIENT_STOCK);
    assertThat(balance(a)).isEqualTo(5);
    assertThat(balance(b)).isZero();
    assertThat(movements(a)).isEqualTo(1);
    assertThat(orders.find(id).getStatus()).isEqualTo("CONFIRMED");
    inventory.receive(stock(b, 3));
    orders.fulfill(id);
    assertThat(balance(a)).isEqualTo(3);
    assertThat(balance(b)).isZero();
  }

  @Test
  void receiptsAreIdempotentAndAdjustmentsReconcileLedger() {
    var receipt = stock(a, 5);
    inventory.receive(receipt);
    code(() -> inventory.receive(receipt), ErrorCode.DUPLICATE_OPERATION);
    inventory.adjust(stock(a, -2));
    assertThat(balance(a)).isEqualTo(3);
    assertThat(movements(a)).isEqualTo(2);
    code(() -> inventory.adjust(stock(a, -4)), ErrorCode.INSUFFICIENT_STOCK);
    assertThat(balance(a)).isEqualTo(3);
    assertThat(
            jdbc.queryForObject(
                "SELECT SUM(quantity) FROM stock_movements WHERE book_id=?", Long.class, a))
        .isEqualTo(3L);
  }

  @Test
  void partialFullAndRepeatedReturnsUseShippedPricesAndLimits() {
    inventory.receive(stock(a, 10));
    long id = order(3, 0);
    orders.confirm(id);
    orders.fulfill(id);
    var f = returns(id, 1, 0);
    orders.returnItems(id, f);
    assertThat(balance(a)).isEqualTo(8);
    assertThat(orders.find(id).getReturnedAmount()).isEqualTo(100L);
    code(() -> orders.returnItems(id, f), ErrorCode.DUPLICATE_OPERATION);
    orders.returnItems(id, returns(id, 2, 0));
    assertThat(balance(a)).isEqualTo(10);
    code(() -> orders.returnItems(id, returns(id, 1, 0)), ErrorCode.RETURN_LIMIT);
    assertThat(orders.find(id).getReturnedAmount()).isEqualTo(300L);
  }

  @Test
  void foreignReturnItemAndUnfulfilledReturnAreRejected() {
    long first = order(1, 0), second = order(1, 0);
    code(() -> orders.returnItems(first, returns(first, 1, 0)), ErrorCode.ORDER_STATE);
    inventory.receive(stock(a, 2));
    orders.confirm(first);
    orders.fulfill(first);
    var f = returns(second, 1, 0);
    code(() -> orders.returnItems(first, f), ErrorCode.ORDER_ITEMS_INVALID);
  }

  List<String> race(
      java.util.concurrent.Callable<String> first, java.util.concurrent.Callable<String> second)
      throws Exception {
    var ready = new CountDownLatch(2);
    var start = new CountDownLatch(1);
    try (var executor = Executors.newFixedThreadPool(2)) {
      var tasks =
          List.of(first, second).stream()
              .map(
                  task ->
                      executor.submit(
                          () -> {
                            authenticate("tst_admin");
                            ready.countDown();
                            start.await();
                            try {
                              return task.call();
                            } catch (UserFacingException ex) {
                              return ex.getCode().name();
                            } finally {
                              SecurityContextHolder.clearContext();
                            }
                          }))
              .toList();
      assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
      start.countDown();
      return List.of(
          tasks.get(0).get(20, TimeUnit.SECONDS), tasks.get(1).get(20, TimeUnit.SECONDS));
    }
  }

  @Test
  void concurrentDifferentOrdersCannotOversellAnyItem() throws Exception {
    inventory.receive(stock(a, 5));
    inventory.receive(stock(b, 5));
    long first = order(4, 4), second = order(4, 4);
    orders.confirm(first);
    orders.confirm(second);
    assertThat(
            race(
                () -> {
                  orders.fulfill(first);
                  return "OK";
                },
                () -> {
                  orders.fulfill(second);
                  return "OK";
                }))
        .containsExactlyInAnyOrder("OK", "INSUFFICIENT_STOCK");
    assertThat(balance(a)).isEqualTo(1);
    assertThat(balance(b)).isEqualTo(1);
    assertThat(movements(a)).isEqualTo(2);
  }

  @Test
  void concurrentDuplicateFulfillmentProducesOneShipment() throws Exception {
    inventory.receive(stock(a, 10));
    long id = order(3, 0);
    orders.confirm(id);
    assertThat(
            race(
                () -> {
                  orders.fulfill(id);
                  return "OK";
                },
                () -> {
                  orders.fulfill(id);
                  return "OK";
                }))
        .containsExactlyInAnyOrder("OK", "ORDER_STATE");
    assertThat(balance(a)).isEqualTo(7);
    assertThat(movements(a)).isEqualTo(2);
  }

  @Test
  void concurrentReturnsCannotExceedShippedQuantity() throws Exception {
    inventory.receive(stock(a, 10));
    long id = order(3, 0);
    orders.confirm(id);
    orders.fulfill(id);
    var first = returns(id, 2, 0);
    var second = returns(id, 2, 0);
    assertThat(
            race(
                () -> {
                  orders.returnItems(id, first);
                  return "OK";
                },
                () -> {
                  orders.returnItems(id, second);
                  return "OK";
                }))
        .containsExactlyInAnyOrder("OK", "RETURN_LIMIT");
    assertThat(balance(a)).isEqualTo(9);
    assertThat(orders.find(id).getReturnedAmount()).isEqualTo(200L);
  }

  @Test
  void monthlyReportsReconcileAcrossMonthBoundaryAndKeepHistoricalPrices() {
    inventory.receive(stock(a, 10));
    inventory.receive(stock(b, 10));
    long id = order(3, 2);
    orders.confirm(id);
    orders.fulfill(id);
    clock.time.updateAndGet(value -> value.plus(Duration.ofMinutes(20)));
    orders.returnItems(id, returns(id, 1, 1));
    var january = new ReportQuery();
    january.setFrom(LocalDate.of(2026, 1, 1));
    january.setTo(LocalDate.of(2026, 1, 31));
    var jan = reports.sales(january);
    assertThat(jan.summary().getGrossAmount()).isEqualByComparingTo("700");
    assertThat(jan.summary().getReturnAmount()).isEqualByComparingTo("0");
    var february = new ReportQuery();
    february.setFrom(LocalDate.of(2026, 2, 1));
    february.setTo(LocalDate.of(2026, 2, 28));
    var feb = reports.sales(february);
    assertThat(feb.summary().getGrossAmount()).isEqualByComparingTo("0");
    assertThat(feb.summary().getReturnAmount()).isEqualByComparingTo("300");
    assertThat(feb.summary().getNetAmount()).isEqualByComparingTo("-300");
    january.setTo(LocalDate.of(2026, 2, 28));
    var whole = reports.sales(january);
    assertThat(whole.summary().getNetAmount()).isEqualByComparingTo("400");
    assertThat(whole.partners().getFirst().getNetAmount()).isEqualByComparingTo("400");
    assertThat(
            whole.books().stream()
                .map(ReportRow::getNetAmount)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add))
        .isEqualByComparingTo("400");
  }

  @Test
  void serviceRolesAndDateValidationApplyToNewFeatures() {
    authenticate("tst_manager");
    assertThatThrownBy(() -> inventory.adjust(stock(a, 1)))
        .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    authenticate("tst_user");
    assertThatThrownBy(() -> order(1, 0))
        .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    assertThat(inventory.list(new InventoryQuery())).isNotNull();
    var q = new ReportQuery();
    q.setFrom(LocalDate.of(2027, 1, 1));
    q.setTo(LocalDate.of(2026, 1, 1));
    assertThatThrownBy(() -> reports.sales(q))
        .isInstanceOf(jakarta.validation.ConstraintViolationException.class);
  }

  @Test
  void realJspScenarioPreservesInputEscapesHtmlAndCompletesWorkflow() throws Exception {
    try (var browser = new TestHttpBrowser(port)) {
      browser.login();
      for (String path :
          List.of(
              "/orders",
              "/orders/new",
              "/inventory",
              "/inventory/receipts",
              "/inventory/adjustments",
              "/inventory/movements",
              "/reports/sales")) assertThat(browser.get(path).statusCode()).as(path).isEqualTo(200);
      String payload = "<script>alert('xss')</script>";
      var invalid =
          browser.post(
              "/orders",
              Map.of(
                  "requestKey",
                  UUID.randomUUID().toString(),
                  "notes",
                  payload,
                  "items[0].quantity",
                  "0"));
      assertThat(invalid.statusCode()).isEqualTo(200);
      assertThat(invalid.body())
          .contains("VALIDATION_FAILED", "&lt;script&gt;")
          .doesNotContain(payload);
      String stockKey = UUID.randomUUID().toString();
      assertThat(
              browser
                  .post(
                      "/inventory/receipts",
                      Map.of(
                          "bookId",
                          String.valueOf(a),
                          "quantity",
                          "10",
                          "note",
                          "시연 입고",
                          "requestKey",
                          stockKey))
                  .statusCode())
          .isEqualTo(302);
      browser.get("/orders/new");
      var created =
          browser.post(
              "/orders",
              Map.of(
                  "partnerId",
                  String.valueOf(partner),
                  "notes",
                  payload,
                  "requestKey",
                  UUID.randomUUID().toString(),
                  "items[0].bookId",
                  String.valueOf(a),
                  "items[0].quantity",
                  "3"));
      assertThat(created.statusCode()).isEqualTo(302);
      String location =
          java.net.URI.create(created.headers().firstValue("Location").orElseThrow()).getPath();
      long id = Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
      assertThat(browser.get(location).body()).contains("&lt;script&gt;").doesNotContain(payload);
      assertThat(browser.post(location + "/confirm", Map.of()).statusCode()).isEqualTo(302);
      browser.get(location);
      assertThat(browser.post(location + "/fulfill", Map.of()).statusCode()).isEqualTo(302);
      assertThat(browser.get(location).body()).contains("부분 반품");
      long item = orders.find(id).getItems().getFirst().getId();
      var invalidReturn =
          browser.post(
              location + "/returns",
              Map.of(
                  "requestKey",
                  UUID.randomUUID().toString(),
                  "reason",
                  payload,
                  "items[0].itemId",
                  String.valueOf(item),
                  "items[0].quantity",
                  "4"));
      assertThat(invalidReturn.statusCode()).isEqualTo(200);
      assertThat(invalidReturn.body())
          .contains("RETURN_LIMIT", "&lt;script&gt;")
          .doesNotContain(payload);
      browser.get(location);
      assertThat(
              browser
                  .post(
                      location + "/returns",
                      Map.of(
                          "requestKey",
                          UUID.randomUUID().toString(),
                          "reason",
                          "시연 부분 반품",
                          "items[0].itemId",
                          String.valueOf(item),
                          "items[0].quantity",
                          "1"))
                  .statusCode())
          .isEqualTo(302);
      assertThat(balance(a)).isEqualTo(8);
      assertThat(browser.get(location).body()).contains("시연 부분 반품");
      assertThat(browser.get("/inventory/movements?bookId=" + a).body()).contains("시연 부분 반품");
      var report = browser.get("/reports/sales?from=2026-01-01&to=2026-01-31");
      assertThat(report.statusCode()).isEqualTo(200);
      assertThat(report.body()).contains("순매출", "월별 매출", marker);
      var badDate = browser.get("/reports/sales?from=2026-02-01&to=2026-01-01");
      assertThat(badDate.statusCode()).isEqualTo(400);
      assertThat(badDate.body()).contains("2026-02-01", "VALIDATION_FAILED");
    }
  }

  static class BusinessClock extends Clock {
    final AtomicReference<Instant> time =
        new AtomicReference<>(Instant.parse("2026-01-31T14:50:00Z"));

    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    public Clock withZone(ZoneId zone) {
      return this;
    }

    public Instant instant() {
      return time.get();
    }
  }

  @TestConfiguration
  static class TimeConfig {
    @Bean
    @Primary
    BusinessClock businessClock() {
      return new BusinessClock();
    }
  }
}
