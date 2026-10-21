package com.portfolio.erp.order;

import com.portfolio.erp.book.Book;
import com.portfolio.erp.common.*;
import com.portfolio.erp.common.error.*;
import com.portfolio.erp.inventory.StockLedger;
import com.portfolio.erp.partner.Partner;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@PreAuthorize("hasAnyRole('ADMIN','MANAGER','USER')")
public class OrderService {
  private final OrderMapper mapper;
  private final StockLedger ledger;
  private final CurrentActor actor;

  public OrderService(OrderMapper mapper, StockLedger ledger, CurrentActor actor) {
    this.mapper = mapper;
    this.ledger = ledger;
    this.actor = actor;
  }

  public SalesOrder find(long id) {
    var order = mapper.find(id);
    if (order == null) throw new NotFoundException();
    order.setItems(mapper.items(id));
    return order;
  }

  public List<ReturnRecord> returns(long id) {
    return mapper.returns(id);
  }

  public PageResult<SalesOrder> list(@Valid OrderQuery query) {
    var page = new PageQuery();
    page.setPage(query.getPage());
    return new PageResult<>(mapper.list(query), mapper.count(query), page);
  }

  public List<Book> books() {
    return mapper.activeBooks();
  }

  public List<Partner> partners() {
    return mapper.activePartners();
  }

  @Transactional
  @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
  public long create(@Valid OrderForm form) {
    CurrentActor.requestKey(form.getRequestKey());
    var partner = mapper.lockPartner(form.getPartnerId());
    if (partner == null || !partner.getActive())
      throw new UserFacingException(ErrorCode.INACTIVE_MASTER);
    if (mapper.createKeyExists(form.getRequestKey()))
      throw new UserFacingException(ErrorCode.DUPLICATE_OPERATION);
    var lines = new ArrayList<OrderItem>();
    var seen = new HashSet<Long>();
    long total = 0;
    for (var input :
        form.getItems().stream().sorted(Comparator.comparing(OrderForm.Line::getBookId)).toList()) {
      if (!seen.add(input.getBookId()))
        throw new UserFacingException(ErrorCode.ORDER_ITEMS_INVALID);
      var book = mapper.lockBook(input.getBookId());
      if (book == null || !book.getActive())
        throw new UserFacingException(ErrorCode.INACTIVE_MASTER);
      long price = input.getUnitPrice() == null ? book.getListPrice() : input.getUnitPrice();
      long amount;
      try {
        amount = Math.multiplyExact(price, input.getQuantity());
        total = Math.addExact(total, amount);
      } catch (ArithmeticException ex) {
        throw new UserFacingException(ErrorCode.AMOUNT_LIMIT);
      }
      if (total > 999999999999999L) throw new UserFacingException(ErrorCode.AMOUNT_LIMIT);
      var item = new OrderItem();
      item.setBookId(book.getId());
      item.setBookTitle(book.getTitle());
      item.setQuantity(input.getQuantity());
      item.setUnitPrice(price);
      item.setLineTotal(amount);
      lines.add(item);
    }
    var order = new SalesOrder();
    order.setOrderNo("ORD-" + UUID.randomUUID().toString().replace("-", ""));
    order.setRequestKey(form.getRequestKey());
    order.setPartnerId(partner.getId());
    order.setPartnerName(partner.getName());
    order.setOrderedAt(actor.now());
    order.setUpdatedAt(actor.now());
    order.setStatus("DRAFT");
    order.setNotes(form.getNotes());
    order.setTotalAmount(total);
    order.setCreatedBy(actor.id());
    mapper.insert(order);
    for (var item : lines) {
      item.setOrderId(order.getId());
      mapper.insertItem(item);
    }
    return order.getId();
  }

  private SalesOrder lock(long id) {
    var order = mapper.lock(id);
    if (order == null) throw new NotFoundException();
    return order;
  }

  private void state(boolean allowed) {
    if (!allowed) throw new UserFacingException(ErrorCode.ORDER_STATE);
  }

  @Transactional
  @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
  public void confirm(long id) {
    var order = lock(id);
    state("DRAFT".equals(order.getStatus()));
    state(mapper.transition(id, "DRAFT", "CONFIRMED", actor.now()) == 1);
  }

  @Transactional
  @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
  public void cancel(long id) {
    var order = lock(id);
    state(List.of("DRAFT", "CONFIRMED").contains(order.getStatus()));
    state(mapper.transition(id, order.getStatus(), "CANCELLED", actor.now()) == 1);
  }

  @Transactional
  @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
  public void fulfill(long id) {
    var order = lock(id);
    state("CONFIRMED".equals(order.getStatus()));
    // 품목의 입력 순서가 달라도 도서 ID 순으로 잠가 교착 가능성을 줄인다.
    for (var item : mapper.items(id))
      ledger.apply(
          item.getBookId(),
          -item.getQuantity(),
          "SHIPMENT",
          "ORDER",
          id,
          "SHIPMENT:" + id + ":" + item.getBookId(),
          "주문 출고");
    state(mapper.transition(id, "CONFIRMED", "FULFILLED", actor.now()) == 1);
  }

  @Transactional
  @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
  public long returnItems(long id, @Valid ReturnForm form) {
    CurrentActor.requestKey(form.getRequestKey());
    var order = lock(id);
    state("FULFILLED".equals(order.getStatus()));
    if (mapper.returnKeyExists(form.getRequestKey()))
      throw new UserFacingException(ErrorCode.DUPLICATE_OPERATION);
    // 잠금 대기 후에도 최신 누적 반품 수량을 읽도록 현재 읽기를 사용한다.
    var byId = new HashMap<Long, OrderItem>();
    mapper.lockItems(id).forEach(item -> byId.put(item.getId(), item));
    var seen = new HashSet<Long>();
    var selected = new ArrayList<ReturnForm.Line>();
    long total = 0;
    for (var input : form.getItems()) {
      var item = byId.get(input.getItemId());
      if (item == null || !seen.add(input.getItemId()))
        throw new UserFacingException(ErrorCode.ORDER_ITEMS_INVALID);
      if (input.getQuantity() == 0) continue;
      if (input.getQuantity() > item.getQuantity() - item.getReturnedQuantity())
        throw new UserFacingException(ErrorCode.RETURN_LIMIT);
      total = Math.addExact(total, Math.multiplyExact(item.getUnitPrice(), input.getQuantity()));
      selected.add(input);
    }
    if (selected.isEmpty()) throw new UserFacingException(ErrorCode.RETURN_LIMIT);
    var record = new ReturnRecord();
    record.setOrderId(id);
    record.setRequestKey(form.getRequestKey());
    record.setReason(form.getReason());
    record.setTotalAmount(total);
    record.setActorId(actor.id());
    record.setReturnedAt(actor.now());
    mapper.insertReturn(record);
    for (var input :
        selected.stream()
            .sorted(Comparator.comparing(line -> byId.get(line.getItemId()).getBookId()))
            .toList()) {
      var item = byId.get(input.getItemId());
      state(mapper.incrementReturned(item.getId(), input.getQuantity()) == 1);
      mapper.insertReturnItem(
          record.getId(),
          item.getId(),
          input.getQuantity(),
          item.getUnitPrice() * input.getQuantity());
      ledger.apply(
          item.getBookId(),
          input.getQuantity(),
          "RETURN",
          "RETURN",
          record.getId(),
          "RETURN:" + record.getId() + ":" + item.getId(),
          form.getReason());
    }
    return record.getId();
  }
}
