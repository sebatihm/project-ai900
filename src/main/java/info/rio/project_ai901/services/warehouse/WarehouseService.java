package info.rio.project_ai901.services.warehouse;

import info.rio.project_ai901.api.dto.CreateWarehouseRequest;
import info.rio.project_ai901.api.dto.PaginatedResponse;
import info.rio.project_ai901.api.dto.UpdateWarehouseRequest;
import info.rio.project_ai901.api.dto.WarehouseDetailResponse;
import info.rio.project_ai901.api.dto.WarehouseSummaryResponse;
import info.rio.project_ai901.exceptions.DuplicateResourceException;
import info.rio.project_ai901.exceptions.InvalidRequestException;
import info.rio.project_ai901.exceptions.ResourceInUseException;
import info.rio.project_ai901.exceptions.ResourceNotFoundException;
import info.rio.project_ai901.models.Branch;
import info.rio.project_ai901.models.Warehouse;
import info.rio.project_ai901.repositories.BranchRepository;
import info.rio.project_ai901.repositories.InventoryRepository;
import info.rio.project_ai901.repositories.WarehouseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;
    private final BranchRepository branchRepository;
    private final InventoryRepository inventoryRepository;
    private final WarehouseMapper mapper;

    public WarehouseService(WarehouseRepository warehouseRepository, BranchRepository branchRepository,
            InventoryRepository inventoryRepository, WarehouseMapper mapper) {
        this.warehouseRepository = warehouseRepository;
        this.branchRepository = branchRepository;
        this.inventoryRepository = inventoryRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<WarehouseSummaryResponse> list(boolean includeInactive, UUID branchId, long offset,
            int limit) {
        int size = clampLimit(limit);
        long safeOffset = Math.max(offset, 0);
        PageRequest pageable = PageRequest.of((int) (safeOffset / size), size, Sort.by("code").ascending());
        Page<Warehouse> pageResult = fetch(includeInactive, branchId, pageable);
        List<WarehouseSummaryResponse> items = pageResult.getContent().stream()
                .map(mapper::toSummary)
                .toList();
        return new PaginatedResponse<>(items, pageResult.getTotalElements(), safeOffset, size);
    }

    @Transactional(readOnly = true)
    public WarehouseDetailResponse detail(UUID id) {
        return mapper.toDetail(find(id));
    }

    @Transactional
    public WarehouseDetailResponse create(CreateWarehouseRequest request) {
        String code = normalize(request.code());
        if (warehouseRepository.existsByCodeIgnoreCase(code)) {
            throw new DuplicateResourceException("Ya existe un almacen con el codigo: " + code);
        }
        Branch branch = resolveBranch(request.branchId());
        Warehouse warehouse = new Warehouse();
        warehouse.setCode(code);
        warehouse.setName(request.name().trim());
        warehouse.setBranch(branch);
        warehouse.setActive(true);
        warehouseRepository.save(warehouse);
        return mapper.toDetail(warehouse);
    }

    @Transactional
    public WarehouseDetailResponse update(UUID id, UpdateWarehouseRequest request) {
        Warehouse warehouse = find(id);
        String code = normalize(request.code());
        if (warehouseRepository.existsByCodeIgnoreCaseAndIdNot(code, id)) {
            throw new DuplicateResourceException("Ya existe un almacen con el codigo: " + code);
        }
        Branch branch = resolveBranch(request.branchId());
        warehouse.setCode(code);
        warehouse.setName(request.name().trim());
        warehouse.setBranch(branch);
        warehouseRepository.save(warehouse);
        return mapper.toDetail(warehouse);
    }

    @Transactional
    public WarehouseDetailResponse deactivate(UUID id) {
        Warehouse warehouse = find(id);
        if (!warehouse.isActive()) {
            throw new ResourceNotFoundException("Almacen no encontrado: " + id);
        }
        if (inventoryRepository.countByWarehouse_Id(id) > 0) {
            throw new ResourceInUseException("No se puede desactivar el almacen porque tiene inventario asociado");
        }
        warehouse.setActive(false);
        warehouseRepository.save(warehouse);
        return mapper.toDetail(warehouse);
    }

    @Transactional(readOnly = true)
    public UUID branchIdOf(UUID warehouseId) {
        return find(warehouseId).getBranch().getId();
    }

    private Page<Warehouse> fetch(boolean includeInactive, UUID branchId, PageRequest pageable) {
        if (includeInactive) {
            return branchId == null ? warehouseRepository.findAll(pageable)
                    : warehouseRepository.findByBranch_Id(branchId, pageable);
        }
        return branchId == null ? warehouseRepository.findByActive(true, pageable)
                : warehouseRepository.findByActiveAndBranch_Id(true, branchId, pageable);
    }

    private Branch resolveBranch(UUID branchId) {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new InvalidRequestException("Sucursal no encontrada: " + branchId));
        if (!branch.isActive()) {
            throw new InvalidRequestException("La sucursal no esta activa: " + branchId);
        }
        return branch;
    }

    private Warehouse find(UUID id) {
        return warehouseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Almacen no encontrado: " + id));
    }

    private String normalize(String code) {
        return code == null ? null : code.trim();
    }

    private int clampLimit(int limit) {
        if (limit < 1) {
            return 1;
        }
        return Math.min(limit, 100);
    }
}
