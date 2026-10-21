package com.portfolio.erp;

import java.time.*;
import java.util.concurrent.atomic.AtomicReference;
import com.portfolio.erp.book.*;
import com.portfolio.erp.security.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.*;
import org.springframework.boot.test.system.*;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.password.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(SecurityMvcTest.TimeConfiguration.class)
@ExtendWith(OutputCaptureExtension.class)
class SecurityMvcTest {
    static final String PASSWORD = "Synthetic-Security!42";
    @Autowired MockMvc mvc;
    @Autowired UserMapper users;
    @Autowired PasswordEncoder encoder;
    @Autowired JdbcTemplate jdbc;
    @Autowired TestClock clock;
    @Autowired SessionRegistry registry;
    @Autowired LiveSessions live;
    @Autowired BookService books;
    @Autowired AccountAdminService admin;
    @Autowired StaffService staff;
    long adminId, userId, otherId;
    @BeforeEach void prepare() {
        assertThat(jdbc.queryForObject("SELECT DATABASE()",String.class)).endsWith("_test");
        clock.now.set(Instant.parse("2026-10-20T00:00:00Z"));
        jdbc.update("DELETE FROM login_attempts");
        jdbc.update("DELETE FROM erp_users WHERE username LIKE 'sec!_%' ESCAPE '!'");
        adminId = account("sec_admin", "ADMIN"); userId = account("sec_user", "USER");
        otherId = account("sec_other", "USER"); account("sec_manager", "MANAGER");
    }
    @AfterEach void cleanup() {
        SecurityContextHolder.clearContext();
        for (var principal : registry.getAllPrincipals())
            for (var session : registry.getAllSessions(principal,true)) {
                live.invalidate(session.getSessionId()); registry.removeSessionInformation(session.getSessionId());
            }
        jdbc.update("DELETE FROM erp_users WHERE username LIKE 'sec!_%' ESCAPE '!'");
    }
    long account(String username,String role) {
        var account = new UserAccount(); account.setUsername(username); account.setDisplayName("가상 직원");
        account.setEnabled(true); account.setRole(role); account.setPasswordHash(encoder.encode(PASSWORD)); users.insert(account); return account.getId();
    }
    MockHttpSession login(String username) throws Exception {
        return (MockHttpSession)mvc.perform(post("/login").with(csrf()).param("username",username).param("password",PASSWORD))
                .andExpect(status().isFound()).andExpect(redirectedUrl("/")).andReturn().getRequest().getSession(false);
    }
    void expiredAccess(String id) throws Exception {
        mvc.perform(get("/books").accept("application/json").with(request -> {
            request.setRequestedSessionId(id); request.setRequestedSessionIdValid(false); return request;
        })).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("SESSION_EXPIRED"));
    }
    @Test void unauthenticatedErpAccessIsBlockedButLoginIsPublic() throws Exception {
        mvc.perform(get("/login")).andExpect(status().isOk()).andExpect(view().name("auth/login"));
        for(String path:new String[]{"/", "/books", "/partners", "/admin/users", "/employees/1", "/health", "/orders", "/inventory", "/reports/sales"})
            mvc.perform(get(path).accept("application/json")).andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("LOGIN_REQUIRED"));
        mvc.perform(get("/books")).andExpect(status().isFound()).andExpect(redirectedUrl("/login?reason=required"));
    }
    @Test void transactionEndpointsEnforceRolesAndCsrf() throws Exception {
        var user=login("sec_user");
        for(String path:new String[]{"/orders","/inventory","/inventory/movements","/reports/sales"})
            mvc.perform(get(path).session(user)).andExpect(status().isOk());
        for(String path:new String[]{"/orders/new","/inventory/receipts","/inventory/adjustments"})
            mvc.perform(get(path).session(user).accept("application/json")).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        for(String path:new String[]{"/orders","/orders/1/fulfill","/orders/1/returns","/inventory/receipts"})
            mvc.perform(post(path).session(user).with(csrf()).accept("application/json")).andExpect(status().isForbidden());
        var manager=login("sec_manager");
        mvc.perform(get("/inventory/adjustments").session(manager).accept("application/json")).andExpect(status().isForbidden());
        mvc.perform(post("/orders").session(manager).accept("application/json")).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("CSRF_INVALID"));
    }
    @Test void successfulLoginChangesSessionIdAndErasesPassword() throws Exception {
        var before = new MockHttpSession(); String old = before.getId();
        var result = mvc.perform(post("/login").session(before).with(csrf()).param("username","sec_user").param("password",PASSWORD))
                .andExpect(status().isFound()).andReturn();
        var session = result.getRequest().getSession(false);
        assertThat(session.getId()).isNotEqualTo(old); assertThat(session.getMaxInactiveInterval()).isEqualTo(1800);
        var context = (org.springframework.security.core.context.SecurityContext)session.getAttribute("SPRING_SECURITY_CONTEXT");
        assertThat(((ErpPrincipal)context.getAuthentication().getPrincipal()).getPassword()).isNull();
        mvc.perform(get("/books").session((MockHttpSession)session)).andExpect(status().isOk());
    }
    @Test void logoutInvalidatesServerSessionAndRequiresCsrf() throws Exception {
        var session = login("sec_user");
        mvc.perform(post("/logout").session(session).accept("application/json"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("CSRF_INVALID"));
        mvc.perform(post("/logout").session(session).with(csrf())).andExpect(status().isFound()).andExpect(redirectedUrl("/login?logout"));
        assertThat(session.isInvalid()).isTrue();
        mvc.perform(get("/books").accept("application/json")).andExpect(status().isUnauthorized());
    }
    @Test void wrongPasswordAndUnknownAccountHaveSameSafeMessage(CapturedOutput output) throws Exception {
        String message = null;
        for (String username:new String[]{"sec_user","sec_missing"}) {
            var result = mvc.perform(post("/login").with(csrf()).param("username",username).param("password","private-wrong-password")
                    .accept("application/json")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("LOGIN_FAILED")).andReturn();
            var body = new tools.jackson.databind.ObjectMapper().readTree(result.getResponse().getContentAsString());
            if (message == null) message = body.get("message").asText(); else assertThat(body.get("message").asText()).isEqualTo(message);
            assertThat(result.getResponse().getContentAsString()).doesNotContain(username,"BadCredentialsException","private-wrong-password");
        }
        assertThat(output.getAll()).doesNotContain("private-wrong-password");
    }
    @Test void newLoginImmediatelyInvalidatesOldSession() throws Exception {
        var first = login("sec_user"); String old = first.getId();
        var second = login("sec_user");
        assertThat(first.isInvalid()).isTrue();
        assertThat(registry.getAllSessions(usersPrincipal("sec_user"),false)).hasSize(1);
        expiredAccess(old);
        mvc.perform(get("/partners").session(second)).andExpect(status().isOk());
    }
    @Test void simultaneousLoginsLeaveExactlyOneUsableSession() throws Exception {
        try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> login("sec_user"));
            var second = executor.submit(() -> login("sec_user"));
            var sessions = java.util.Arrays.asList(first.get(),second.get());
            assertThat(registry.getAllSessions(usersPrincipal("sec_user"),false)).hasSize(1);
            var usable = sessions.stream().filter(session -> session != null && !session.isInvalid()).toList();
            assertThat(usable).hasSize(1);
            mvc.perform(get("/books").session(usable.getFirst())).andExpect(status().isOk());
        }
    }
    @Test void idleExpiryIsThirtyMinutesWithActivityRefresh() throws Exception {
        var session = login("sec_user");
        clock.advance(Duration.ofMinutes(29)); mvc.perform(get("/books").session(session)).andExpect(status().isOk());
        clock.advance(Duration.ofMinutes(30));
        mvc.perform(get("/books").session(session).accept("application/json"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("SESSION_EXPIRED"));
        assertThat(session.isInvalid()).isTrue();
    }
    @Test void absoluteExpiryIsEightHoursDespiteContinuousActivity() throws Exception {
        var session = login("sec_user"); Object original = session.getAttribute(SessionGuardFilter.LOGIN_AT);
        for (int i=0;i<23;i++) { clock.advance(Duration.ofMinutes(20)); mvc.perform(get("/books").session(session)).andExpect(status().isOk()); }
        assertThat(session.getAttribute(SessionGuardFilter.LOGIN_AT)).isEqualTo(original);
        clock.advance(Duration.ofMinutes(20));
        mvc.perform(get("/books").session(session).accept("application/json")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("SESSION_EXPIRED"));
    }
    @Test void missingAndForeignCsrfAreForbiddenBeforeMutation() throws Exception {
        var session = login("sec_manager"); long before = jdbc.queryForObject("SELECT COUNT(*) FROM books",Long.class);
        for (String value:new String[]{"","foreign-token"})
            mvc.perform(post("/books").session(session).param("_csrf",value).param("title","unsafe").param("listPrice","100")
                    .accept("application/json")).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("CSRF_INVALID"));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM books",Long.class)).isEqualTo(before);
    }
    @Test void userCannotOpenAdminOrWriteMasterData() throws Exception {
        var session = login("sec_user");
        mvc.perform(get("/admin/users").session(session).accept("application/json")).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        mvc.perform(get("/books/new").session(session)).andExpect(status().isForbidden());
        mvc.perform(post("/books").session(session).with(csrf()).param("title","unauthorized").param("listPrice","100"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/partners").session(session)).andExpect(status().isOk());
    }
    @Test void serviceMethodsAlsoEnforceRoles() {
        authenticate("sec_user");
        assertThatThrownBy(() -> admin.list()).isInstanceOf(AccessDeniedException.class);
        var form = new BookForm(); form.setTitle("권한 검사"); form.setListPrice(100L);
        assertThatThrownBy(() -> books.create(form)).isInstanceOf(AccessDeniedException.class);
        authenticate("sec_manager"); assertThat(books.list(new com.portfolio.erp.common.PageQuery())).isNotNull();
        assertThatThrownBy(() -> admin.list()).isInstanceOf(AccessDeniedException.class);
    }
    @Test void otherEmployeeObjectCannotBeReadEvenThroughService() throws Exception {
        var session = login("sec_user");
        mvc.perform(get("/employees/"+userId).session(session)).andExpect(status().isOk());
        mvc.perform(get("/employees/"+otherId).session(session).accept("application/json"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        authenticate("sec_user"); assertThatThrownBy(() -> staff.find(otherId)).isInstanceOf(AccessDeniedException.class);
        authenticate("sec_admin"); assertThat(staff.find(otherId).getId()).isEqualTo(otherId);
    }
    @Test void blockingAccountRevokesSessionsAndNewLogin() throws Exception {
        var target = login("sec_user"); String old = target.getId(); var administrator = login("sec_admin");
        mvc.perform(post("/admin/users/"+userId+"/block").session(administrator).with(csrf())).andExpect(status().isFound());
        assertThat(target.isInvalid()).isTrue(); expiredAccess(old);
        mvc.perform(post("/login").with(csrf()).param("username","sec_user").param("password",PASSWORD).accept("application/json"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("LOGIN_FAILED"));
    }
    @Test void databaseRevocationAlsoStopsSessionsWithoutRegistryNotification() throws Exception {
        var session = login("sec_user"); users.block(userId);
        mvc.perform(get("/books").session(session).accept("application/json")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("SESSION_EXPIRED"));
    }
    @Test void passwordChangeRevokesSessionAndOldPassword() throws Exception {
        var session = login("sec_user"); String replacement="Changed-Synthetic!55";
        mvc.perform(post("/account/password").session(session).with(csrf()).param("currentPassword",PASSWORD).param("newPassword",replacement))
                .andExpect(status().isFound()).andExpect(redirectedUrl("/login?passwordChanged"));
        assertThat(session.isInvalid()).isTrue();
        assertThat(encoder.matches(replacement,users.findById(userId).getPasswordHash())).isTrue();
        mvc.perform(post("/login").with(csrf()).param("username","sec_user").param("password",PASSWORD).accept("application/json"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/login").with(csrf()).param("username","sec_user").param("password",replacement)).andExpect(status().isFound());
    }
    @Test void passwordPolicyAndWrongCurrentPasswordNeverEchoSecrets(CapturedOutput output) throws Exception {
        var session = login("sec_user"); String secret="Wrong-private-password!";
        var result = mvc.perform(post("/account/password").session(session).with(csrf()).param("currentPassword",secret).param("newPassword","short"))
                .andExpect(status().isBadRequest()).andExpect(view().name("auth/password")).andReturn();
        assertThat(result.getModelAndView().getModel()).doesNotContainKeys("currentPassword","newPassword");
        assertThat(output.getAll()).doesNotContain(secret,PASSWORD,"JSESSIONID", "sessionId=");
        mvc.perform(post("/account/password").session(session).with(csrf()).param("currentPassword",PASSWORD).param("newPassword","short"))
                .andExpect(status().isBadRequest());
        assertThat(encoder.matches(PASSWORD,users.findById(userId).getPasswordHash())).isTrue();
    }
    @Test void repeatedFailuresAreLimitedForKnownAndUnknownAccounts() throws Exception {
        for (String username:new String[]{"sec_user","sec_unknown"}) {
            for(int i=0;i<5;i++) mvc.perform(post("/login").with(csrf()).param("username",username).param("password","wrong").accept("application/json"))
                    .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("LOGIN_FAILED"));
            mvc.perform(post("/login").with(csrf()).param("username",username).param("password",PASSWORD).accept("application/json"))
                    .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("LOGIN_FAILED"));
        }
        clock.advance(Duration.ofMinutes(15)); login("sec_user");
    }
    @Test void perIpLimitCannotBeBypassedWithChangingUsernames() throws Exception {
        for (int i=0;i<31;i++) mvc.perform(post("/login").with(csrf()).param("username","sec_unknown_"+i).param("password","wrong")
                .accept("application/json")).andExpect(status().isUnauthorized());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM login_attempts",Integer.class)).isEqualTo(31);
        mvc.perform(post("/login").with(csrf()).param("username","sec_admin").param("password",PASSWORD).accept("application/json"))
                .andExpect(status().isUnauthorized());
    }
    @Test void adminCanCreateHashedAccountsButCannotBlockSelf() throws Exception {
        var session = login("sec_admin");
        mvc.perform(post("/admin/users").session(session).with(csrf()).param("username","sec_created").param("displayName","신규 가상 직원")
                .param("role","MANAGER").param("password",PASSWORD)).andExpect(status().isFound());
        var account = users.findByUsername("sec_created");
        assertThat(account.getPasswordHash()).startsWith("{bcrypt}").doesNotContain(PASSWORD);
        mvc.perform(post("/admin/users/"+adminId+"/block").session(session).with(csrf()).accept("application/json"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("BUSINESS_RULE"));
    }
    @Test void delegationAndUtf8PasswordLimitAreExplicit() {
        assertThat(encoder).isInstanceOf(DelegatingPasswordEncoder.class);
        assertThatThrownBy(() -> PasswordPolicy.verify("가".repeat(25))).isInstanceOf(com.portfolio.erp.common.error.UserFacingException.class);
        assertThatCode(() -> PasswordPolicy.verify("가".repeat(24))).doesNotThrowAnyException();
    }
    @Test void invalidSessionAndUrlSessionIdsAreRejected() throws Exception {
        expiredAccess("synthetic-old-session");
        mvc.perform(get("/books;jsessionid=synthetic-id").accept("application/json"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }
    ErpPrincipal usersPrincipal(String username) { var principal = new ErpPrincipal(users.findByUsername(username)); principal.eraseCredentials(); return principal; }
    void authenticate(String username) { var principal=usersPrincipal(username); SecurityContextHolder.getContext().setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(principal,null,principal.getAuthorities())); }
    static class TestClock extends Clock {
        final AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-10-20T00:00:00Z"));
        void advance(Duration duration) { now.updateAndGet(value->value.plus(duration)); }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now.get(); }
    }
    @TestConfiguration static class TimeConfiguration { @Bean @Primary TestClock testClock() { return new TestClock(); } }
}
