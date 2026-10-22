package info.rio.project_ai901.repositories;

import info.rio.project_ai901.models.Customer;
import info.rio.project_ai901.models.enums.CustomerType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, UUID id);

    Page<Customer> findByType(CustomerType type, Pageable pageable);

    Page<Customer> findByBranch_Id(UUID branchId, Pageable pageable);

    Page<Customer> findByTypeAndBranch_Id(CustomerType type, UUID branchId, Pageable pageable);
}
