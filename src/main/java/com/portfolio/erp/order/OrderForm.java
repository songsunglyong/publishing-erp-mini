package com.portfolio.erp.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;

public class OrderForm {
  @NotNull(message = "{validation.partner.required}")
  @Min(value = 1, message = "{validation.partner.required}")
  private Long partnerId;

  @Size(max = 500, message = "{validation.note}")
  private String notes;

  @NotBlank(message = "{validation.request.key}")
  private String requestKey;

  @Valid
  @NotEmpty(message = "{validation.order.items}")
  @Size(max = 20, message = "{validation.order.items}")
  private List<@NotNull(message = "{validation.order.items}") Line> items =
      new ArrayList<>(List.of(new Line()));

  public Long getPartnerId() {
    return partnerId;
  }

  public void setPartnerId(Long value) {
    partnerId = value;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String value) {
    notes = value;
  }

  public String getRequestKey() {
    return requestKey;
  }

  public void setRequestKey(String value) {
    requestKey = value;
  }

  public List<Line> getItems() {
    return items;
  }

  public void setItems(List<Line> value) {
    items = value;
  }

  public static class Line {
    @NotNull(message = "{validation.book.required}")
    @Min(value = 1, message = "{validation.book.required}")
    private Long bookId;

    @NotNull(message = "{validation.quantity.positive}")
    @Min(value = 1, message = "{validation.quantity.positive}")
    @Max(value = 1000000, message = "{validation.quantity.positive}")
    private Integer quantity = 1;

    @Min(value = 0, message = "{validation.price.min}")
    @Max(value = 999999999999L, message = "{validation.price.max}")
    private Long unitPrice;

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

    public Long getUnitPrice() {
      return unitPrice;
    }

    public void setUnitPrice(Long value) {
      unitPrice = value;
    }
  }
}
