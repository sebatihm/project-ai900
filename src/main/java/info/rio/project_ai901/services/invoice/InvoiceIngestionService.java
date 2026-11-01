package info.rio.project_ai901.services.invoice;

import tools.jackson.databind.ObjectMapper;
import info.rio.project_ai901.api.dto.InvoiceAnalysisResponse;
import info.rio.project_ai901.api.dto.InvoiceItemResponse;
import info.rio.project_ai901.exceptions.DuplicateInvoiceException;
import info.rio.project_ai901.exceptions.InvalidRequestException;
import info.rio.project_ai901.models.Invoice;
import info.rio.project_ai901.models.InvoiceItem;
import info.rio.project_ai901.models.Supplier;
import info.rio.project_ai901.models.enums.InvoiceEventType;
import info.rio.project_ai901.models.enums.InvoiceStatus;
import info.rio.project_ai901.repositories.InvoiceItemRepository;
import info.rio.project_ai901.repositories.InvoiceRepository;
import info.rio.project_ai901.repositories.SupplierRepository;
import info.rio.project_ai901.services.ai.DocumentIntelligenceService;
import info.rio.project_ai901.services.ai.dto.InvoiceItemData;
import info.rio.project_ai901.services.ai.dto.InvoiceOcrResult;
import info.rio.project_ai901.services.invoice.dto.SuspicionReason;
import info.rio.project_ai901.services.storage.StorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class InvoiceIngestionService {

    private static final String PDF_CONTENT_TYPE = "application/pdf";

    private final InvoiceFileValidator fileValidator;
    private final InvoiceNormalizer normalizer;
    private final StorageService storageService;
    private final DocumentIntelligenceService documentIntelligenceService;
    private final InvoiceScoringService scoringService;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final InvoiceEventRecorder eventRecorder;
    private final SupplierRepository supplierRepository;
    private final InvoiceMapper mapper;
    private final ObjectMapper objectMapper;
    private final String containerIn;
    private final String containerValid;
    private final String containerQuarantine;
    private final int highScore;

    public InvoiceIngestionService(InvoiceFileValidator fileValidator, InvoiceNormalizer normalizer,
            StorageService storageService, DocumentIntelligenceService documentIntelligenceService,
            InvoiceScoringService scoringService, InvoiceRepository invoiceRepository,
            InvoiceItemRepository invoiceItemRepository, InvoiceEventRecorder eventRecorder,
            SupplierRepository supplierRepository, InvoiceMapper mapper,
            ObjectMapper objectMapper,
            @Value("${app.storage.container.in:facturas-entrada}") String containerIn,
            @Value("${app.storage.container.valid:facturas-validas}") String containerValid,
            @Value("${app.storage.container.quarantine:facturas-cuarentena}") String containerQuarantine,
            @Value("${app.invoice.high-score:3}") int highScore) {
        this.fileValidator = fileValidator;
        this.normalizer = normalizer;
        this.storageService = storageService;
        this.documentIntelligenceService = documentIntelligenceService;
        this.scoringService = scoringService;
        this.invoiceRepository = invoiceRepository;
        this.invoiceItemRepository = invoiceItemRepository;
        this.eventRecorder = eventRecorder;
        this.supplierRepository = supplierRepository;
        this.mapper = mapper;
        this.objectMapper = objectMapper;
        this.containerIn = containerIn;
        this.containerValid = containerValid;
        this.containerQuarantine = containerQuarantine;
        this.highScore = highScore;
    }

    @Transactional
    public InvoiceAnalysisResponse analyze(String originalFilename, byte[] content, UUID supplierId) {
        Supplier supplier = supplierId == null ? null
                : supplierRepository.findById(supplierId)
                        .orElseThrow(() -> new InvalidRequestException("Proveedor no encontrado: " + supplierId));

        ValidatedFile file = fileValidator.validate(originalFilename, content);
        String sha256 = InvoiceHashing.sha256Hex(file.content());

        invoiceRepository.findByFileHash(sha256).ifPresent(original -> {
            throw new DuplicateInvoiceException("esta factura ya fue subida", original.getId(),
                    original.getContainer(), original.getBlobPath());
        });

        byte[] pdf = normalizer.toPdf(file.content(), file.signature());
        String folder = LocalDate.now().toString();
        String filename = UUID.randomUUID() + ".pdf";
        Map<String, String> metadata = metadata(supplier, sha256);
        String inboxPath = storageService.upload(containerIn, folder, filename, new ByteArrayInputStream(pdf),
                pdf.length, PDF_CONTENT_TYPE, metadata);

        InvoiceOcrResult ocr = analyze(pdf);
        ScoringResult scoring = scoringService.evaluate(new ScoringInput(supplier, ocr.getVendorName(),
                ocr.getInvoiceNumber(), ocr.getInvoiceDate(), ocr.getSubtotal(), ocr.getTax(), ocr.getTotal(),
                ocr.getConfidenceByField(), ocr.getMinConfidence()));
        InvoiceStatus status = resolveStatus(ocr, scoring);

        Invoice invoice = new Invoice();
        invoice.setSupplier(supplier);
        invoice.setOriginalFileName(file.originalFileName());
        invoice.setFileHash(sha256);
        invoice.setContainer(containerIn);
        invoice.setBlobPath(inboxPath);
        invoice.setInvoiceNumber(ocr.getInvoiceNumber());
        invoice.setInvoiceDate(ocr.getInvoiceDate());
        invoice.setDueDate(ocr.getDueDate());
        invoice.setSubtotal(ocr.getSubtotal());
        invoice.setTax(ocr.getTax());
        invoice.setTotal(ocr.getTotal());
        invoice.setCurrency(ocr.getCurrency());
        invoice.setStatus(status);
        invoice.setMinConfidence(ocr.getMinConfidence());
        invoice.setSuspicionScore(scoring.score());
        invoice.setReasons(writeReasons(scoring.reasons()));
        invoice.setRawAnalysis(ocr.getRawJson());
        invoice.setCreatedAt(LocalDateTime.now());
        invoiceRepository.save(invoice);

        List<InvoiceItemResponse> itemResponses = new ArrayList<>();
        for (InvoiceItemData data : ocr.getItems()) {
            InvoiceItem item = new InvoiceItem();
            item.setInvoice(invoice);
            item.setDescription(data.description());
            item.setQuantity(data.quantity());
            item.setAmount(data.amount());
            invoiceItemRepository.save(item);
            itemResponses.add(new InvoiceItemResponse(data.description(), data.quantity(), data.amount()));
        }

        eventRecorder.record(invoice, null, InvoiceEventType.UPLOADED,
                "sha256=" + sha256 + ", nombre original=" + file.originalFileName());
        eventRecorder.record(invoice, null, InvoiceEventType.ANALYZED, analyzeDetail(ocr, status, scoring.score()));

        moveToFinalContainer(invoice, pdf, folder, filename, metadata, inboxPath);

        return mapper.toAnalysis(invoice, scoring.reasons(), itemResponses);
    }

    private InvoiceOcrResult analyze(byte[] pdf) {
        try {
            return documentIntelligenceService.analyzeInvoice(new ByteArrayInputStream(pdf));
        } catch (RuntimeException e) {
            InvoiceOcrResult failed = new InvoiceOcrResult();
            failed.setAnalysisFailed(true);
            failed.setAnalysisError(e.getMessage());
            return failed;
        }
    }

    private InvoiceStatus resolveStatus(InvoiceOcrResult ocr, ScoringResult scoring) {
        if (ocr.isAnalysisFailed() || !ocr.isIdentifiedAsInvoice()) {
            return InvoiceStatus.REJECTED;
        }
        if (ocr.getInvoiceNumber() == null || ocr.getInvoiceDate() == null || ocr.getTotal() == null) {
            return InvoiceStatus.REJECTED;
        }
        return scoring.score() >= highScore ? InvoiceStatus.SUSPICIOUS : InvoiceStatus.VALID;
    }

    private String analyzeDetail(InvoiceOcrResult ocr, InvoiceStatus status, int score) {
        if (ocr.isAnalysisFailed()) {
            return "El analisis fallo: " + ocr.getAnalysisError();
        }
        if (!ocr.isIdentifiedAsInvoice()) {
            return "El documento no fue identificado como factura";
        }
        return "Estado " + status + ", puntaje de sospecha " + score;
    }

    private void moveToFinalContainer(Invoice invoice, byte[] pdf, String folder, String filename,
            Map<String, String> metadata, String inboxPath) {
        String target = invoice.getStatus() == InvoiceStatus.VALID ? containerValid : containerQuarantine;
        String movedPath = storageService.upload(target, folder, filename, new ByteArrayInputStream(pdf), pdf.length,
                PDF_CONTENT_TYPE, metadata);
        if (!target.equals(containerIn)) {
            storageService.delete(containerIn, inboxPath);
        }
        invoice.setContainer(target);
        invoice.setBlobPath(movedPath);
        if (invoice.getStatus() == InvoiceStatus.SUSPICIOUS) {
            eventRecorder.record(invoice, null, InvoiceEventType.QUARANTINED,
                    "Enviada a cuarentena por puntaje de sospecha " + invoice.getSuspicionScore());
        } else if (invoice.getStatus() == InvoiceStatus.REJECTED) {
            eventRecorder.record(invoice, null, InvoiceEventType.REJECTED,
                    "Rechazada automaticamente (no identificada como factura)");
        }
    }

    private Map<String, String> metadata(Supplier supplier, String sha256) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("sha256", sha256);
        if (supplier != null && supplier.getId() != null) {
            metadata.put("supplier_id", supplier.getId().toString());
        }
        return metadata;
    }

    private String writeReasons(List<SuspicionReason> reasons) {
        try {
            return objectMapper.writeValueAsString(reasons);
        } catch (tools.jackson.core.JacksonException e) {
            throw new IllegalStateException("No se pudieron serializar los motivos de sospecha", e);
        }
    }
}
