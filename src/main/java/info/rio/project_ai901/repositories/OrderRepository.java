package info.rio.project_ai901.repositories;

import info.rio.project_ai901.models.Order;
import info.rio.project_ai901.models.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
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

        @Query("""
                select o.branch.id as branchId, o.branch.code as branchCode, o.branch.name as branchName,
                count(o) as orders, sum(o.total) as total
                from Order o
                where o.status = :status
                and o.orderDate >= :from and o.orderDate < :to
                and (:branchId is null or o.branch.id = :branchId)
                group by o.branch.id, o.branch.code, o.branch.name
                order by sum(o.total) desc
                """)
        List<ReportRows.BranchSales> salesByBranch(@Param("status") OrderStatus status,
                                                @Param("from") LocalDateTime from,
                                                @Param("to") LocalDateTime to,
                                                @Param("branchId") UUID branchId);

        @Query("""
                select o.code as code, o.orderDate as orderDate, o.branch.name as branchName, o.total as total
                from Order o
                where o.status = :status
                and o.orderDate >= :from and o.orderDate < :to
                and (:branchId is null or o.branch.id = :branchId)
                order by o.orderDate asc
                """)
        List<ReportRows.OrderRow> deliveredRows(@Param("status") OrderStatus status,
                                        @Param("from") LocalDateTime from,
                                        @Param("to") LocalDateTime to,
                                        @Param("branchId") UUID branchId,
                                        Pageable pageable);
}
