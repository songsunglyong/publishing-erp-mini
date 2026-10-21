package com.portfolio.erp;

import com.portfolio.erp.book.*;
import com.portfolio.erp.partner.*;
import com.portfolio.erp.common.*;
import com.portfolio.erp.common.error.*;
import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.system.*;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Import;
import org.springframework.dao.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.*;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers={BookController.class, PartnerController.class, HomeController.class, SafeErrorController.class},
    excludeAutoConfiguration={org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class})
@Import({WebConfiguration.class, UserErrors.class, TraceIdFilter.class, UserErrorMvcTest.FailureController.class})
@ExtendWith(OutputCaptureExtension.class)
class UserErrorMvcTest {
    @Autowired MockMvc mvc;
    @Autowired MessageSource messages;
    @MockitoBean BookService books;
    @MockitoBean PartnerService partners;
    @MockitoBean JdbcTemplate jdbc;
    private MockHttpSession session;
    @BeforeEach void session() { session = new MockHttpSession(); session.setAttribute("csrfToken", "test-token"); }
    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder bookPost(String path) {
        return post(path).session(session).param("_csrf", "test-token").param("title", "입력한 제목")
                .param("listPrice", "18000").param("active", "true");
    }
    private ErrorNotice notice(MvcResult result) { return (ErrorNotice) result.getRequest().getAttribute("errorNotice"); }

    @Test void invalidFormRetainsValuesAndGivesKoreanCorrection() throws Exception {
        var result = mvc.perform(bookPost("/books").param("isbn", "bad"))
                .andExpect(status().isOk()).andExpect(view().name("book/form"))
                .andExpect(model().attributeHasFieldErrors("form", "isbn")).andReturn();
        assertThat(notice(result).getCode()).isEqualTo("VALIDATION_FAILED");
        assertThat(((BookForm) result.getModelAndView().getModel().get("form")).getTitle()).isEqualTo("입력한 제목");
        BindingResult binding = (BindingResult) result.getModelAndView().getModel().get(BindingResult.MODEL_KEY_PREFIX + "form");
        assertThat(binding.getFieldError("isbn").getDefaultMessage()).contains("ISBN 번호를 확인");
        verifyNoInteractions(books);
    }
    @Test void invalidNumberAndDateKeepRawValuesWithSafeMessages() throws Exception {
        var result = mvc.perform(post("/books").session(session).param("_csrf", "test-token")
                .param("title", "유지할 제목").param("listPrice", "12.5").param("publicationDate", "2026-02-30"))
                .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("form", "listPrice", "publicationDate")).andReturn();
        BindingResult binding = (BindingResult) result.getModelAndView().getModel().get(BindingResult.MODEL_KEY_PREFIX + "form");
        assertThat(binding.getFieldError("listPrice").getRejectedValue()).isEqualTo("12.5");
        assertThat(messages.getMessage(binding.getFieldError("listPrice"), java.util.Locale.KOREAN)).contains("정수로 입력").doesNotContain("NumberFormatException");
    }
    @Test void duplicateIsbnKeepsEditTargetAndInput() throws Exception {
        when(books.find(25)).thenReturn(new Book());
        doThrow(new DuplicateKeyException("SQL secret-password")).when(books).update(eq(25L), any());
        var result = mvc.perform(bookPost("/books/25")).andExpect(status().isOk())
                .andExpect(view().name("book/form")).andExpect(model().attribute("entityId", 25L))
                .andExpect(model().attributeHasFieldErrors("form", "isbn")).andReturn();
        assertThat(notice(result).getCode()).isEqualTo("DUPLICATE_ISBN");
        assertThat(notice(result).getMessage()).contains("다른 ISBN").doesNotContain("SQL");
    }
    @Test void duplicatePartnerCodeAndBusinessRuleHaveDifferentCodes() throws Exception {
        when(partners.create(any())).thenThrow(new DuplicateKeyException("SQL account-secret"));
        var result = mvc.perform(post("/partners").session(session).param("_csrf", "test-token")
                .param("code", "STORE_01").param("name", "입력한 책방").param("type", "BOOKSTORE"))
                .andExpect(view().name("partner/form")).andExpect(model().attributeHasFieldErrors("form", "code")).andReturn();
        assertThat(notice(result).getCode()).isEqualTo("DUPLICATE_PARTNER_CODE");
        assertThat(((PartnerForm)result.getModelAndView().getModel().get("form")).getName()).isEqualTo("입력한 책방");
        when(books.create(any())).thenThrow(new UserFacingException(ErrorCode.BUSINESS_RULE));
        var business = mvc.perform(bookPost("/books")).andExpect(view().name("book/form")).andReturn();
        assertThat(notice(business).getCode()).isEqualTo("BUSINESS_RULE");
    }
    @Test void databaseOutageKeepsFormAndDoesNotLeakOrLogSecrets(CapturedOutput output) throws Exception {
        String secret = "password=S3cr3t! SELECT email FROM users <script>alert(1)</script>";
        when(books.create(any())).thenThrow(new DataAccessResourceFailureException(secret));
        var result = mvc.perform(bookPost("/books").header("X-Trace-Id", "untrusted-id"))
                .andExpect(status().isServiceUnavailable()).andExpect(view().name("book/form"))
                .andExpect(header().string("Cache-Control", "no-store")).andReturn();
        var notice = notice(result);
        assertThat(notice.getCode()).isEqualTo("SERVICE_UNAVAILABLE");
        assertThat(notice.getMessage()).contains("반영 여부", "운영 담당자").doesNotContain(secret);
        assertThat(notice.getTraceId()).matches("[0-9a-f-]{36}");
        assertThat(result.getResponse().getHeader("X-Trace-Id")).isEqualTo(notice.getTraceId());
        assertThat(output.getAll()).contains(notice.getTraceId(), "DataAccessResourceFailureException").doesNotContain(secret, "S3cr3t!");
        assertThat(((BookForm)result.getModelAndView().getModel().get("form")).getTitle()).isEqualTo("입력한 제목");
    }
    @Test void unknownFailureIsGenericAndRetainsForm() throws Exception {
        when(books.create(any())).thenThrow(new IllegalStateException("SQL private detail"));
        var result = mvc.perform(bookPost("/books")).andExpect(status().isInternalServerError())
                .andExpect(view().name("book/form")).andReturn();
        assertThat(notice(result).getCode()).isEqualTo("SERVER_ERROR");
        assertThat(notice(result).getMessage()).doesNotContain("SQL", "private");
    }
    @Test void missingDataBadIdWrongMethodAndUnknownPathUseStandardCodes() throws Exception {
        when(books.find(99)).thenThrow(new NotFoundException());
        mvc.perform(get("/books/99/edit").accept("application/json"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("NOT_FOUND"));
        mvc.perform(get("/books/not-number/edit").accept("application/json"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("BAD_REQUEST"));
        mvc.perform(get("/books/99/deactivate").accept("application/json"))
                .andExpect(status().isMethodNotAllowed()).andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
        mvc.perform(get("/unknown-page").accept("application/json"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }
    @Test void invalidSearchRetainsSearchTextAndNeverQueriesDatabase() throws Exception {
        mvc.perform(get("/partners").param("q", "유지할 검색어").param("page", "abc"))
                .andExpect(status().isBadRequest()).andExpect(view().name("partner/list"))
                .andExpect(model().attribute("query", hasProperty("q", equalTo("유지할 검색어"))));
        verifyNoInteractions(partners);
    }
    @ParameterizedTest @EnumSource(value=ErrorCode.class, names={"LOGIN_FAILED", "LOGIN_REQUIRED", "SESSION_EXPIRED", "ACCESS_DENIED", "BUSINESS_RULE"})
    void futureAuthenticationAndBusinessAdaptersHaveKoreanContract(ErrorCode code) throws Exception {
        mvc.perform(get("/_test/failure").param("code", code.name()).accept("application/json").header("Accept-Language", "en"))
                .andExpect(status().is(code.getStatus())).andExpect(jsonPath("$.code").value(code.name()))
                .andExpect(jsonPath("$.message").value(messages.getMessage(code.getMessageKey(), null, java.util.Locale.KOREAN)))
                .andExpect(jsonPath("$.traceId").isNotEmpty()).andExpect(jsonPath("$.exception").doesNotExist());
    }
    @Test void loginFailureNeverNamesUnknownAccount() throws Exception {
        mvc.perform(get("/_test/failure").param("code", "LOGIN_FAILED").accept("application/json"))
                .andExpect(jsonPath("$.message").value(messages.getMessage("error.LOGIN_FAILED", null, java.util.Locale.KOREAN)));
    }
    @Test void healthFailureIsJsonWithoutTechnicalDetails(CapturedOutput output) throws Exception {
        when(jdbc.queryForObject(anyString(), eq(Integer.class))).thenThrow(new DataAccessResourceFailureException("SQL password=hidden-value"));
        mvc.perform(get("/health")).andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("SERVICE_UNAVAILABLE"))
                .andExpect(content().string(not(containsString("hidden-value"))));
        assertThat(output.getAll()).doesNotContain("hidden-value");
    }
    @Test void containerFallbackIgnoresExceptionAndMessageAttributes() throws Exception {
        mvc.perform(get("/error").requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 500)
                .requestAttr(RequestDispatcher.ERROR_EXCEPTION, new RuntimeException("SQL password=secret"))
                .requestAttr(RequestDispatcher.ERROR_MESSAGE, "<script>alert(1)</script>").accept("application/json"))
                .andExpect(status().isInternalServerError()).andExpect(jsonPath("$.code").value("SERVER_ERROR"))
                .andExpect(content().string(not(containsString("password")))).andExpect(content().string(not(containsString("<script>"))));
    }
    @Test void wrappedJdbcFailureAndUnknownConstraintStayDistinct() throws Exception {
        when(books.create(any())).thenThrow(new org.mybatis.spring.MyBatisSystemException("jdbc_failure",
                new java.sql.SQLNonTransientConnectionException("password=hidden-db-value", "08001")));
        var unavailable = mvc.perform(bookPost("/books")).andExpect(status().isServiceUnavailable()).andReturn();
        assertThat(notice(unavailable).getCode()).isEqualTo("SERVICE_UNAVAILABLE");
        doThrow(new DataIntegrityViolationException("unknown SQL constraint")).when(books).create(any());
        var unknown = mvc.perform(bookPost("/books")).andExpect(status().isInternalServerError()).andReturn();
        assertThat(notice(unknown).getCode()).isEqualTo("SERVER_ERROR");
    }
    @Test void permissionFailureInFormRetainsForbiddenStatus() throws Exception {
        when(books.create(any())).thenThrow(new UserFacingException(ErrorCode.ACCESS_DENIED));
        var result = mvc.perform(bookPost("/books")).andExpect(status().isForbidden()).andReturn();
        assertThat(notice(result).getCode()).isEqualTo("ACCESS_DENIED");
    }
    @Test void everyCodeHasKoreanMessage() {
        for (var code : ErrorCode.values()) assertThat(messages.getMessage(code.getMessageKey(), null, java.util.Locale.KOREAN)).matches(".*[가-힣].*");
    }
    @RestController
    static class FailureController {
        @GetMapping("/_test/failure") void fail(@RequestParam ErrorCode code) { throw new UserFacingException(code); }
    }
}
