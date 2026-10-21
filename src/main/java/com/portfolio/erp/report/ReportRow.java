package com.portfolio.erp.report;

import java.math.BigDecimal;

public class ReportRow {
  private String label;
  private BigDecimal grossAmount;
  private BigDecimal returnAmount;
  private BigDecimal netAmount;
  private Long shippedQuantity;
  private Long returnedQuantity;

  public String getLabel() {
    return label;
  }

  public void setLabel(String value) {
    label = value;
  }

  public BigDecimal getGrossAmount() {
    return grossAmount;
  }

  public void setGrossAmount(BigDecimal value) {
    grossAmount = value;
  }

  public BigDecimal getReturnAmount() {
    return returnAmount;
  }

  public void setReturnAmount(BigDecimal value) {
    returnAmount = value;
  }

  public BigDecimal getNetAmount() {
    return netAmount;
  }

  public void setNetAmount(BigDecimal value) {
    netAmount = value;
  }

  public Long getShippedQuantity() {
    return shippedQuantity;
  }

  public void setShippedQuantity(Long value) {
    shippedQuantity = value;
  }

  public Long getReturnedQuantity() {
    return returnedQuantity;
  }

  public void setReturnedQuantity(Long value) {
    returnedQuantity = value;
  }
}
