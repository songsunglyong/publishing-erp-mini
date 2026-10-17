package com.portfolio.erp.partner;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import com.portfolio.erp.common.PageQuery;

@Mapper
public interface PartnerMapper {
    Partner findById(long id);
    List<Partner> findAll(PageQuery query);
    long count(PageQuery query);
    int insert(Partner entity);
    int update(Partner entity);
    int deactivate(long id);
}
