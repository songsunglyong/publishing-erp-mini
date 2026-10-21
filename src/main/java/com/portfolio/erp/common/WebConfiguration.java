package com.portfolio.erp.common;

import java.util.Locale;
import org.springframework.context.annotation.*;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.FixedLocaleResolver;

/** CSRF and session checks now belong to the Spring Security filter chain. */
@Configuration
public class WebConfiguration {
    @Bean public LocaleResolver localeResolver() { return new FixedLocaleResolver(Locale.KOREAN); }
}
