package info.rio.project_ai901.repositories;

import info.rio.project_ai901.models.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

    List<OrderItem> findByOrder_Id(UUID orderId);

    long countByOrder_Id(UUID orderId);

    long countByProduct_Id(UUID productId);

    void deleteByOrder_Id(UUID orderId);
}
