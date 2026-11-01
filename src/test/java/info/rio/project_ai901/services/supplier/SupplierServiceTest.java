package info.rio.project_ai901.services.supplier;

import info.rio.project_ai901.api.dto.CreateSupplierRequest;
import info.rio.project_ai901.api.dto.SupplierDetailResponse;
import info.rio.project_ai901.api.dto.UpdateSupplierRequest;
import info.rio.project_ai901.exceptions.DuplicateResourceException;
import info.rio.project_ai901.exceptions.ResourceInUseException;
import info.rio.project_ai901.exceptions.ResourceNotFoundException;
import info.rio.project_ai901.models.Supplier;
import info.rio.project_ai901.repositories.InvoiceRepository;
import info.rio.project_ai901.repositories.SupplierRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SupplierServiceTest {

    private SupplierRepository supplierRepository;
    private InvoiceRepository invoiceRepository;
    private SupplierService service;
    private UUID supplierId;

    @BeforeEach
    void setUp() {
        supplierRepository = Mockito.mock(SupplierRepository.class);
        invoiceRepository = Mockito.mock(InvoiceRepository.class);
        service = new SupplierService(supplierRepository, invoiceRepository,
                new SupplierMapper("http://frontend.test/"));
        supplierId = UUID.randomUUID();
        when(supplierRepository.save(any(Supplier.class))).thenAnswer(invocation -> {
            Supplier saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(supplierId);
            }
            return saved;
        });
    }

    @Test
    void createsSupplierNormalizingNameRfcAndEmail() {
        when(supplierRepository.existsByRfcIgnoreCase("ABC123")).thenReturn(false);

        SupplierDetailResponse result = service.create(new CreateSupplierRequest(
                " Proveedor Demo ", " abc123 ", " ventas@demo.test "));

        assertThat(result.name()).isEqualTo("Proveedor Demo");
        assertThat(result.rfc()).isEqualTo("ABC123");
        assertThat(result.email()).isEqualTo("ventas@demo.test");
        assertThat(result.invoiceAnalyzerUrl())
                .isEqualTo("http://frontend.test/invoice-analyzer/" + supplierId);
        verify(supplierRepository).save(any(Supplier.class));
    }

    @Test
    void createsSupplierWithBlankEmailAsNull() {
        SupplierDetailResponse result = service.create(new CreateSupplierRequest(
                "Proveedor Demo", "ABC123", "   "));

        assertThat(result.email()).isNull();
    }

    @Test
    void rejectsDuplicatedRfcOnCreate() {
        when(supplierRepository.existsByRfcIgnoreCase("ABC123")).thenReturn(true);

        assertThatThrownBy(() -> service.create(new CreateSupplierRequest(
                "Proveedor Demo", "abc123", null)))
                .isInstanceOf(DuplicateResourceException.class);

        verify(supplierRepository, never()).save(any(Supplier.class));
    }

    @Test
    void updatesSupplierSuccessfully() {
        Supplier existing = supplier(supplierId);
        when(supplierRepository.findById(supplierId)).thenReturn(Optional.of(existing));
        when(supplierRepository.existsByRfcIgnoreCaseAndIdNot("NEW123", supplierId)).thenReturn(false);

        SupplierDetailResponse result = service.update(supplierId,
                new UpdateSupplierRequest("Proveedor Nuevo", "new123", "nuevo@demo.test"));

        assertThat(result.id()).isEqualTo(supplierId);
        assertThat(result.name()).isEqualTo("Proveedor Nuevo");
        assertThat(result.rfc()).isEqualTo("NEW123");
        assertThat(result.email()).isEqualTo("nuevo@demo.test");
        verify(supplierRepository).save(existing);
    }

    @Test
    void rejectsUpdateWithRfcOfAnotherSupplier() {
        when(supplierRepository.findById(supplierId)).thenReturn(Optional.of(supplier(supplierId)));
        when(supplierRepository.existsByRfcIgnoreCaseAndIdNot("NEW123", supplierId)).thenReturn(true);

        assertThatThrownBy(() -> service.update(supplierId,
                new UpdateSupplierRequest("Proveedor Nuevo", "new123", null)))
                .isInstanceOf(DuplicateResourceException.class);

        verify(supplierRepository, never()).save(any(Supplier.class));
    }

    @Test
    void refusesToDeleteSupplierWithInvoices() {
        when(supplierRepository.findById(supplierId)).thenReturn(Optional.of(supplier(supplierId)));
        when(invoiceRepository.countBySupplier_Id(supplierId)).thenReturn(1L);

        assertThatThrownBy(() -> service.delete(supplierId))
                .isInstanceOf(ResourceInUseException.class)
                .hasMessage("No se puede eliminar el proveedor porque tiene facturas asociadas");

        verify(supplierRepository, never()).delete(any(Supplier.class));
    }

    @Test
    void deletesSupplierWithoutInvoices() {
        Supplier existing = supplier(supplierId);
        when(supplierRepository.findById(supplierId)).thenReturn(Optional.of(existing));
        when(invoiceRepository.countBySupplier_Id(supplierId)).thenReturn(0L);

        service.delete(supplierId);

        verify(supplierRepository).delete(existing);
    }

    @Test
    void throwsNotFoundForUnknownSupplier() {
        when(supplierRepository.findById(supplierId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.detail(supplierId))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.delete(supplierId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listsWithClampedOffsetAndLimit() {
        when(supplierRepository.findAll(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(supplier(supplierId)), PageRequest.of(0, 100), 1));

        var result = service.list(-5, 500);

        assertThat(result.offset()).isZero();
        assertThat(result.limit()).isEqualTo(100);
        assertThat(result.items()).hasSize(1);
    }

    private Supplier supplier(UUID id) {
        Supplier supplier = new Supplier();
        supplier.setId(id);
        supplier.setName("Proveedor Demo");
        supplier.setRfc("ABC123");
        supplier.setEmail("ventas@demo.test");
        return supplier;
    }
}
