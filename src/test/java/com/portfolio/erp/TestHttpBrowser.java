package com.portfolio.erp;

import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;
import static org.assertj.core.api.Assertions.assertThat;

class TestHttpBrowser implements AutoCloseable {
    final HttpClient client = HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
            .followRedirects(HttpClient.Redirect.NEVER).build();
    final int port;
    String token;
    TestHttpBrowser(int port) { this.port = port; }
    HttpResponse<String> get(String path) throws Exception {
        var result = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).GET().build(), HttpResponse.BodyHandlers.ofString());
        var match = Pattern.compile("name=\"_csrf\" value=\"([^\"]+)\"").matcher(result.body());
        if (match.find()) token = match.group(1);
        return result;
    }
    HttpResponse<String> post(String path, Map<String,String> values) throws Exception {
        Map<String,String> data = new LinkedHashMap<>(values); if (token != null) data.put("_csrf", token);
        String body = data.entrySet().stream().map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .reduce((a,b) -> a + "&" + b).orElse("");
        return client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }
    void login() throws Exception {
        assertThat(get("/login").statusCode()).isEqualTo(200);
        assertThat(post("/login", Map.of("username", "tst_admin", "password", SecurityTestUsers.PASSWORD)).statusCode()).isEqualTo(302);
        assertThat(get("/").statusCode()).isEqualTo(200);
    }
    @Override public void close() { client.close(); }
}
