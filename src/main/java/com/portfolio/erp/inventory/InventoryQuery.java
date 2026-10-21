package com.portfolio.erp.inventory;

import com.portfolio.erp.common.PageQuery;
import jakarta.validation.constraints.*;

public class InventoryQuery extends PageQuery {
  @Min(value = 1, message = "{validation.book.required}")
  private Long bookId;

  public Long getBookId() {
    return bookId;
  }

  public void setBookId(Long value) {
    bookId = value;
  }
}
