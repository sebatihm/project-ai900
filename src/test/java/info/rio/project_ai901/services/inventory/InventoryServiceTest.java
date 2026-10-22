package info.rio.project_ai901.services.inventory;

import info.rio.project_ai901.api.dto.CreateInventoryRequest;
import info.rio.project_ai901.api.dto.InventoryDetailResponse;
import info.rio.project_ai901.api.dto.UpdateInventoryRequest;
import info.rio.project_ai901.exceptions.DuplicateResourceException;
import info.rio.project_ai901.exceptions.InvalidRequestException;
import info.rio.project_ai901.exceptions.ResourceNotFoundException;
import info.rio.project_ai901.models.Branch;
import info.rio.project_ai901.models.Inventory;
import info.rio.project_ai901.models.Product;
import info.rio.project_ai901.models.Warehouse;
import info.rio.project_ai901.repositories.InventoryRepository;
import info.rio.project_ai901.repositories.ProductRepository;
import info.rio.project_ai901.repositories.WarehouseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InventoryServiceTest {

    private InventoryRepository inventoryRepository;
    private WarehouseRepository warehouseRepository;
    private ProductRepository productRepository;
    private InventoryService service;

    @BeforeEach
    void setUp() {
        inventoryRepository = Mockito.mock(InventoryRepository.class);
        warehouseRepository = Mockito.mock(WarehouseRepository.class);
        productRepository = Mockito.mock(ProductRepository.class);
        service = new InventoryService(inventoryRepository, warehouseRepository, productRepository,
                new InventoryMapper());
    }

    @Test
    void createsInventoryForActiveWarehouse() {
        UUID warehouseId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        when(warehouseRepository.findById(warehouseId)).thenReturn(Optional.of(warehouse(warehouseId, true)));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product(productId)));
        when(inventoryRepository.existsByWarehouse_IdAndProduct_Id(warehouseId, productId)).thenReturn(false);

        InventoryDetailResponse result = service.create(new CreateInventoryRequest(warehouseId, productId, 25));

        assertThat(result.quantity()).isEqualTo(25);
        assertThat(result.warehouseId()).isEqualTo(warehouseId);
        assertThat(result.productId()).isEqualTo(productId);
        verify(inventoryRepository).save(any(Inventory.class));
    }

    @Test
    void acceptsZeroQuantity() {
        UUID warehouseId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        when(warehouseRepository.findById(warehouseId)).thenReturn(Optional.of(warehouse(warehouseId, true)));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product(productId)));
        when(inventoryRepository.existsByWarehouse_IdAndProduct_Id(warehouseId, productId)).thenReturn(false);

        InventoryDetailResponse result = service.create(new CreateInventoryRequest(warehouseId, productId, 0));

        assertThat(result.quantity()).isZero();
        verify(inventoryRepository).save(any(Inventory.class));
    }

    @Test
    void rejectsDuplicatedWarehouseProductPair() {
        UUID warehouseId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        when(warehouseRepository.findById(warehouseId)).thenReturn(Optional.of(warehouse(warehouseId, true)));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product(productId)));
        when(inventoryRepository.existsByWarehouse_IdAndProduct_Id(warehouseId, productId)).thenReturn(true);

        assertThatThrownBy(() -> service.create(new CreateInventoryRequest(warehouseId, productId, 10)))
                .isInstanceOf(DuplicateResourceException.class);
        verify(inventoryRepository, never()).save(any(Inventory.class));
    }

    @Test
    void rejectsInactiveWarehouse() {
        UUID warehouseId = UUID.randomUUID();
        when(warehouseRepository.findById(warehouseId)).thenReturn(Optional.of(warehouse(warehouseId, false)));

        assertThatThrownBy(() -> service.create(
                new CreateInventoryRequest(warehouseId, UUID.randomUUID(), 10)))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void throwsNotFoundForUnknownWarehouse() {
        UUID warehouseId = UUID.randomUUID();
        when(warehouseRepository.findById(warehouseId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(
                new CreateInventoryRequest(warehouseId, UUID.randomUUID(), 10)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void throwsNotFoundForUnknownProduct() {
        UUID warehouseId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        when(warehouseRepository.findById(warehouseId)).thenReturn(Optional.of(warehouse(warehouseId, true)));
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(new CreateInventoryRequest(warehouseId, productId, 10)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void throwsNotFoundForUnknownInventory() {
        UUID id = UUID.randomUUID();
        when(inventoryRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.detail(id)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.delete(id)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updatesQuantityOfInventoryInActiveWarehouse() {
        UUID id = UUID.randomUUID();
        Inventory inventory = inventory(id, warehouse(UUID.randomUUID(), true));
        when(inventoryRepository.findById(id)).thenReturn(Optional.of(inventory));

        InventoryDetailResponse result = service.update(id, new UpdateInventoryRequest(42));

        assertThat(result.quantity()).isEqualTo(42);
        verify(inventoryRepository).save(any(Inventory.class));
    }

    @Test
    void rejectsUpdateOfInventoryInInactiveWarehouse() {
        UUID id = UUID.randomUUID();
        Inventory inventory = inventory(id, warehouse(UUID.randomUUID(), false));
        when(inventoryRepository.findById(id)).thenReturn(Optional.of(inventory));

        assertThatThrownBy(() -> service.update(id,
                new UpdateInventoryRequest(42)))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void deletesInventory() {
        UUID id = UUID.randomUUID();
        Inventory inventory = inventory(id, warehouse(UUID.randomUUID(), true));
        when(inventoryRepository.findById(id)).thenReturn(Optional.of(inventory));

        service.delete(id);

        verify(inventoryRepository).delete(inventory);
    }

    private Warehouse warehouse(UUID id, boolean active) {
        Branch branch = new Branch();
        branch.setId(UUID.randomUUID());
        branch.setCode("SUC-01");
        branch.setName("Sucursal Centro");
        branch.setCity("CDMX");
        branch.setActive(true);

        Warehouse warehouse = new Warehouse();
        warehouse.setId(id);
        warehouse.setCode("ALM-01");
        warehouse.setName("Almacen Norte");
        warehouse.setActive(active);
        warehouse.setBranch(branch);
        return warehouse;
    }

    private Product product(UUID id) {
        Product product = new Product();
        product.setId(id);
        product.setSku("SKU-001");
        product.setName("Coca Cola");
        product.setUnit("ml");
        product.setPrice(new BigDecimal("12.50"));
        return product;
    }

    private Inventory inventory(UUID id, Warehouse warehouse) {
        Inventory inventory = new Inventory();
        inventory.setId(id);
        inventory.setWarehouse(warehouse);
        inventory.setProduct(product(UUID.randomUUID()));
        inventory.setQuantity(10);
        return inventory;
    }
}
