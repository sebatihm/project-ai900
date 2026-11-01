package info.rio.project_ai901.services.supplier;

import info.rio.project_ai901.api.dto.SupplierDetailResponse;
import info.rio.project_ai901.api.dto.SupplierSummaryResponse;
import info.rio.project_ai901.models.Supplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SupplierMapper {

    private final String frontendBaseUrl;

    public SupplierMapper(@Value("${app.frontend.base-url:http://localhost:3000}") String frontendBaseUrl) {
        this.frontendBaseUrl = frontendBaseUrl.replaceAll("/+$", "");
    }

    public SupplierSummaryResponse toSummary(Supplier supplier) {
        return new SupplierSummaryResponse(
                supplier.getId(),
                supplier.getName(),
                supplier.getRfc(),
                supplier.getEmail(),
                invoiceAnalyzerUrl(supplier));
    }

    public SupplierDetailResponse toDetail(Supplier supplier) {
        return new SupplierDetailResponse(
                supplier.getId(),
                supplier.getName(),
                supplier.getRfc(),
                supplier.getEmail(),
                invoiceAnalyzerUrl(supplier));
    }

    private String invoiceAnalyzerUrl(Supplier supplier) {
        return frontendBaseUrl + "/invoice-analyzer/" + supplier.getId();
    }
}
