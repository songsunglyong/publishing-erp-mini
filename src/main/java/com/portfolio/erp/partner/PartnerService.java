package com.portfolio.erp.partner;

import jakarta.validation.Valid;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import com.portfolio.erp.common.*;

@Service
@org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN','MANAGER','USER')")
@Validated
@Transactional(readOnly = true)
public class PartnerService {
    private final PartnerMapper mapper;
    public PartnerService(PartnerMapper mapper) { this.mapper = mapper; }
    public Partner find(long id) {
        var entity = mapper.findById(id);
        if (entity == null) throw new NotFoundException();
        return entity;
    }
    public PageResult<Partner> list(@Valid PageQuery query) {
        return new PageResult<>(mapper.findAll(query), mapper.count(query), query);
    }
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Transactional public long create(@Valid PartnerForm form) {
        var entity = Partner.from(form);
        mapper.insert(entity);
        return entity.getId();
    }
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Transactional public void update(long id, @Valid PartnerForm form) {
        var entity = Partner.from(form);
        entity.setId(id);
        if (mapper.update(entity) == 0) throw new NotFoundException();
    }
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Transactional public void deactivate(long id) {
        if (mapper.deactivate(id) == 0) throw new NotFoundException();
    }
}
