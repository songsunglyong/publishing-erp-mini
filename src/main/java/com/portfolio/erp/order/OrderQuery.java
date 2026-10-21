package com.portfolio.erp.order;

import jakarta.validation.constraints.*;

public class OrderQuery {
  @Size(max = 100, message = "{validation.query.size}")
  private String q = "";

  @Pattern(regexp = "|DRAFT|CONFIRMED|FULFILLED|CANCELLED", message = "{validation.order.status}")
  private String status = "";

  @Min(value = 1, message = "{validation.page.min}")
  @Max(value = 1000000, message = "{validation.page.max}")
  private int page = 1;

  public String getQ() {
    return q;
  }

  public void setQ(String value) {
    q = value == null ? "" : value.trim();
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String value) {
    status = value == null ? "" : value;
  }

  public int getPage() {
    return page;
  }

  public void setPage(int value) {
    page = value;
  }

  public int getLimit() {
    return 10;
  }

  public int getOffset() {
    return (page - 1) * 10;
  }

  public String getPattern() {
    return "%" + q.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
  }
}
