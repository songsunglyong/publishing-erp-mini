package com.portfolio.erp.security;

import java.time.Instant;
import java.util.List;
import org.apache.ibatis.annotations.*;

@Mapper
public interface UserMapper {
    UserAccount findByUsername(String username);
    UserAccount findById(long id);
    StaffProfile profile(long id);
    List<StaffProfile> profiles();
    int insert(UserAccount account);
    long count();
    int block(long id);
    int changePassword(@Param("id") long id, @Param("hash") String hash, @Param("version") long version);
    void reserveAttempt(@Param("key") String key, @Param("now") Instant now, @Param("cutoff") Instant cutoff);
    int attempts(String key);
    void clearAttempt(String key);
    void pruneAttempts(Instant cutoff);
}
