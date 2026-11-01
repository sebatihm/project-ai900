package info.rio.project_ai901.services.report.dto;


import java.math.BigDecimal;

public record GeneratedReport(String token, String filename, String periodLabel, String scope,
                              long orders, BigDecimal total) {}
