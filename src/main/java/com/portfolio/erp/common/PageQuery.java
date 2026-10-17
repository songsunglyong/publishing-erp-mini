package com.portfolio.erp.common;

import jakarta.validation.constraints.*;

public class PageQuery {
    @Size(max = 100, message = "검색어는 100자 이하여야 합니다.")
    private String q = "";
    @Pattern(regexp = "|true|false", message = "올바른 상태를 선택하세요.")
    private String status = "";
    @Min(value = 1, message = "페이지는 1 이상이어야 합니다.")
    @Max(value = 1000000, message = "페이지 범위를 초과했습니다.")
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
