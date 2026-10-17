package com.portfolio.erp.common;

import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class HomeController {
    private final JdbcTemplate jdbc;
    public HomeController(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    @GetMapping("/") public String home() { return "home"; }
    @GetMapping("/health") @ResponseBody public Map<String, String> health() {
        jdbc.queryForObject("SELECT 1", Integer.class);
        return Map.of("status", "UP", "database", "MariaDB");
    }
}
