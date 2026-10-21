package com.portfolio.erp.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.HexFormat;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginAttempts {
    private final UserMapper mapper;
    private final Clock clock;
    private final AtomicLong lastPrune = new AtomicLong();
    public LoginAttempts(UserMapper mapper, Clock clock) { this.mapper = mapper; this.clock = clock; }
    public static String key(String category, String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest((category + ":" + value).getBytes(StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException ex) { throw new IllegalStateException("digest_unavailable"); }
    }
    private String accountKey(String username) { return key("account", username.trim().toLowerCase(Locale.ROOT)); }
    @Transactional public boolean reserve(String username, String address) {
        Instant now = clock.instant(), cutoff = now.minus(Duration.ofMinutes(15));
        long previous = lastPrune.get();
        if (now.getEpochSecond() - previous >= 60 && lastPrune.compareAndSet(previous, now.getEpochSecond()))
            mapper.pruneAttempts(now.minus(Duration.ofMinutes(30)));
        // Reserve atomically before password hashing; one IP cannot allocate unlimited account buckets.
        String ip = key("ip", address);
        mapper.reserveAttempt(ip, now, cutoff);
        if (mapper.attempts(ip) > 30) return false;
        String account = accountKey(username);
        mapper.reserveAttempt(account, now, cutoff);
        return mapper.attempts(account) <= 5;
    }
    @Transactional public void succeeded(String username) { mapper.clearAttempt(accountKey(username)); }
}
