package com.portfolio.erp.security;

import org.springframework.stereotype.Service;
import org.springframework.security.core.userdetails.*;

@Service
public class MybatisUserDetailsService implements UserDetailsService {
    private final UserMapper mapper;
    public MybatisUserDetailsService(UserMapper mapper) { this.mapper = mapper; }
    @Override public UserDetails loadUserByUsername(String username) {
        var account = mapper.findByUsername(username);
        if (account == null) throw new UsernameNotFoundException("authentication_failed");
        return new ErpPrincipal(account);
    }
}
