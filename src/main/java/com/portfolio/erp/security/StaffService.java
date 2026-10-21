package com.portfolio.erp.security;

import com.portfolio.erp.common.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.security.access.prepost.PreAuthorize;

@Service
public class StaffService {
    private final UserMapper mapper;
    public StaffService(UserMapper mapper) { this.mapper = mapper; }
    @PreAuthorize("@staffAccess.canRead(#id, authentication)")
    public StaffProfile find(long id) {
        var profile = mapper.profile(id);
        if (profile == null) throw new NotFoundException();
        return profile;
    }
}
