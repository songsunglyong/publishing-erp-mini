package com.portfolio.erp.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;

public class ReturnForm {
  @NotBlank(message = "{validation.reason}")
  @Size(max = 500, message = "{validation.note}")
  private String reason;

  @NotBlank(message = "{validation.request.key}")
  private String requestKey;

  @Valid
  @NotEmpty(message = "{validation.order.items}")
  @Size(max = 20, message = "{validation.order.items}")
  private List<@NotNull(message = "{validation.order.items}") Line> items = new ArrayList<>();

  public String getReason() {
    return reason;
  }

  public void setReason(String value) {
    reason = value;
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
    private Long itemId;

    @NotNull(message = "{validation.return.quantity}")
    @Min(value = 0, message = "{validation.return.quantity}")
    @Max(value = 1000000, message = "{validation.return.quantity}")
    private Integer quantity = 0;

    public Long getItemId() {
      return itemId;
    }

    public void setItemId(Long value) {
      itemId = value;
    }

    public Integer getQuantity() {
      return quantity;
    }

    public void setQuantity(Integer value) {
      quantity = value;
    }
  }
}
