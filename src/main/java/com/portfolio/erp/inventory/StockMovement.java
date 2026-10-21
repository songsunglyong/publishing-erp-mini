package com.portfolio.erp.inventory;

import java.time.LocalDateTime;

public class StockMovement {
  private Long id;
  private Long bookId;
  private String title;
  private String type;
  private Integer quantity;
  private Integer balanceAfter;
  private String referenceType;
  private Long referenceId;
  private String operationKey;
  private Long actorId;
  private String note;
  private LocalDateTime createdAt;

  public Long getId() {
    return id;
  }

  public void setId(Long value) {
    id = value;
  }

  public Long getBookId() {
    return bookId;
  }

  public void setBookId(Long value) {
    bookId = value;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String value) {
    title = value;
  }

  public String getType() {
    return type;
  }

  public void setType(String value) {
    type = value;
  }

  public Integer getQuantity() {
    return quantity;
  }

  public void setQuantity(Integer value) {
    quantity = value;
  }

  public Integer getBalanceAfter() {
    return balanceAfter;
  }

  public void setBalanceAfter(Integer value) {
    balanceAfter = value;
  }

  public String getReferenceType() {
    return referenceType;
  }

  public void setReferenceType(String value) {
    referenceType = value;
  }

  public Long getReferenceId() {
    return referenceId;
  }

  public void setReferenceId(Long value) {
    referenceId = value;
  }

  public String getOperationKey() {
    return operationKey;
  }

  public void setOperationKey(String value) {
    operationKey = value;
  }

  public Long getActorId() {
    return actorId;
  }

  public void setActorId(Long value) {
    actorId = value;
  }

  public String getNote() {
    return note;
  }

  public void setNote(String value) {
    note = value;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime value) {
    createdAt = value;
  }
}
