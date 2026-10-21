package com.portfolio.erp.order;

import com.portfolio.erp.book.Book;
import com.portfolio.erp.partner.Partner;
import java.util.List;
import org.apache.ibatis.annotations.*;

@Mapper
public interface OrderMapper {
  SalesOrder find(long id);

  SalesOrder lock(long id);

  List<OrderItem> items(long id);

  List<OrderItem> lockItems(long id);

  List<SalesOrder> list(OrderQuery query);

  long count(OrderQuery query);

  List<Book> activeBooks();

  List<Partner> activePartners();

  Book lockBook(long id);

  Partner lockPartner(long id);

  int insert(SalesOrder order);

  int insertItem(OrderItem item);

  int transition(
      @Param("id") long id,
      @Param("from") String from,
      @Param("to") String to,
      @Param("at") java.time.LocalDateTime at);

  boolean createKeyExists(String key);

  boolean returnKeyExists(String key);

  int insertReturn(ReturnRecord record);

  int insertReturnItem(
      @Param("returnId") long returnId,
      @Param("itemId") long itemId,
      @Param("quantity") int quantity,
      @Param("amount") long amount);

  int incrementReturned(@Param("id") long id, @Param("quantity") int quantity);

  List<ReturnRecord> returns(long id);
}
