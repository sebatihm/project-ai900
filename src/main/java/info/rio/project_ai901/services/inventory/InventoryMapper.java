package info.rio.project_ai901.services.inventory;

import info.rio.project_ai901.api.dto.InventoryDetailResponse;
import info.rio.project_ai901.api.dto.InventorySummaryResponse;
import info.rio.project_ai901.models.Inventory;
import info.rio.project_ai901.models.Product;
import info.rio.project_ai901.models.Warehouse;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class InventoryMapper {

    public InventorySummaryResponse toSummary(Inventory inventory) {
        Warehouse warehouse = inventory.getWarehouse();
        Product product = inventory.getProduct();
        return new InventorySummaryResponse(
                inventory.getId(),
                warehouse != null ? warehouse.getId() : null,
                warehouse != null ? warehouse.getCode() : null,
                warehouse != null ? warehouse.getName() : null,
                product != null ? product.getId() : null,
                product != null ? product.getSku() : null,
                product != null ? product.getName() : null,
                inventory.getQuantity());
    }

    public InventoryDetailResponse toDetail(Inventory inventory) {
        Warehouse warehouse = inventory.getWarehouse();
        Product product = inventory.getProduct();
        UUID branchId = null;
        String branchName = null;
        if (warehouse != null && warehouse.getBranch() != null) {
            branchId = warehouse.getBranch().getId();
            branchName = warehouse.getBranch().getName();
        }
        return new InventoryDetailResponse(
                inventory.getId(),
                warehouse != null ? warehouse.getId() : null,
                warehouse != null ? warehouse.getCode() : null,
                warehouse != null ? warehouse.getName() : null,
                branchId,
                branchName,
                product != null ? product.getId() : null,
                product != null ? product.getSku() : null,
                product != null ? product.getName() : null,
                inventory.getQuantity());
    }
}
