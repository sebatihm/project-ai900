package info.rio.project_ai901.services.report.dto;

import java.time.LocalDate;

public record ReportPeriod(LocalDate from, LocalDate to, String label) {}