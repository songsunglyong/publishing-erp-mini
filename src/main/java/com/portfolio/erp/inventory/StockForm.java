package com.portfolio.erp.inventory;

import jakarta.validation.constraints.*;

public class StockForm {
  @NotNull(message = "{validation.book.required}")
  @Min(value = 1, message = "{validation.book.required}")
  private Long bookId;

  @NotNull(message = "{validation.stock.quantity}")
  @Min(value = -1000000, message = "{validation.stock.quantity}")
  @Max(value = 1000000, message = "{validation.stock.quantity}")
  private Integer quantity;

  @NotBlank(message = "{validation.reason}")
  @Size(max = 500, message = "{validation.note}")
  private String note;

  @NotBlank(message = "{validation.request.key}")
  private String requestKey;

  public Long getBookId() {
    return bookId;
  }

  public void setBookId(Long value) {
    bookId = value;
  }

  public Integer getQuantity() {
    return quantity;
  }

  public void setQuantity(Integer value) {
    quantity = value;
  }

  public String getNote() {
    return note;
  }

  public void setNote(String value) {
    note = value;
  }

  public String getRequestKey() {
    return requestKey;
  }

  public void setRequestKey(String value) {
    requestKey = value;
  }
}
