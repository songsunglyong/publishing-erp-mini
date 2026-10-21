package com.portfolio.erp.order;

import java.time.LocalDateTime;
import java.util.List;

public class SalesOrder {
  private Long id;
  private String orderNo;
  private String requestKey;
  private Long partnerId;
  private String partnerName;
  private LocalDateTime orderedAt;
  private String status;
  private String notes;
  private Long totalAmount;
  private Long createdBy;
  private LocalDateTime fulfilledAt;
  private LocalDateTime updatedAt;
  private Long returnedAmount;
  private List<OrderItem> items;

  public Long getId() {
    return id;
  }

  public void setId(Long value) {
    id = value;
  }

  public String getOrderNo() {
    return orderNo;
  }

  public void setOrderNo(String value) {
    orderNo = value;
  }

  public String getRequestKey() {
    return requestKey;
  }

  public void setRequestKey(String value) {
    requestKey = value;
  }

  public Long getPartnerId() {
    return partnerId;
  }

  public void setPartnerId(Long value) {
    partnerId = value;
  }

  public String getPartnerName() {
    return partnerName;
  }

  public void setPartnerName(String value) {
    partnerName = value;
  }

  public LocalDateTime getOrderedAt() {
    return orderedAt;
  }

  public void setOrderedAt(LocalDateTime value) {
    orderedAt = value;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String value) {
    status = value;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String value) {
    notes = value;
  }

  public Long getTotalAmount() {
    return totalAmount;
  }

  public void setTotalAmount(Long value) {
    totalAmount = value;
  }

  public Long getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(Long value) {
    createdBy = value;
  }

  public LocalDateTime getFulfilledAt() {
    return fulfilledAt;
  }

  public void setFulfilledAt(LocalDateTime value) {
    fulfilledAt = value;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime value) {
    updatedAt = value;
  }

  public Long getReturnedAmount() {
    return returnedAmount;
  }

  public void setReturnedAmount(Long value) {
    returnedAmount = value;
  }

  public List<OrderItem> getItems() {
    return items;
  }

  public void setItems(List<OrderItem> value) {
    items = value;
  }
}
