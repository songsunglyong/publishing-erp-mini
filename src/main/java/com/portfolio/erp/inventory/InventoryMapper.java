package com.portfolio.erp.inventory;

import java.util.List;
import org.apache.ibatis.annotations.*;

@Mapper
public interface InventoryMapper {
  StockRow lock(long bookId);

  int setBalance(@Param("bookId") long bookId, @Param("balance") int balance);

  int insertMovement(StockMovement movement);

  List<StockRow> list(InventoryQuery query);

  long count(InventoryQuery query);

  List<StockMovement> movements(InventoryQuery query);

  long movementCount(InventoryQuery query);
}
