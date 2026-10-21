package com.portfolio.erp.security;

import java.time.Clock;
import java.time.Instant;
import jakarta.servlet.DispatcherType;
import com.portfolio.erp.common.error.*;
import org.springframework.context.annotation.*;
import org.springframework.core.env.*;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.core.session.*;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.savedrequest.NullRequestCache;
import org.springframework.security.web.session.HttpSessionEventPublisher;

@Configuration
@EnableMethodSecurity
public class SecurityConfiguration {
    @Bean public org.springframework.security.web.context.SecurityContextRepository securityContexts() {
        var sessions = new org.springframework.security.web.context.HttpSessionSecurityContextRepository();
        sessions.setDisableUrlRewriting(true);
        return new org.springframework.security.web.context.DelegatingSecurityContextRepository(
                new org.springframework.security.web.context.RequestAttributeSecurityContextRepository(), sessions);
    }
    @Bean public Clock securityClock() { return Clock.systemUTC(); }
    @Bean public PasswordEncoder passwordEncoder() { return PasswordEncoderFactories.createDelegatingPasswordEncoder(); }
    @Bean public SessionRegistry sessionRegistry() { return new SessionRegistryImpl(); }
    @Bean public LiveSessions liveSessions() { return new LiveSessions(); }
    @Bean public org.springframework.boot.web.servlet.ServletListenerRegistrationBean<LiveSessions> liveSessionEvents(LiveSessions sessions) {
        return new org.springframework.boot.web.servlet.ServletListenerRegistrationBean<>(sessions);
    }
    @Bean public HttpSessionEventPublisher sessionEvents() { return new HttpSessionEventPublisher(); }
    @Bean public WebSecurityCustomizer safeFirewall(SecurityResponses responses) {
        return web -> web.requestRejectedHandler((request, response, ex) -> responses.send(request, response, ErrorCode.BAD_REQUEST));
    }
    @Bean public SecurityFilterChain security(HttpSecurity http, MybatisUserDetailsService details,
            PasswordEncoder encoder, SessionRegistry registry, Clock clock, UserMapper users,
            LoginAttempts attempts, SecurityResponses responses, UserErrors errors, Environment env, LiveSessions live, org.springframework.security.web.context.SecurityContextRepository contexts) throws Exception {
        if (env.acceptsProfiles(Profiles.of("prod")) && (env.acceptsProfiles(Profiles.of("local", "demo", "container"))
                || !env.getProperty("server.servlet.session.cookie.secure", Boolean.class, true)))
            throw new IllegalStateException("production_cookie_configuration_invalid");
        var provider = new DaoAuthenticationProvider(details);
        provider.setPasswordEncoder(encoder);
        provider.setHideUserNotFoundExceptions(true);
        http.authenticationProvider(provider)
            .securityContext(context -> context.securityContextRepository(contexts))
            .authorizeHttpRequests(auth -> auth
                .dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.FORWARD).permitAll()
                .requestMatchers("/login", "/error", "/css/**", "/js/**", "/webjars/**", "/favicon.ico").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/books/new", "/partners/new", "/books/*/edit", "/partners/*/edit").hasAnyRole("ADMIN", "MANAGER")
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/books", "/books/**", "/partners", "/partners/**").hasAnyRole("ADMIN", "MANAGER")
                .requestMatchers("/inventory/adjustments").hasRole("ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/orders/new", "/inventory/receipts").hasAnyRole("ADMIN", "MANAGER")
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/orders", "/orders/**", "/inventory/**").hasAnyRole("ADMIN", "MANAGER")
                .anyRequest().authenticated())
            .requestCache(cache -> cache.requestCache(new NullRequestCache()))
            .csrf(Customizer.withDefaults())
            .formLogin(login -> login.loginPage("/login").loginProcessingUrl("/login")
                .successHandler((request, response, authentication) -> {
                    Instant now = clock.instant();
                    // Serialize completion of simultaneous logins. The last successful login wins.
                    synchronized (registry) {
                        var current = request.getSession();
                        registry.registerNewSession(current.getId(), authentication.getPrincipal());
                        for (var old : registry.getAllSessions(authentication.getPrincipal(), true)) {
                            if (!old.getSessionId().equals(current.getId())) {
                                old.expireNow(); live.invalidate(old.getSessionId());
                                registry.removeSessionInformation(old.getSessionId());
                            }
                        }
                        current.setAttribute(SessionGuardFilter.LOGIN_AT, now);
                        current.setAttribute(SessionGuardFilter.LAST_SEEN, now);
                        current.setMaxInactiveInterval(1800);
                        live.track(current);
                        // A competing successful login may have invalidated the earlier session before this handler.
                        contexts.saveContext(org.springframework.security.core.context.SecurityContextHolder.getContext(), request, response);
                    }
                    try { attempts.succeeded(authentication.getName()); }
                    catch (RuntimeException ex) {
                        responses.clear(request, response);
                        responses.send(request, response, ErrorCode.SERVICE_UNAVAILABLE); return;
                    }
                    response.sendRedirect("/");
                })
                .failureHandler((request, response, ex) -> {
                    ErrorCode code = ex instanceof org.springframework.security.authentication.InternalAuthenticationServiceException
                            ? errors.classify(ex) : ErrorCode.LOGIN_FAILED;
                    responses.send(request, response, code);
                }))
            .sessionManagement(session -> session
                .sessionFixation(fixation -> fixation.changeSessionId())
                .maximumSessions(1).maxSessionsPreventsLogin(false).sessionRegistry(registry)
                .expiredSessionStrategy(event -> responses.send(event.getRequest(), event.getResponse(), ErrorCode.SESSION_EXPIRED)))
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, ex) -> responses.send(request, response, ErrorCode.LOGIN_REQUIRED))
                .accessDeniedHandler((request, response, ex) -> responses.send(request, response,
                    ex instanceof org.springframework.security.web.csrf.CsrfException ? ErrorCode.CSRF_INVALID : ErrorCode.ACCESS_DENIED)))
            .logout(logout -> logout.logoutUrl("/logout").invalidateHttpSession(true).clearAuthentication(true)
                .addLogoutHandler((request, response, authentication) -> live.forget(request.getSession(false)))
                .deleteCookies("JSESSIONID").logoutSuccessUrl("/login?logout"))
            .addFilterAfter(new SessionGuardFilter(clock, registry, users, responses), SecurityContextHolderFilter.class)
            .addFilterBefore(new LoginAttemptFilter(attempts, responses, errors), UsernamePasswordAuthenticationFilter.class);
        if (env.acceptsProfiles(Profiles.of("prod"))) http.redirectToHttps(Customizer.withDefaults());
        return http.build();
    }
}
