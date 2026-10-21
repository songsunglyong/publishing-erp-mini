package com.portfolio.erp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"test","prod"})
class ProductionSecurityMvcTest {
    @Autowired MockMvc mvc;
    @Autowired ServerProperties server;
    @Test void productionRequiresHttpsAndKeepsSecureCookie() throws Exception {
        mvc.perform(get("/login").secure(false)).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("https://localhost/login"));
        mvc.perform(get("https://localhost/login").secure(true)).andExpect(status().isOk()).andExpect(view().name("auth/login"));
        assertThat(server.getServlet().getSession().getCookie().getSecure()).isTrue();
        assertThat(server.getServlet().getSession().getCookie().getHttpOnly()).isTrue();
        assertThat(server.getServlet().getSession().getTimeout()).isEqualTo(java.time.Duration.ofMinutes(30));
    }
}
