package com.portfolio.erp.order;

import java.time.LocalDateTime;

public class ReturnRecord {
  private Long id;
  private Long orderId;
  private String requestKey;
  private String reason;
  private Long totalAmount;
  private Long actorId;
  private LocalDateTime returnedAt;

  public Long getId() {
    return id;
  }

  public void setId(Long value) {
    id = value;
  }

  public Long getOrderId() {
    return orderId;
  }

  public void setOrderId(Long value) {
    orderId = value;
  }

  public String getRequestKey() {
    return requestKey;
  }

  public void setRequestKey(String value) {
    requestKey = value;
  }

  public String getReason() {
    return reason;
  }

  public void setReason(String value) {
    reason = value;
  }

  public Long getTotalAmount() {
    return totalAmount;
  }

  public void setTotalAmount(Long value) {
    totalAmount = value;
  }

  public Long getActorId() {
    return actorId;
  }

  public void setActorId(Long value) {
    actorId = value;
  }

  public LocalDateTime getReturnedAt() {
    return returnedAt;
  }

  public void setReturnedAt(LocalDateTime value) {
    returnedAt = value;
  }
}
