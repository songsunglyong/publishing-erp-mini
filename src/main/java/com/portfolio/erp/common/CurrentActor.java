package com.portfolio.erp.common;

import com.portfolio.erp.common.error.*;
import com.portfolio.erp.security.ErpPrincipal;
import java.time.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentActor {
  private final Clock clock;

  public CurrentActor(Clock clock) {
    this.clock = clock;
  }

  public long id() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null
        || !(authentication.getPrincipal() instanceof ErpPrincipal principal))
      throw new UserFacingException(ErrorCode.LOGIN_REQUIRED);
    return principal.getId();
  }

  public LocalDateTime now() {
    return LocalDateTime.ofInstant(clock.instant(), ZoneId.of("Asia/Seoul"));
  }

  public static void requestKey(String value) {
    try {
      if (value == null || !java.util.UUID.fromString(value).toString().equalsIgnoreCase(value))
        throw new IllegalArgumentException();
    } catch (IllegalArgumentException ex) {
      throw new UserFacingException(ErrorCode.VALIDATION_FAILED);
    }
  }
}
