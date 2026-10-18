package info.rio.project_ai901.services.warehouse;

import info.rio.project_ai901.api.dto.CreateWarehouseRequest;
import info.rio.project_ai901.api.dto.WarehouseDetailResponse;
import info.rio.project_ai901.exceptions.DuplicateResourceException;
import info.rio.project_ai901.exceptions.InvalidRequestException;
import info.rio.project_ai901.exceptions.ResourceNotFoundException;
import info.rio.project_ai901.models.Branch;
import info.rio.project_ai901.models.Warehouse;
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

class WarehouseServiceTest {

    private WarehouseRepository warehouseRepository;
    private BranchRepository branchRepository;
    private WarehouseService service;

    @BeforeEach
    void setUp() {
        warehouseRepository = Mockito.mock(WarehouseRepository.class);
        branchRepository = Mockito.mock(BranchRepository.class);
        service = new WarehouseService(warehouseRepository, branchRepository, new WarehouseMapper());
    }

    @Test
    void createsWarehouseForActiveBranch() {
        UUID branchId = UUID.randomUUID();
        when(warehouseRepository.existsByCodeIgnoreCase("ALM-01")).thenReturn(false);
        when(branchRepository.findById(branchId)).thenReturn(Optional.of(branch(branchId, true)));

        WarehouseDetailResponse result = service.create(new CreateWarehouseRequest("ALM-01", "Almacen Norte", branchId));

        assertThat(result.code()).isEqualTo("ALM-01");
        assertThat(result.branchId()).isEqualTo(branchId);
        assertThat(result.active()).isTrue();
        Mockito.verify(warehouseRepository).save(any(Warehouse.class));
    }

    @Test
    void rejectsDuplicatedCode() {
        when(warehouseRepository.existsByCodeIgnoreCase("ALM-01")).thenReturn(true);

        assertThatThrownBy(() -> service.create(
                new CreateWarehouseRequest("ALM-01", "Almacen Norte", UUID.randomUUID())))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void rejectsUnknownBranch() {
        UUID branchId = UUID.randomUUID();
        when(warehouseRepository.existsByCodeIgnoreCase("ALM-01")).thenReturn(false);
        when(branchRepository.findById(branchId)).thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> service.create(new CreateWarehouseRequest("ALM-01", "Almacen Norte", branchId)))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void rejectsInactiveBranch() {
        UUID branchId = UUID.randomUUID();
        when(warehouseRepository.existsByCodeIgnoreCase("ALM-01")).thenReturn(false);
        when(branchRepository.findById(branchId)).thenReturn(Optional.of(branch(branchId, false)));

        assertThatThrownBy(
                () -> service.create(new CreateWarehouseRequest("ALM-01", "Almacen Norte", branchId)))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void throwsNotFoundForUnknownWarehouse() {
        UUID id = UUID.randomUUID();
        when(warehouseRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.detail(id)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deactivatesWarehouse() {
        UUID id = UUID.randomUUID();
        Warehouse warehouse = warehouse(id, true);
        when(warehouseRepository.findById(id)).thenReturn(Optional.of(warehouse));

        WarehouseDetailResponse result = service.deactivate(id);

        assertThat(warehouse.isActive()).isFalse();
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

    private Warehouse warehouse(UUID id, boolean active) {
        Warehouse warehouse = new Warehouse();
        warehouse.setId(id);
        warehouse.setCode("ALM-01");
        warehouse.setName("Almacen Norte");
        warehouse.setActive(active);
        warehouse.setBranch(branch(UUID.randomUUID(), true));
        return warehouse;
    }
}
