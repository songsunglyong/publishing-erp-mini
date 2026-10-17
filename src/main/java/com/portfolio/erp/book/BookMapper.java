package com.portfolio.erp.book;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import com.portfolio.erp.common.PageQuery;

@Mapper
public interface BookMapper {
    Book findById(long id);
    List<Book> findAll(PageQuery query);
    long count(PageQuery query);
    int insert(Book entity);
    int update(Book entity);
    int deactivate(long id);
    int insertInventory(long bookId);
}
