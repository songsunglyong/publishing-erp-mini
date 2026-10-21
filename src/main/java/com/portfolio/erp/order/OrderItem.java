package com.portfolio.erp.order;


public class OrderItem {
  private Long id;
  private Long orderId;
  private Long bookId;
  private String bookTitle;
  private Integer quantity;
  private Long unitPrice;
  private Long lineTotal;
  private Integer returnedQuantity;

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

  public Long getBookId() {
    return bookId;
  }

  public void setBookId(Long value) {
    bookId = value;
  }

  public String getBookTitle() {
    return bookTitle;
  }

  public void setBookTitle(String value) {
    bookTitle = value;
  }

  public Integer getQuantity() {
    return quantity;
  }

  public void setQuantity(Integer value) {
    quantity = value;
  }

  public Long getUnitPrice() {
    return unitPrice;
  }

  public void setUnitPrice(Long value) {
    unitPrice = value;
  }

  public Long getLineTotal() {
    return lineTotal;
  }

  public void setLineTotal(Long value) {
    lineTotal = value;
  }

  public Integer getReturnedQuantity() {
    return returnedQuantity;
  }

  public void setReturnedQuantity(Integer value) {
    returnedQuantity = value;
  }
}
