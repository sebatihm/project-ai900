package info.rio.project_ai901.models;

import info.rio.project_ai901.models.enums.InvoiceStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "invoices")
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @Column(length = 255)
    private String originalFileName;

    @Column(nullable = false, unique = true, length = 64)
    private String fileHash;

    @Column(nullable = false, length = 100)
    private String container;

    @Column(nullable = false, length = 500)
    private String blobPath;

    @Column(length = 50)
    private String invoiceNumber;

    private LocalDate invoiceDate;

    private LocalDate dueDate;

    @Column(precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(precision = 12, scale = 2)
    private BigDecimal tax;

    @Column(precision = 12, scale = 2)
    private BigDecimal total;

    @Column(length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InvoiceStatus status;

    @Column(precision = 4, scale = 3)
    private BigDecimal minConfidence;

    @Column(nullable = false)
    private int suspicionScore;

    @Lob
    @Column(name = "reasons")
    private String reasons;

    @Lob
    @Column(name = "raw_analysis")
    private String rawAnalysis;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime reviewedAt;
}
