package info.rio.project_ai901.services.inventory;

import info.rio.project_ai901.api.dto.CreateInventoryRequest;
import info.rio.project_ai901.api.dto.InventoryDetailResponse;
import info.rio.project_ai901.api.dto.InventorySummaryResponse;
import info.rio.project_ai901.api.dto.PaginatedResponse;
import info.rio.project_ai901.api.dto.UpdateInventoryRequest;
import info.rio.project_ai901.exceptions.DuplicateResourceException;
import info.rio.project_ai901.exceptions.InvalidRequestException;
import info.rio.project_ai901.exceptions.ResourceNotFoundException;
import info.rio.project_ai901.models.Inventory;
import info.rio.project_ai901.models.Product;
import info.rio.project_ai901.models.Warehouse;
import info.rio.project_ai901.repositories.InventoryRepository;
import info.rio.project_ai901.repositories.ProductRepository;
import info.rio.project_ai901.repositories.WarehouseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final WarehouseRepository warehouseRepository;
    private final ProductRepository productRepository;
    private final InventoryMapper mapper;

    public InventoryService(InventoryRepository inventoryRepository, WarehouseRepository warehouseRepository,
            ProductRepository productRepository, InventoryMapper mapper) {
        this.inventoryRepository = inventoryRepository;
        this.warehouseRepository = warehouseRepository;
        this.productRepository = productRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<InventorySummaryResponse> list(UUID warehouseId, UUID productId, long offset, int limit) {
        int size = clampLimit(limit);
        long safeOffset = Math.max(offset, 0);
        PageRequest pageable = PageRequest.of((int) (safeOffset / size), size, Sort.by("product.sku").ascending());
        Page<Inventory> pageResult = fetch(warehouseId, productId, pageable);
        List<InventorySummaryResponse> items = pageResult.getContent().stream()
                .map(mapper::toSummary)
                .toList();
        return new PaginatedResponse<>(items, pageResult.getTotalElements(), safeOffset, size);
    }

    @Transactional(readOnly = true)
    public InventoryDetailResponse detail(UUID id) {
        return mapper.toDetail(find(id));
    }

    @Transactional
    public InventoryDetailResponse create(CreateInventoryRequest request) {
        Warehouse warehouse = resolveWarehouse(request.warehouseId());
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + request.productId()));
        if (inventoryRepository.existsByWarehouse_IdAndProduct_Id(warehouse.getId(), product.getId())) {
            throw new DuplicateResourceException("Ya existe inventario para el almacen " + warehouse.getCode()
                    + " y el producto " + product.getSku());
        }
        Inventory inventory = new Inventory();
        inventory.setWarehouse(warehouse);
        inventory.setProduct(product);
        inventory.setQuantity(request.quantity());
        inventoryRepository.save(inventory);
        return mapper.toDetail(inventory);
    }

    @Transactional
    public InventoryDetailResponse update(UUID id, UpdateInventoryRequest request) {
        Inventory inventory = find(id);
        assertWarehouseActive(inventory.getWarehouse());
        inventory.setQuantity(request.quantity());
        inventoryRepository.save(inventory);
        return mapper.toDetail(inventory);
    }

    @Transactional
    public void delete(UUID id) {
        Inventory inventory = find(id);
        inventoryRepository.delete(inventory);
    }

    private Page<Inventory> fetch(UUID warehouseId, UUID productId, PageRequest pageable) {
        if (warehouseId != null && productId != null) {
            return inventoryRepository.findByWarehouse_IdAndProduct_Id(warehouseId, productId, pageable);
        }
        if (warehouseId != null) {
            return inventoryRepository.findByWarehouse_Id(warehouseId, pageable);
        }
        if (productId != null) {
            return inventoryRepository.findByProduct_Id(productId, pageable);
        }
        return inventoryRepository.findAll(pageable);
    }

    private Warehouse resolveWarehouse(UUID warehouseId) {
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("Almacen no encontrado: " + warehouseId));
        assertWarehouseActive(warehouse);
        return warehouse;
    }

    private void assertWarehouseActive(Warehouse warehouse) {
        if (warehouse == null || !warehouse.isActive()) {
            throw new InvalidRequestException("El almacen no esta activo");
        }
    }

    private Inventory find(UUID id) {
        return inventoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inventario no encontrado: " + id));
    }

    private int clampLimit(int limit) {
        if (limit <= 0) {
            return 20;
        }
        return Math.min(limit, 100);
    }
}
