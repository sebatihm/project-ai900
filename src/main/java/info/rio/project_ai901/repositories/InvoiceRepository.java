package info.rio.project_ai901.repositories;

import info.rio.project_ai901.models.Invoice;
import info.rio.project_ai901.models.enums.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    Optional<Invoice> findByFileHash(String fileHash);

    boolean existsByFileHash(String fileHash);

    List<Invoice> findBySupplier_IdAndInvoiceNumber(UUID supplierId, String invoiceNumber);

    List<Invoice> findBySupplier_IdAndInvoiceDateAndTotal(UUID supplierId, LocalDate invoiceDate, BigDecimal total);

    List<Invoice> findTop20BySupplier_IdOrderByCreatedAtDesc(UUID supplierId);

    Page<Invoice> findByStatus(InvoiceStatus status, Pageable pageable);

    long countByStatus(InvoiceStatus status);

    long count();
}
