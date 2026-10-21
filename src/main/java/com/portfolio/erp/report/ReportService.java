package com.portfolio.erp.report;

import com.portfolio.erp.inventory.StockRow;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@Transactional(readOnly = true)
@PreAuthorize("hasAnyRole('ADMIN','MANAGER','USER')")
public class ReportService {
  private final ReportMapper mapper;

  public ReportService(ReportMapper mapper) {
    this.mapper = mapper;
  }

  public record SalesReport(
      ReportRow summary,
      List<ReportRow> monthly,
      List<ReportRow> books,
      List<ReportRow> partners,
      List<StockRow> lowStock) {}

  public SalesReport sales(@Valid ReportQuery query) {
    return new SalesReport(
        mapper.summary(query),
        mapper.monthly(query),
        mapper.books(query),
        mapper.partners(query),
        mapper.lowStock(query));
  }
}
