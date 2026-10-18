package info.rio.project_ai901.services.branch;

import info.rio.project_ai901.api.dto.BranchDetailResponse;
import info.rio.project_ai901.api.dto.CreateBranchRequest;
import info.rio.project_ai901.exceptions.DuplicateResourceException;
import info.rio.project_ai901.exceptions.ResourceInUseException;
import info.rio.project_ai901.exceptions.ResourceNotFoundException;
import info.rio.project_ai901.models.Branch;
import info.rio.project_ai901.repositories.BranchRepository;
import info.rio.project_ai901.repositories.WarehouseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class BranchServiceTest {

    private BranchRepository branchRepository;
    private WarehouseRepository warehouseRepository;
    private BranchService service;

    @BeforeEach
    void setUp() {
        branchRepository = Mockito.mock(BranchRepository.class);
        warehouseRepository = Mockito.mock(WarehouseRepository.class);
        service = new BranchService(branchRepository, warehouseRepository, new BranchMapper());
    }

    @Test
    void createsBranchWithNormalizedCode() {
        when(branchRepository.existsByCodeIgnoreCase("SUC-01")).thenReturn(false);

        BranchDetailResponse result = service.create(new CreateBranchRequest("  suc-01 ", "Sucursal Centro", "CDMX"));

        assertThat(result.code()).isEqualTo("suc-01");
        assertThat(result.name()).isEqualTo("Sucursal Centro");
        assertThat(result.active()).isTrue();
        Mockito.verify(branchRepository).save(any(Branch.class));
    }

    @Test
    void rejectsDuplicatedCode() {
        when(branchRepository.existsByCodeIgnoreCase("SUC-01")).thenReturn(true);

        assertThatThrownBy(
                () -> service.create(new CreateBranchRequest("SUC-01", "Sucursal Centro", "CDMX")))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void throwsNotFoundForUnknownBranch() {
        UUID id = UUID.randomUUID();
        when(branchRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.detail(id)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void refusesToDeactivateBranchWithActiveWarehouses() {
        UUID id = UUID.randomUUID();
        when(branchRepository.findById(id)).thenReturn(Optional.of(branch(id, true)));
        when(warehouseRepository.existsByActiveTrueAndBranch_Id(id)).thenReturn(true);

        assertThatThrownBy(() -> service.deactivate(id)).isInstanceOf(ResourceInUseException.class);
    }

    @Test
    void deactivatesBranchWhenNoActiveWarehouses() {
        UUID id = UUID.randomUUID();
        Branch branch = branch(id, true);
        when(branchRepository.findById(id)).thenReturn(Optional.of(branch));
        when(warehouseRepository.existsByActiveTrueAndBranch_Id(id)).thenReturn(false);

        BranchDetailResponse result = service.deactivate(id);

        assertThat(branch.isActive()).isFalse();
        assertThat(result.active()).isFalse();
    }

    private Branch branch(UUID id, boolean active) {
        Branch branch = new Branch();
        branch.setId(id);
        branch.setCode("SUC-01");
        branch.setName("Sucursal Centro");
        branch.setCity("CDMX");
        branch.setActive(active);
        return branch;
    }
}
