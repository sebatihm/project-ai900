package info.rio.project_ai901.services.ai;

import info.rio.project_ai901.services.ai.dto.InvoiceOcrResult;

import java.io.InputStream;

public interface DocumentIntelligenceService {

    InvoiceOcrResult analyzeInvoice(InputStream content);
}
