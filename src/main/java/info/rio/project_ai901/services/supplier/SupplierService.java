package info.rio.project_ai901.services.supplier;

import info.rio.project_ai901.api.dto.CreateSupplierRequest;
import info.rio.project_ai901.api.dto.PaginatedResponse;
import info.rio.project_ai901.api.dto.SupplierDetailResponse;
import info.rio.project_ai901.api.dto.SupplierSummaryResponse;
import info.rio.project_ai901.api.dto.UpdateSupplierRequest;
import info.rio.project_ai901.exceptions.DuplicateResourceException;
import info.rio.project_ai901.exceptions.ResourceInUseException;
import info.rio.project_ai901.exceptions.ResourceNotFoundException;
import info.rio.project_ai901.models.Supplier;
import info.rio.project_ai901.repositories.InvoiceRepository;
import info.rio.project_ai901.repositories.SupplierRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final InvoiceRepository invoiceRepository;
    private final SupplierMapper mapper;

    public SupplierService(SupplierRepository supplierRepository, InvoiceRepository invoiceRepository,
            SupplierMapper mapper) {
        this.supplierRepository = supplierRepository;
        this.invoiceRepository = invoiceRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<SupplierSummaryResponse> list(long offset, int limit) {
        int size = clampLimit(limit);
        long safeOffset = Math.max(offset, 0);
        PageRequest pageable = PageRequest.of((int) (safeOffset / size), size, Sort.by("name").ascending());
        Page<Supplier> pageResult = supplierRepository.findAll(pageable);
        List<SupplierSummaryResponse> items = pageResult.getContent().stream()
                .map(mapper::toSummary)
                .toList();
        return new PaginatedResponse<>(items, pageResult.getTotalElements(), safeOffset, size);
    }

    @Transactional(readOnly = true)
    public SupplierDetailResponse detail(UUID id) {
        return mapper.toDetail(find(id));
    }

    @Transactional
    public SupplierDetailResponse create(CreateSupplierRequest request) {
        String rfc = normalizeRfc(request.rfc());
        if (supplierRepository.existsByRfcIgnoreCase(rfc)) {
            throw new DuplicateResourceException("Ya existe un proveedor con el RFC: " + rfc);
        }
        Supplier supplier = new Supplier();
        apply(supplier, request.name(), rfc, request.email());
        supplierRepository.save(supplier);
        return mapper.toDetail(supplier);
    }

    @Transactional
    public SupplierDetailResponse update(UUID id, UpdateSupplierRequest request) {
        Supplier supplier = find(id);
        String rfc = normalizeRfc(request.rfc());
        if (supplierRepository.existsByRfcIgnoreCaseAndIdNot(rfc, id)) {
            throw new DuplicateResourceException("Ya existe un proveedor con el RFC: " + rfc);
        }
        apply(supplier, request.name(), rfc, request.email());
        supplierRepository.save(supplier);
        return mapper.toDetail(supplier);
    }

    @Transactional
    public void delete(UUID id) {
        Supplier supplier = find(id);
        if (invoiceRepository.countBySupplier_Id(id) > 0) {
            throw new ResourceInUseException(
                    "No se puede eliminar el proveedor porque tiene facturas asociadas");
        }
        supplierRepository.delete(supplier);
    }

    private void apply(Supplier supplier, String name, String rfc, String email) {
        supplier.setName(name.trim());
        supplier.setRfc(rfc);
        supplier.setEmail(normalizeEmail(email));
    }

    private Supplier find(UUID id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado: " + id));
    }

    private String normalizeRfc(String rfc) {
        return rfc.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        String trimmed = email.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private int clampLimit(int limit) {
        if (limit <= 0) {
            return 20;
        }
        return Math.min(limit, 100);
    }
}
