package com.portfolio.erp.common;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfiguration implements WebMvcConfigurer {
    @Override public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                var session = request.getSession();
                String token;
                synchronized (session) {
                    token = (String) session.getAttribute("csrfToken");
                    if (token == null) {
                        token = UUID.randomUUID().toString();
                        session.setAttribute("csrfToken", token);
                    }
                }
                request.setAttribute("csrfToken", token);
                if (!"GET".equals(request.getMethod()) && !"HEAD".equals(request.getMethod())) {
                    String supplied = request.getParameter("_csrf");
                    if (supplied == null) supplied = request.getHeader("X-CSRF-TOKEN");
                    if (supplied == null || !MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8), supplied.getBytes(StandardCharsets.UTF_8))) {
                        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "화면을 새로고침한 뒤 다시 시도하세요.");
                    }
                }
                response.setHeader("X-Content-Type-Options", "nosniff");
                return true;
            }
        }).addPathPatterns("/books", "/books/**", "/partners", "/partners/**");
    }
}
