package com.portfolio.erp.report;

import jakarta.validation.constraints.*;
import java.time.*;
import org.springframework.format.annotation.DateTimeFormat;

public class ReportQuery {
  @NotNull(message = "{validation.report.range}")
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate from = LocalDate.now(ZoneId.of("Asia/Seoul")).withDayOfYear(1);

  @NotNull(message = "{validation.report.range}")
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate to = LocalDate.now(ZoneId.of("Asia/Seoul"));

  @Min(value = 0, message = "{validation.stock.threshold}")
  @Max(value = 1000000, message = "{validation.stock.threshold}")
  private int lowStock = 5;

  @AssertTrue(message = "{validation.report.range}")
  public boolean isRangeValid() {
    return from != null
        && to != null
        && !from.isBefore(LocalDate.of(1900, 1, 1))
        && to.isBefore(LocalDate.of(9999, 1, 1))
        && !to.isBefore(from)
        && java.time.temporal.ChronoUnit.DAYS.between(from, to) <= 3660;
  }

  public LocalDate getFrom() {
    return from;
  }

  public void setFrom(LocalDate value) {
    from = value;
  }

  public LocalDate getTo() {
    return to;
  }

  public void setTo(LocalDate value) {
    to = value;
  }

  public LocalDateTime getToExclusive() {
    return to.plusDays(1).atStartOfDay();
  }

  public int getLowStock() {
    return lowStock;
  }

  public void setLowStock(int value) {
    lowStock = value;
  }
}
