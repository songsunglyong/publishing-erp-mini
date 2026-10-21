package com.portfolio.erp.common;

import jakarta.validation.constraints.*;

public class PageQuery {
    @Size(max = 100, message = "{validation.query.size}")
    private String q = "";
    @Pattern(regexp = "|true|false", message = "{validation.status}")
    private String status = "";
    @Min(value = 1, message = "{validation.page.min}")
    @Max(value = 1000000, message = "{validation.page.max}")
    private int page = 1;
    public String getQ() { return q; }
    public void setQ(String q) { this.q = q == null ? "" : q.trim(); }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status == null ? "" : status; }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public int getLimit() { return 10; }
    public int getOffset() { return (page - 1) * getLimit(); }
    public Boolean getActive() { return status.isEmpty() ? null : Boolean.valueOf(status); }
    public String getPattern() {
        return "%" + q.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
    }
}
