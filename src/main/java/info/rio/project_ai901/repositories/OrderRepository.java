package info.rio.project_ai901.repositories;

import info.rio.project_ai901.models.Order;
import info.rio.project_ai901.models.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    boolean existsByCodeIgnoreCase(String code);

    long countByCustomer_Id(UUID customerId);

    long countByOrderDateBetween(LocalDateTime start, LocalDateTime end);

    Optional<Order> findByCodeIgnoreCase(String code);

    @Query("select o from Order o where (:customerId is null or o.customer.id = :customerId) "
            + "and (:branchId is null or o.branch.id = :branchId) "
            + "and (:status is null or o.status = :status)")
    Page<Order> search(@Param("customerId") UUID customerId, @Param("branchId") UUID branchId,
            @Param("status") OrderStatus status, Pageable pageable);
}
