package info.rio.project_ai901.services.invoice;

import info.rio.project_ai901.api.dto.InvoiceDetailResponse;
import info.rio.project_ai901.api.dto.ReviewAction;
import info.rio.project_ai901.api.dto.ReviewRequest;
import info.rio.project_ai901.exceptions.InvalidRequestException;
import info.rio.project_ai901.exceptions.ResourceNotFoundException;
import info.rio.project_ai901.services.storage.StorageException;
import info.rio.project_ai901.models.Invoice;
import info.rio.project_ai901.models.User;
import info.rio.project_ai901.models.enums.InvoiceEventType;
import info.rio.project_ai901.models.enums.InvoiceStatus;
import info.rio.project_ai901.repositories.InvoiceEventRepository;
import info.rio.project_ai901.repositories.InvoiceItemRepository;
import info.rio.project_ai901.repositories.InvoiceRepository;
import info.rio.project_ai901.repositories.UserRepository;
import info.rio.project_ai901.services.storage.StorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
public class InvoiceReviewService {

    private static final String PDF_CONTENT_TYPE = "application/pdf";

    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final InvoiceEventRepository invoiceEventRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;
    private final InvoiceEventRecorder eventRecorder;
    private final InvoiceMapper mapper;
    private final String containerValid;
    private final String containerQuarantine;

    public InvoiceReviewService(InvoiceRepository invoiceRepository, InvoiceItemRepository invoiceItemRepository,
            InvoiceEventRepository invoiceEventRepository, UserRepository userRepository,
            StorageService storageService, InvoiceEventRecorder eventRecorder, InvoiceMapper mapper,
            @Value("${app.storage.container.valid:facturas-validas}") String containerValid,
            @Value("${app.storage.container.quarantine:facturas-cuarentena}") String containerQuarantine) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceItemRepository = invoiceItemRepository;
        this.invoiceEventRepository = invoiceEventRepository;
        this.userRepository = userRepository;
        this.storageService = storageService;
        this.eventRecorder = eventRecorder;
        this.mapper = mapper;
        this.containerValid = containerValid;
        this.containerQuarantine = containerQuarantine;
    }

    @Transactional
    public InvoiceDetailResponse review(UUID invoiceId, ReviewRequest request) {
        if (request == null || request.action() == null) {
            throw new InvalidRequestException("La accion es obligatoria (APPROVE|REJECT)");
        }
        if (request.reviewedBy() == null || request.reviewedBy().isBlank()) {
            throw new InvalidRequestException("El revisor (reviewedBy) es obligatorio");
        }
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Factura no encontrada: " + invoiceId));
        User reviewer = userRepository.findByEmail(request.reviewedBy())
                .orElseThrow(() -> new InvalidRequestException("Usuario no encontrado: " + request.reviewedBy()));

        InvoiceStatus target = request.action() == ReviewAction.APPROVE ? InvoiceStatus.VALID
                : InvoiceStatus.REJECTED;
        moveToTargetContainer(invoice, request.action() == ReviewAction.APPROVE ? containerValid : containerQuarantine);

        invoice.setStatus(target);
        invoice.setReviewedBy(reviewer);
        invoice.setReviewedAt(LocalDateTime.now());
        invoiceRepository.save(invoice);

        eventRecorder.record(invoice, reviewer,
                request.action() == ReviewAction.APPROVE ? InvoiceEventType.APPROVED : InvoiceEventType.REJECTED,
                request.comment());

        return mapper.toDetail(invoice, mapper.parseReasons(invoice.getReasons()),
                mapper.toItems(invoiceItemRepository.findByInvoice_Id(invoiceId)),
                mapper.toEvents(invoiceEventRepository.findByInvoice_IdOrderByCreatedAtAsc(invoiceId)));
    }

    private void moveToTargetContainer(Invoice invoice, String target) {
        if (target.equals(invoice.getContainer())) {
            return;
        }
        String path = invoice.getBlobPath();
        int slash = path == null ? -1 : path.lastIndexOf('/');
        String folder = slash < 0 ? LocalDate.now().toString() : path.substring(0, slash);
        String filename = slash < 0 ? path : path.substring(slash + 1);

        byte[] content = download(invoice.getContainer(), path);
        String movedPath = storageService.upload(target, folder, filename, new ByteArrayInputStream(content),
                content.length, PDF_CONTENT_TYPE, Map.of("sha256", invoice.getFileHash()));
        storageService.delete(invoice.getContainer(), path);

        invoice.setContainer(target);
        invoice.setBlobPath(movedPath);
    }

    private byte[] download(String container, String path) {
        try (InputStream stream = storageService.download(container, path)) {
            return stream.readAllBytes();
        } catch (IOException e) {
            throw new StorageException("No se pudo leer el archivo de la factura", e);
        }
    }
}
