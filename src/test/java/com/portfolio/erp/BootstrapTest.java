package com.portfolio.erp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@org.springframework.context.annotation.Import(SecurityTestUsers.class)
class BootstrapTest {
    @Autowired TestRestTemplate http;
    @org.springframework.boot.test.web.server.LocalServerPort int port;
    @Autowired JdbcTemplate jdbc;
    @Test void actualMariaDbConnectionAndHealth() throws Exception {
        assertThat(jdbc.queryForObject("SELECT VERSION()", String.class)).contains("MariaDB");
        try (var browser = new TestHttpBrowser(port)) {
            browser.login(); var response = browser.get("/health");
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).contains("UP", "MariaDB");
        }
    }
    @Test void jspCompilesAndRendersOverRealHttp() throws Exception {
        try (var browser = new TestHttpBrowser(port)) {
            browser.login(); var response = browser.get("/");
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).contains("출판 ERP Mini", "도서 관리").doesNotContain("<%@");
        }
        assertThat(http.getForEntity("/webjars/jquery/3.7.1/jquery.min.js", String.class).getStatusCode().value()).isEqualTo(200);
    }
    @Test void realLoginFailureRendersSafeKoreanJspWithoutCredentials() throws Exception {
        try (var browser = new TestHttpBrowser(port)) {
            browser.get("/login");
            String submitted="Private-wrong-test!";
            var failed=browser.post("/login",java.util.Map.of("username","missing_account","password",submitted));
            assertThat(failed.statusCode()).isEqualTo(401);
            assertThat(failed.body()).contains("LOGIN_FAILED", "로그인 정보를 확인할 수 없습니다.", "type=\"password\"")
                    .doesNotContain(submitted,"missing_account","BadCredentialsException");
        }
    }
    @Test void realSessionsRotateReplaceAndLogoutWithJspCsrfTokens() throws Exception {
        try(var first=new TestHttpBrowser(port); var second=new TestHttpBrowser(port)) {
            var initial=first.get("/login");
            String before=initial.headers().firstValue("Set-Cookie").orElseThrow();
            var logged=first.post("/login",java.util.Map.of("username","tst_admin","password",SecurityTestUsers.PASSWORD));
            assertThat(logged.statusCode()).isEqualTo(302);
            String after=logged.headers().firstValue("Set-Cookie").orElseThrow();
            assertThat(before.equals(after)).isFalse();
            assertThat(first.get("/").statusCode()).isEqualTo(200);
            second.login();
            assertThat(first.get("/books").statusCode()).isEqualTo(302);
            assertThat(second.post("/logout",java.util.Map.of()).statusCode()).isEqualTo(302);
            assertThat(second.get("/books").statusCode()).isEqualTo(302);
        }
    }

}
