package info.rio.project_ai901.services.customer;

import info.rio.project_ai901.api.dto.CustomerDetailResponse;
import info.rio.project_ai901.api.dto.CustomerSummaryResponse;
import info.rio.project_ai901.models.Branch;
import info.rio.project_ai901.models.Customer;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CustomerMapper {

    public CustomerSummaryResponse toSummary(Customer customer) {
        return new CustomerSummaryResponse(
                customer.getId(),
                customer.getName(),
                customer.getType(),
                customer.getEmail(),
                customer.getPhone());
    }

    public CustomerDetailResponse toDetail(Customer customer) {
        Branch branch = customer.getBranch();
        UUID branchId = branch != null ? branch.getId() : null;
        String branchName = branch != null ? branch.getName() : null;
        return new CustomerDetailResponse(
                customer.getId(),
                customer.getName(),
                customer.getType(),
                customer.getEmail(),
                customer.getPhone(),
                branchId,
                branchName);
    }
}
