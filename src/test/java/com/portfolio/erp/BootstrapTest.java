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
class BootstrapTest {
    @Autowired TestRestTemplate http;
    @Autowired JdbcTemplate jdbc;
    @Test void actualMariaDbConnectionAndHealth() {
        assertThat(jdbc.queryForObject("SELECT VERSION()", String.class)).contains("MariaDB");
        var response = http.getForEntity("/health", String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("UP", "MariaDB");
    }
    @Test void jspCompilesAndRendersOverRealHttp() {
        var response = http.getForEntity("/", String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("출판 ERP Mini", "도서 관리").doesNotContain("<%@");
        assertThat(http.getForEntity("/webjars/jquery/3.7.1/jquery.min.js", String.class).getStatusCode().value()).isEqualTo(200);
    }
}
