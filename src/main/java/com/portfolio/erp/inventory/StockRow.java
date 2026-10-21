package com.portfolio.erp.inventory;


public class StockRow {
  private Long bookId;
  private String title;
  private String isbn;
  private Boolean active;
  private Integer quantityOnHand;

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

  public String getIsbn() {
    return isbn;
  }

  public void setIsbn(String value) {
    isbn = value;
  }

  public Boolean getActive() {
    return active;
  }

  public void setActive(Boolean value) {
    active = value;
  }

  public Integer getQuantityOnHand() {
    return quantityOnHand;
  }

  public void setQuantityOnHand(Integer value) {
    quantityOnHand = value;
  }
}
