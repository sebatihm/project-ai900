package info.rio.project_ai901.services.warehouse;

import info.rio.project_ai901.api.dto.WarehouseDetailResponse;
import info.rio.project_ai901.api.dto.WarehouseSummaryResponse;
import info.rio.project_ai901.models.Warehouse;
import org.springframework.stereotype.Component;

@Component
public class WarehouseMapper {

    public WarehouseSummaryResponse toSummary(Warehouse warehouse) {
        return new WarehouseSummaryResponse(warehouse.getId(), warehouse.getCode(), warehouse.getName(),
                branchId(warehouse), branchName(warehouse));
    }

    public WarehouseDetailResponse toDetail(Warehouse warehouse) {
        return new WarehouseDetailResponse(warehouse.getId(), warehouse.getCode(), warehouse.getName(),
                warehouse.isActive(), branchId(warehouse), branchName(warehouse), branchCity(warehouse));
    }

    private java.util.UUID branchId(Warehouse warehouse) {
        return warehouse.getBranch() == null ? null : warehouse.getBranch().getId();
    }

    private String branchName(Warehouse warehouse) {
        return warehouse.getBranch() == null ? null : warehouse.getBranch().getName();
    }

    private String branchCity(Warehouse warehouse) {
        return warehouse.getBranch() == null ? null : warehouse.getBranch().getCity();
    }
}
