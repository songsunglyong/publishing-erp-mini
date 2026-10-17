package com.portfolio.erp;

import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Pattern;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import jakarta.validation.ConstraintViolationException;
import com.portfolio.erp.book.*;
import com.portfolio.erp.partner.*;
import com.portfolio.erp.common.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class MasterDataTest {
    @Autowired BookService books;
    @Autowired PartnerService partners;
    @Autowired BookMapper bookMapper;
    @Autowired JdbcTemplate jdbc;
    @LocalServerPort int port;
    private final List<HttpClient> clients = new ArrayList<>();

    @BeforeEach void requireDedicatedTestDatabase() {
        assertThat(jdbc.queryForObject("SELECT DATABASE()", String.class)).endsWith("_test");
    }
    @AfterEach void removeOnlyTestRecords() {
        clients.forEach(HttpClient::close);
        jdbc.update("DELETE FROM inventory WHERE book_id IN (SELECT id FROM books WHERE title LIKE 'TST!_%' ESCAPE '!')");
        jdbc.update("DELETE FROM books WHERE title LIKE 'TST!_%' ESCAPE '!'");
        jdbc.update("DELETE FROM partners WHERE code LIKE 'TST!_%' ESCAPE '!'");
    }
    private String marker() { return "TST_" + UUID.randomUUID().toString().replace("-", "").substring(0, 15).toUpperCase(); }
    private BookForm book(String title) {
        var form = new BookForm(); form.setTitle(title); form.setListPrice(18000L);
        form.setAuthor("가상 저자"); form.setPublisher("가상 출판사");
        form.setPublicationDate(LocalDate.of(2026, 1, 2)); return form;
    }
    private PartnerForm partner(String code) {
        var form = new PartnerForm(); form.setCode(code); form.setName("가상 책방");
        form.setType("BOOKSTORE"); form.setEmail("test@example.invalid"); return form;
    }
    private PageQuery query(String q, int page, String status) {
        var query = new PageQuery(); query.setQ(q); query.setPage(page); query.setStatus(status); return query;
    }
    private long bookId(String title) { return jdbc.queryForObject("SELECT id FROM books WHERE title = ?", Long.class, title); }

    @Test void flywayAndOptInSyntheticSeedsAreAppliedOnRealMariaDb() {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM flyway_schema_history WHERE success = TRUE AND version IN ('1','1.1')", Integer.class)).isEqualTo(2);
        assertThat(books.list(query("시연 도서:", 1, "")).getTotal()).isEqualTo(3);
        assertThat(partners.list(query("DEMO_", 1, "")).getTotal()).isEqualTo(3);
    }
    @Test @Transactional void demoSeedAlsoWorksAfterLocalBooksAlreadyExist() {
        jdbc.update("DELETE FROM inventory WHERE book_id IN (SELECT id FROM books WHERE title LIKE '시연 도서:%')");
        jdbc.update("DELETE FROM books WHERE title LIKE '시연 도서:%'");
        jdbc.update("DELETE FROM partners WHERE code IN ('DEMO_STORE','DEMO_DIST','DEMO_INACTIVE')");
        long existingId = books.create(book(marker()));
        var seed = new org.springframework.jdbc.datasource.init.ResourceDatabasePopulator(
            new org.springframework.core.io.ClassPathResource("db/demo/V1_1__synthetic_master_data.sql"));
        seed.setSqlScriptEncoding("UTF-8");
        seed.execute(Objects.requireNonNull(jdbc.getDataSource()));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM inventory WHERE book_id=?", Integer.class, existingId)).isEqualTo(1);
        assertThat(books.list(query("시연 도서:", 1, "")).getTotal()).isEqualTo(3);
    }
    @Test @Transactional void bookCrudKeepsInventoryAndDeactivatedRows() {
        var form = book(marker()); long id = books.create(form);
        var saved = books.find(id);
        assertThat(saved.getCreatedAt()).isNotNull(); assertThat(saved.getPublicationDate()).isEqualTo(form.getPublicationDate());
        assertThat(jdbc.queryForObject("SELECT quantity_on_hand FROM inventory WHERE book_id = ?", Integer.class, id)).isZero();
        form.setListPrice(23000L); form.setAuthor("변경 저자"); books.update(id, form);
        assertThat(books.find(id).getListPrice()).isEqualTo(23000L);
        books.deactivate(id); books.deactivate(id);
        assertThat(books.find(id).getActive()).isFalse();
        assertThat(books.list(query(form.getTitle(), 1, "false")).getTotal()).isEqualTo(1);
        assertThat(books.list(query(form.getTitle(), 1, "true")).getTotal()).isZero();
    }
    @Test void duplicateIsbnRollsBackWithoutOrphanInventory() {
        long before = jdbc.queryForObject("SELECT COUNT(*) FROM inventory", Long.class);
        var form = book(marker()); form.setIsbn("9780000000002");
        assertThatThrownBy(() -> books.create(form)).isInstanceOf(DuplicateKeyException.class);
        assertThat(books.list(query(form.getTitle(), 1, "")).getTotal()).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM inventory", Long.class)).isEqualTo(before);
    }
    @Test void inventoryFailureRollsBackBookInsertInSameTransaction() {
        String title = marker();
        jdbc.execute("CREATE TRIGGER test_inventory_failure BEFORE INSERT ON inventory FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'rollback test'");
        try {
            assertThatThrownBy(() -> books.create(book(title))).isInstanceOf(org.springframework.dao.DataAccessException.class);
            assertThat(books.list(query(title, 1, "")).getTotal()).isZero();
        } finally {
            jdbc.execute("DROP TRIGGER test_inventory_failure");
        }
    }
    @Test @Transactional void multipleBlankIsbnsBecomeNullAndAreAllowed() {
        var first = book(marker()); first.setIsbn("   ");
        var second = book(marker()); second.setIsbn("");
        assertThat(books.find(books.create(first)).getIsbn()).isNull();
        assertThat(books.find(books.create(second)).getIsbn()).isNull();
    }
    @Test @Transactional void bookSearchFiltersAndPaginationUseLiteralWildcardsAndStableOrdering() {
        String prefix = marker();
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < 12; i++) ids.add(books.create(book(prefix + "_" + i)));
        var first = books.list(query(prefix, 1, "")); var second = books.list(query(prefix, 2, ""));
        assertThat(first.getTotal()).isEqualTo(12); assertThat(first.getItems()).hasSize(10);
        assertThat(second.getItems()).hasSize(2);
        assertThat(first.getItems().getFirst().getId()).isEqualTo(ids.getLast());
        assertThat(first.getItems()).extracting(Book::getId).doesNotContainAnyElementsOf(second.getItems().stream().map(Book::getId).toList());
        books.deactivate(ids.getFirst());
        assertThat(books.list(query(prefix, 1, "true")).getTotal()).isEqualTo(11);
        assertThat(books.list(query(prefix, 1, "false")).getTotal()).isEqualTo(1);
        books.create(book(prefix + "%_!"));
        assertThat(books.list(query(prefix + "%_!", 1, "")).getTotal()).isEqualTo(1);
        assertThat(books.list(query("' OR 1=1 --", 1, "")).getTotal()).isZero();
        assertThat(books.list(query("9780000000002", 1, "")).getTotal()).isEqualTo(1);
        assertThat(books.list(query(prefix, 50, "")).getItems()).isEmpty();
    }
    @Test @Transactional void partnerCrudSearchAndUniqueCode() {
        String prefix = marker(); var form = partner(prefix); long id = partners.create(form);
        form.setType("DISTRIBUTOR"); form.setContactPerson(prefix + " 담당자"); partners.update(id, form);
        assertThat(partners.find(id).getType()).isEqualTo("DISTRIBUTOR");
        assertThat(partners.list(query(prefix + " 담당자", 1, "")).getTotal()).isEqualTo(1);
        assertThatThrownBy(() -> partners.create(partner(prefix))).isInstanceOf(DuplicateKeyException.class);
        partners.deactivate(id);
        assertThat(partners.find(id).getActive()).isFalse();
        assertThat(partners.list(query(prefix, 1, "false")).getTotal()).isEqualTo(1);
        assertThat(partners.list(query(prefix, 1, "true")).getTotal()).isZero();
    }
    @Test @Transactional void partnerPaginationAndLiteralSearch() {
        String prefix = marker();
        for (int i = 0; i < 11; i++) partners.create(partner(prefix + "_" + i));
        assertThat(partners.list(query(prefix, 1, "")).getItems()).hasSize(10);
        assertThat(partners.list(query(prefix, 2, "")).getItems()).hasSize(1);
        assertThat(partners.list(query(prefix + "%", 1, "")).getTotal()).isZero();
    }
    @Test void validationAndDatabaseConstraintsBothRejectInvalidData() {
        var bad = book(marker()); bad.setListPrice(-1L);
        assertThatThrownBy(() -> books.create(bad)).isInstanceOf(ConstraintViolationException.class);
        var entity = Book.from(bad);
        assertThatThrownBy(() -> bookMapper.insert(entity)).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("INSERT INTO inventory (book_id, quantity_on_hand) VALUES (9223372036854775807,0)")).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE inventory SET quantity_on_hand=-1 LIMIT 1")).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> books.find(Long.MAX_VALUE)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> partners.update(Long.MAX_VALUE, partner(marker()))).isInstanceOf(NotFoundException.class);
    }

    private class Browser {
        final HttpClient client;
        String token;
        Browser() {
            client = HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).followRedirects(HttpClient.Redirect.NEVER).build();
            clients.add(client);
        }
        HttpResponse<String> get(String path) throws Exception {
            var response = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).GET().build(), HttpResponse.BodyHandlers.ofString());
            var match = Pattern.compile("name=\"_csrf\" value=\"([^\"]+)\"").matcher(response.body());
            if (match.find()) token = match.group(1);
            return response;
        }
        HttpResponse<String> post(String path, Map<String, String> values, boolean csrf) throws Exception {
            Map<String,String> data = new LinkedHashMap<>(values);
            if (csrf) data.put("_csrf", token);
            String body = data.entrySet().stream().map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8)).reduce((a,b) -> a + "&" + b).orElse("");
            return client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).header("Content-Type", "application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
        }
    }
    private Map<String,String> bookData(String title) {
        return new HashMap<>(Map.of("title", title, "listPrice", "19000", "isbn", "", "active", "true", "publicationDate", "2026-10-01"));
    }
    @Test void browserBookCreateEditSearchAndDeactivate() throws Exception {
        var browser = new Browser(); String title = marker();
        assertThat(browser.get("/books/new").body()).contains("도서 등록", "도서명", "정가 (원)");
        assertThat(browser.post("/books", bookData(title), true).statusCode()).isEqualTo(302);
        long id = bookId(title);
        assertThat(browser.get("/books?q=" + title).body()).contains(title, "19,000원");
        assertThat(browser.get("/books/" + id + "/edit").body()).contains("2026-10-01");
        var changed = bookData(title); changed.put("listPrice", "25000"); changed.put("id", "9223372036854775807");
        assertThat(browser.post("/books/" + id, changed, true).statusCode()).isEqualTo(302);
        assertThat(books.find(id).getListPrice()).isEqualTo(25000L);
        assertThat(browser.post("/books/" + id + "/deactivate", Map.of(), true).statusCode()).isEqualTo(302);
        assertThat(books.find(id).getActive()).isFalse();
    }
    @Test void browserPartnerCreateEditAndDeactivate() throws Exception {
        var browser = new Browser(); String code = marker(); browser.get("/partners/new");
        var data = new HashMap<>(Map.of("code",code,"name","시연 테스트 책방","type","BOOKSTORE","active","true","email","test@example.invalid"));
        assertThat(browser.post("/partners", data, true).statusCode()).isEqualTo(302);
        long id = jdbc.queryForObject("SELECT id FROM partners WHERE code=?", Long.class, code);
        assertThat(browser.get("/partners?q=" + code).body()).contains(code, "시연 테스트 책방");
        assertThat(browser.get("/partners/" + id + "/edit").body()).contains("거래처 수정", "거래처 코드", "사용 상태");
        data.put("type","DISTRIBUTOR");
        assertThat(browser.post("/partners/" + id, data, true).statusCode()).isEqualTo(302);
        assertThat(partners.find(id).getType()).isEqualTo("DISTRIBUTOR");
        assertThat(browser.post("/partners/" + id + "/deactivate", Map.of(), true).statusCode()).isEqualTo(302);
        assertThat(partners.find(id).getActive()).isFalse();
    }
    @Test void browserBookValidationAndDuplicateIsbnAreKoreanAndDoNotPersist() throws Exception {
        var browser = new Browser(); browser.get("/books/new");
        var data = bookData(" "); data.put("listPrice", "-1"); data.put("isbn", "9780000000001");
        var bad = browser.post("/books", data, true);
        assertThat(bad.statusCode()).isEqualTo(200);
        assertThat(bad.body()).contains("도서명을 입력하세요.", "정가는 0 이상이어야 합니다.", "유효한 13자리");
        data = bookData(marker()); data.put("isbn", "9780000000002");
        assertThat(browser.post("/books", data, true).body()).contains("이미 등록된 ISBN입니다.");
        assertThat(books.list(query(data.get("title"),1,"")).getTotal()).isZero();
        data.put("listPrice", "12.5"); data.put("publicationDate", "2026-02-30");
        assertThat(browser.post("/books", data, true).body()).contains("정가는 정수로 입력하세요.", "발행일은 올바른 날짜");
        data.put("listPrice", "19000"); data.put("publicationDate", "0001-01-01");
        assertThat(browser.post("/books", data, true).body()).contains("발행일은 1000-01-01부터");
    }
    @Test void browserPartnerValidationAndDuplicateCodePreserveForm() throws Exception {
        var browser = new Browser(); browser.get("/partners/new");
        var data = new HashMap<>(Map.of("code","bad code","name"," ","type","OTHER","active","true","email","invalid"));
        var bad = browser.post("/partners", data, true);
        assertThat(bad.statusCode()).isEqualTo(200);
        assertThat(bad.body()).contains("거래처명을 입력하세요.", "올바른 거래처 유형", "올바른 이메일");
        data.put("code","DEMO_STORE"); data.put("name","가상 책방"); data.put("type","BOOKSTORE"); data.put("email","");
        assertThat(browser.post("/partners", data, true).body()).contains("이미 등록된 거래처 코드입니다.");
    }
    @Test void duplicateUpdatesDoNotChangeExistingRecords() throws Exception {
        String title = marker(); long id = books.create(book(title));
        var browser = new Browser(); browser.get("/books/" + id + "/edit");
        var data = bookData(title); data.put("isbn", "9780000000002");
        assertThat(browser.post("/books/" + id, data, true).body()).contains("이미 등록된 ISBN입니다.");
        assertThat(books.find(id).getIsbn()).isNull();
        long partnerId = partners.create(partner(marker()));
        browser.get("/partners/" + partnerId + "/edit");
        assertThat(browser.post("/partners/" + partnerId, Map.of("code","DEMO_STORE","name","가상 책방","type","BOOKSTORE","active","true"), true).body()).contains("이미 등록된 거래처 코드입니다.");
        assertThat(partners.find(partnerId).getCode()).startsWith("TST_");
    }
    @Test void csrfIsSessionBoundAndGetCannotMutate() throws Exception {
        var browser = new Browser(); browser.get("/books/new");
        assertThat(browser.post("/books", bookData(marker()), false).statusCode()).isEqualTo(403);
        var other = new Browser(); other.get("/books/new"); other.token = browser.token;
        assertThat(other.post("/books", bookData(marker()), true).statusCode()).isEqualTo(403);
        assertThat(browser.get("/books/123/deactivate").statusCode()).isEqualTo(405);
    }
    @Test void xssIsEscapedAndBadPageParametersReturn400() throws Exception {
        String title = marker() + "<script>alert('x')</script>"; long id = books.create(book(title));
        var browser = new Browser();
        assertThat(browser.get("/books").body()).contains("&lt;script&gt;").doesNotContain("<script>alert(");
        assertThat(browser.get("/books/"+id+"/edit").body()).contains("&lt;script&gt;").doesNotContain("<script>alert(");
        assertThat(browser.get("/books?page=0").statusCode()).isEqualTo(400);
        assertThat(browser.get("/partners?page=abc").statusCode()).isEqualTo(400);
        assertThat(browser.get("/books?status=bad").statusCode()).isEqualTo(400);
        assertThat(browser.get("/books/9223372036854775807/edit").statusCode()).isEqualTo(404);
    }
}
