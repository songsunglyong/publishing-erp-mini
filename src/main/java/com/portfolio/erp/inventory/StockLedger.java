package com.portfolio.erp.inventory;

import com.portfolio.erp.common.*;
import com.portfolio.erp.common.error.*;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** Must share the caller's transaction: stock and its ledger never commit separately. */
@Service
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class StockLedger {
  private final InventoryMapper mapper;
  private final CurrentActor actor;

  public StockLedger(InventoryMapper mapper, CurrentActor actor) {
    this.mapper = mapper;
    this.actor = actor;
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public void apply(
      long bookId,
      int delta,
      String type,
      String referenceType,
      long referenceId,
      String operationKey,
      String note) {
    if (!java.util.List.of("RECEIPT", "SHIPMENT", "RETURN", "ADJUSTMENT").contains(type)
        || delta == 0) throw new UserFacingException(ErrorCode.VALIDATION_FAILED);
    if ("ADJUSTMENT".equals(type)
        && SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
            .noneMatch(a -> a.getAuthority().equals("ROLE_ADMIN")))
      throw new AccessDeniedException("access_denied");
    var row = mapper.lock(bookId);
    if (row == null) throw new NotFoundException();
    if ("RECEIPT".equals(type) && !row.getActive())
      throw new UserFacingException(ErrorCode.INACTIVE_MASTER);
    long balance = (long) row.getQuantityOnHand() + delta;
    if (balance < 0) throw new UserFacingException(ErrorCode.INSUFFICIENT_STOCK);
    if (balance > Integer.MAX_VALUE) throw new UserFacingException(ErrorCode.QUANTITY_LIMIT);
    mapper.setBalance(bookId, (int) balance);
    var movement = new StockMovement();
    movement.setBookId(bookId);
    movement.setQuantity(delta);
    movement.setBalanceAfter((int) balance);
    movement.setType(type);
    movement.setReferenceType(referenceType);
    movement.setReferenceId(referenceId);
    movement.setOperationKey(operationKey);
    movement.setNote(note);
    movement.setActorId(actor.id());
    movement.setCreatedAt(actor.now());
    try {
      mapper.insertMovement(movement);
    } catch (DuplicateKeyException ex) {
      throw new UserFacingException(ErrorCode.DUPLICATE_OPERATION);
    }
  }
}
