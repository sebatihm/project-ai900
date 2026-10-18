package info.rio.project_ai901.services.branch;

import info.rio.project_ai901.api.dto.BranchDetailResponse;
import info.rio.project_ai901.api.dto.BranchSummaryResponse;
import info.rio.project_ai901.models.Branch;
import org.springframework.stereotype.Component;

@Component
public class BranchMapper {

    public BranchSummaryResponse toSummary(Branch branch) {
        return new BranchSummaryResponse(branch.getId(), branch.getCode(), branch.getName(), branch.getCity());
    }

    public BranchDetailResponse toDetail(Branch branch, long warehouseCount) {
        return new BranchDetailResponse(branch.getId(), branch.getCode(), branch.getName(), branch.getCity(),
                branch.isActive(), warehouseCount);
    }
}
