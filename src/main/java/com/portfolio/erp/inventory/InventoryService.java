package com.portfolio.erp.inventory;

import com.portfolio.erp.book.Book;
import com.portfolio.erp.common.*;
import com.portfolio.erp.common.error.*;
import com.portfolio.erp.order.OrderMapper;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@PreAuthorize("hasAnyRole('ADMIN','MANAGER','USER')")
public class InventoryService {
  private final InventoryMapper mapper;
  private final StockLedger ledger;
  private final OrderMapper orders;

  public InventoryService(InventoryMapper mapper, StockLedger ledger, OrderMapper orders) {
    this.mapper = mapper;
    this.ledger = ledger;
    this.orders = orders;
  }

  public PageResult<StockRow> list(@Valid InventoryQuery query) {
    return new PageResult<>(mapper.list(query), mapper.count(query), query);
  }

  public PageResult<StockMovement> movements(@Valid InventoryQuery query) {
    return new PageResult<>(mapper.movements(query), mapper.movementCount(query), query);
  }

  public List<Book> books() {
    return orders.activeBooks();
  }

  @Transactional
  @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
  public void receive(@Valid StockForm form) {
    CurrentActor.requestKey(form.getRequestKey());
    if (form.getQuantity() <= 0) throw new UserFacingException(ErrorCode.VALIDATION_FAILED);
    ledger.apply(
        form.getBookId(),
        form.getQuantity(),
        "RECEIPT",
        "RECEIPT",
        form.getBookId(),
        "RECEIPT:" + form.getRequestKey(),
        form.getNote());
  }

  @Transactional
  @PreAuthorize("hasRole('ADMIN')")
  public void adjust(@Valid StockForm form) {
    CurrentActor.requestKey(form.getRequestKey());
    if (form.getQuantity() == 0) throw new UserFacingException(ErrorCode.VALIDATION_FAILED);
    ledger.apply(
        form.getBookId(),
        form.getQuantity(),
        "ADJUSTMENT",
        "ADJUSTMENT",
        form.getBookId(),
        "ADJUSTMENT:" + form.getRequestKey(),
        form.getNote());
  }
}
