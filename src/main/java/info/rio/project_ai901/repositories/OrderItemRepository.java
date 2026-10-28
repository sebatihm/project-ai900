package info.rio.project_ai901.repositories;

import info.rio.project_ai901.models.OrderItem;
import info.rio.project_ai901.models.enums.OrderStatus;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

    List<OrderItem> findByOrder_Id(UUID orderId);

    long countByOrder_Id(UUID orderId);

    long countByProduct_Id(UUID productId);

    void deleteByOrder_Id(UUID orderId);

    @Query("""
            select i.product.sku as sku, i.product.name as name,
                sum(i.quantity) as units, sum(i.quantity * i.unitPrice) as revenue
            from OrderItem i
            where i.order.status = :status
            and i.order.orderDate >= :from and i.order.orderDate < :to
            and (:branchId is null or i.order.branch.id = :branchId)
            group by i.product.id, i.product.sku, i.product.name
            order by sum(i.quantity * i.unitPrice) desc
            """)
    List<ReportRows.ProductSales> topProducts(@Param("status") OrderStatus status,
                                            @Param("from") LocalDateTime from,
                                            @Param("to") LocalDateTime to,
                                            @Param("branchId") UUID branchId,
                                            Pageable pageable);
}
