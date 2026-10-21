package com.portfolio.erp.report;

import com.portfolio.erp.inventory.StockRow;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ReportMapper {
  ReportRow summary(ReportQuery query);

  List<ReportRow> monthly(ReportQuery query);

  List<ReportRow> books(ReportQuery query);

  List<ReportRow> partners(ReportQuery query);

  List<StockRow> lowStock(ReportQuery query);
}
