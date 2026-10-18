package info.rio.project_ai901.services.order;

import info.rio.project_ai901.api.dto.OrderDetailResponse;
import info.rio.project_ai901.api.dto.OrderItemResponse;
import info.rio.project_ai901.api.dto.OrderSummaryResponse;
import info.rio.project_ai901.models.Branch;
import info.rio.project_ai901.models.Customer;
import info.rio.project_ai901.models.Order;
import info.rio.project_ai901.models.OrderItem;
import info.rio.project_ai901.models.Product;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Component
public class OrderMapper {

    public OrderSummaryResponse toSummary(Order order) {
        Customer customer = order.getCustomer();
        Branch branch = order.getBranch();
        return new OrderSummaryResponse(
                order.getId(),
                order.getCode(),
                order.getStatus(),
                customer != null ? customer.getId() : null,
                customer != null ? customer.getName() : null,
                branch != null ? branch.getId() : null,
                branch != null ? branch.getCode() : null,
                order.getOrderDate(),
                order.getEstimatedDeliveryDate(),
                order.getTotal());
    }

    public OrderItemResponse toItem(OrderItem item) {
        Product product = item.getProduct();
        BigDecimal lineTotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
        return new OrderItemResponse(
                item.getId(),
                product != null ? product.getId() : null,
                product != null ? product.getSku() : null,
                product != null ? product.getName() : null,
                item.getQuantity(),
                item.getUnitPrice(),
                lineTotal);
    }

    public OrderDetailResponse toDetail(Order order, List<OrderItem> items) {
        Customer customer = order.getCustomer();
        Branch branch = order.getBranch();
        UUID branchId = branch != null ? branch.getId() : null;
        List<OrderItemResponse> itemResponses = items.stream().map(this::toItem).toList();
        return new OrderDetailResponse(
                order.getId(),
                order.getCode(),
                order.getStatus(),
                customer != null ? customer.getId() : null,
                customer != null ? customer.getName() : null,
                branchId,
                branch != null ? branch.getCode() : null,
                branch != null ? branch.getName() : null,
                order.getOrderDate(),
                order.getEstimatedDeliveryDate(),
                order.getTotal(),
                itemResponses);
    }
}
