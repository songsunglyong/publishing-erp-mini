package com.portfolio.erp;

import java.net.URI;
import java.net.http.*;
import java.util.Set;
import jakarta.servlet.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties="server.servlet.session.cookie.secure=true")
@ActiveProfiles("test")
class SecureCookieHttpTest {
    @LocalServerPort int port;
    @Autowired ServletContext context;
    @Test void realServletCookieIsSecureHttpOnlySameSiteAndCookieOnly() throws Exception {
        try(var http=HttpClient.newHttpClient()) {
            var response=http.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/login")).GET().build(),HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(200);
            String cookie=response.headers().allValues("Set-Cookie").stream().filter(value->value.startsWith("JSESSIONID=")).findFirst().orElseThrow();
            assertThat(cookie).contains("Secure", "HttpOnly", "SameSite=Lax");
            assertThat(response.body()).doesNotContain(";jsessionid=");
            assertThat(context.getEffectiveSessionTrackingModes()).isEqualTo(Set.of(SessionTrackingMode.COOKIE));
        }
    }
}
