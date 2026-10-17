package com.portfolio.erp.book;

import jakarta.validation.Valid;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import com.portfolio.erp.common.*;

@Service
@Validated
@Transactional(readOnly = true)
public class BookService {
    private final BookMapper mapper;
    public BookService(BookMapper mapper) { this.mapper = mapper; }
    public Book find(long id) {
        var entity = mapper.findById(id);
        if (entity == null) throw new NotFoundException();
        return entity;
    }
    public PageResult<Book> list(@Valid PageQuery query) {
        return new PageResult<>(mapper.findAll(query), mapper.count(query), query);
    }
    @Transactional public long create(@Valid BookForm form) {
        var entity = Book.from(form);
        mapper.insert(entity);
        mapper.insertInventory(entity.getId());
        return entity.getId();
    }
    @Transactional public void update(long id, @Valid BookForm form) {
        var entity = Book.from(form);
        entity.setId(id);
        if (mapper.update(entity) == 0) throw new NotFoundException();
    }
    @Transactional public void deactivate(long id) {
        if (mapper.deactivate(id) == 0) throw new NotFoundException();
    }
}
