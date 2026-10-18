package info.rio.project_ai901.services.branch;

import info.rio.project_ai901.api.dto.BranchDetailResponse;
import info.rio.project_ai901.api.dto.BranchSummaryResponse;
import info.rio.project_ai901.api.dto.CreateBranchRequest;
import info.rio.project_ai901.api.dto.PaginatedResponse;
import info.rio.project_ai901.api.dto.UpdateBranchRequest;
import info.rio.project_ai901.exceptions.DuplicateResourceException;
import info.rio.project_ai901.exceptions.ResourceInUseException;
import info.rio.project_ai901.exceptions.ResourceNotFoundException;
import info.rio.project_ai901.models.Branch;
import info.rio.project_ai901.repositories.BranchRepository;
import info.rio.project_ai901.repositories.WarehouseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class BranchService {

    private final BranchRepository branchRepository;
    private final WarehouseRepository warehouseRepository;
    private final BranchMapper mapper;

    public BranchService(BranchRepository branchRepository, WarehouseRepository warehouseRepository,
            BranchMapper mapper) {
        this.branchRepository = branchRepository;
        this.warehouseRepository = warehouseRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<BranchSummaryResponse> list(boolean includeInactive, long offset, int limit) {
        int size = clampLimit(limit);
        long safeOffset = Math.max(offset, 0);
        PageRequest pageable = PageRequest.of((int) (safeOffset / size), size, Sort.by("code").ascending());
        Page<Branch> pageResult = includeInactive ? branchRepository.findAll(pageable)
                : branchRepository.findByActive(true, pageable);
        List<BranchSummaryResponse> items = pageResult.getContent().stream()
                .map(mapper::toSummary)
                .toList();
        return new PaginatedResponse<>(items, pageResult.getTotalElements(), safeOffset, size);
    }

    @Transactional(readOnly = true)
    public BranchDetailResponse detail(UUID id) {
        Branch branch = find(id);
        return mapper.toDetail(branch, warehouseRepository.countByActiveTrueAndBranch_Id(id));
    }

    @Transactional
    public BranchDetailResponse create(CreateBranchRequest request) {
        String code = normalize(request.code());
        if (branchRepository.existsByCodeIgnoreCase(code)) {
            throw new DuplicateResourceException("Ya existe una sucursal con el codigo: " + code);
        }
        Branch branch = new Branch();
        branch.setCode(code);
        branch.setName(request.name().trim());
        branch.setCity(request.city().trim());
        branch.setActive(true);
        branchRepository.save(branch);
        return mapper.toDetail(branch, 0L);
    }

    @Transactional
    public BranchDetailResponse update(UUID id, UpdateBranchRequest request) {
        Branch branch = find(id);
        String code = normalize(request.code());
        if (branchRepository.existsByCodeIgnoreCaseAndIdNot(code, id)) {
            throw new DuplicateResourceException("Ya existe una sucursal con el codigo: " + code);
        }
        branch.setCode(code);
        branch.setName(request.name().trim());
        branch.setCity(request.city().trim());
        branchRepository.save(branch);
        return mapper.toDetail(branch, warehouseRepository.countByActiveTrueAndBranch_Id(id));
    }

    @Transactional
    public BranchDetailResponse deactivate(UUID id) {
        Branch branch = find(id);
        if (!branch.isActive()) {
            throw new ResourceNotFoundException("Sucursal no encontrada: " + id);
        }
        if (warehouseRepository.existsByActiveTrueAndBranch_Id(id)) {
            throw new ResourceInUseException(
                    "No se puede desactivar la sucursal porque tiene almacenes activos asociados");
        }
        branch.setActive(false);
        branchRepository.save(branch);
        return mapper.toDetail(branch, 0L);
    }

    private Branch find(UUID id) {
        return branchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada: " + id));
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
