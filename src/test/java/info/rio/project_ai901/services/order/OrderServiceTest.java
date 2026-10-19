package info.rio.project_ai901.services.order;

import info.rio.project_ai901.api.dto.CreateOrderItemRequest;
import info.rio.project_ai901.api.dto.CreateOrderRequest;
import info.rio.project_ai901.api.dto.OrderDetailResponse;
import info.rio.project_ai901.api.dto.UpdateOrderRequest;
import info.rio.project_ai901.exceptions.InvalidRequestException;
import info.rio.project_ai901.exceptions.ResourceInUseException;
import info.rio.project_ai901.exceptions.ResourceNotFoundException;
import info.rio.project_ai901.models.Branch;
import info.rio.project_ai901.models.Customer;
import info.rio.project_ai901.models.Order;
import info.rio.project_ai901.models.OrderItem;
import info.rio.project_ai901.models.Product;
import info.rio.project_ai901.models.enums.CustomerType;
import info.rio.project_ai901.models.enums.OrderStatus;
import info.rio.project_ai901.repositories.BranchRepository;
import info.rio.project_ai901.repositories.CustomerRepository;
import info.rio.project_ai901.repositories.OrderItemRepository;
import info.rio.project_ai901.repositories.OrderRepository;
import info.rio.project_ai901.repositories.ProductRepository;
import info.rio.project_ai901.repositories.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderServiceTest {

    private OrderRepository orderRepository;
    private OrderItemRepository orderItemRepository;
    private CustomerRepository customerRepository;
    private BranchRepository branchRepository;
    private ProductRepository productRepository;
    private TicketRepository ticketRepository;
    private OrderService service;

    @BeforeEach
    void setUp() {
        orderRepository = Mockito.mock(OrderRepository.class);
        orderItemRepository = Mockito.mock(OrderItemRepository.class);
        customerRepository = Mockito.mock(CustomerRepository.class);
        branchRepository = Mockito.mock(BranchRepository.class);
        productRepository = Mockito.mock(ProductRepository.class);
        ticketRepository = Mockito.mock(TicketRepository.class);
        service = new OrderService(orderRepository, orderItemRepository, customerRepository, branchRepository,
                productRepository, ticketRepository, new OrderMapper());
    }

    @Test
    void createsOrderWithGeneratedCodeAndSnapshotPrices() {
        UUID customerId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID productA = UUID.randomUUID();
        UUID productB = UUID.randomUUID();
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer(customerId)));
        when(branchRepository.findById(branchId)).thenReturn(Optional.of(branch(branchId)));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            if (order.getId() == null) {
                order.setId(UUID.randomUUID());
            }
            return order;
        });
        when(orderRepository.countByOrderDateBetween(any(), any())).thenReturn(0L);
        when(orderRepository.existsByCodeIgnoreCase(any())).thenReturn(false);
        when(productRepository.findById(productA)).thenReturn(Optional.of(product(productA, "25.00")));
        when(productRepository.findById(productB)).thenReturn(Optional.of(product(productB, "10.00")));

        OrderDetailResponse result = service.create(new CreateOrderRequest(customerId, branchId,
                LocalDateTime.of(2026, 1, 15, 10, 0), LocalDate.of(2026, 1, 20),
                List.of(item(productA, 2), item(productB, 1))));

        assertThat(result.code()).matches("ORD-\\d{8}-0001");
        assertThat(result.status()).isEqualTo(OrderStatus.RECEIVED);
        assertThat(result.total()).isEqualByComparingTo("60.00");
        verify(orderItemRepository).save(argThat(orderItem -> orderItem.getProduct().getId().equals(productA)
                && orderItem.getQuantity() == 2
                && orderItem.getUnitPrice().compareTo(new BigDecimal("25.00")) == 0));
        verify(orderRepository, times(2)).save(any(Order.class));
    }

    @Test
    void rejectsUnknownCustomer() {
        UUID customerId = UUID.randomUUID();
        when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(new CreateOrderRequest(customerId, UUID.randomUUID(),
                null, null, List.of(item(UUID.randomUUID(), 1)))))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void rejectsUnknownBranch() {
        UUID customerId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer(customerId)));
        when(branchRepository.findById(branchId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(new CreateOrderRequest(customerId, branchId,
                null, null, List.of(item(UUID.randomUUID(), 1)))))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void rejectsOrderWithoutItems() {
        UUID customerId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer(customerId)));
        when(branchRepository.findById(branchId)).thenReturn(Optional.of(branch(branchId)));

        assertThatThrownBy(() -> service.create(new CreateOrderRequest(customerId, branchId,
                null, null, List.of())))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void rejectsOrderWithUnknownProductItem() {
        UUID customerId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer(customerId)));
        when(branchRepository.findById(branchId)).thenReturn(Optional.of(branch(branchId)));
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(new CreateOrderRequest(customerId, branchId,
                null, null, List.of(item(productId, 1)))))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void throwsNotFoundForUnknownOrder() {
        UUID id = UUID.randomUUID();
        when(orderRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.detail(id)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.delete(id)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.update(id, null)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.updateStatus(id, OrderStatus.PREPARING))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void refusesToUpdateOrderInNonReceivedStatus() {
        UUID id = UUID.randomUUID();
        when(orderRepository.findById(id)).thenReturn(Optional.of(order(id, OrderStatus.PREPARING)));

        assertThatThrownBy(() -> service.update(id, orderRequest(id, id, List.of(item(UUID.randomUUID(), 1)))))
                .isInstanceOf(ResourceInUseException.class);
        verify(orderItemRepository, never()).deleteByOrder_Id(any(UUID.class));
    }

    @Test
    void updatesReceivedOrder() {
        UUID id = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Order order = order(id, OrderStatus.RECEIVED);
        when(orderRepository.findById(id)).thenReturn(Optional.of(order));
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer(customerId)));
        when(branchRepository.findById(branchId)).thenReturn(Optional.of(branch(branchId)));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product(productId, "25.00")));

        OrderDetailResponse result = service.update(id, orderRequest(customerId, branchId,
                List.of(item(productId, 3))));

        assertThat(result.status()).isEqualTo(OrderStatus.RECEIVED);
        assertThat(result.total()).isEqualByComparingTo("75.00");
        verify(orderItemRepository).deleteByOrder_Id(id);
        verify(orderRepository).save(order);
    }

    @Test
    void acceptsValidStatusTransitions() {
        assertTransitionAccepted(OrderStatus.RECEIVED, OrderStatus.PREPARING);
        assertTransitionAccepted(OrderStatus.RECEIVED, OrderStatus.CANCELED);
        assertTransitionAccepted(OrderStatus.PREPARING, OrderStatus.SHIPPED);
        assertTransitionAccepted(OrderStatus.PREPARING, OrderStatus.CANCELED);
        assertTransitionAccepted(OrderStatus.SHIPPED, OrderStatus.DELIVERED);
    }

    @Test
    void rejectsInvalidStatusTransitions() {
        assertTransitionRejected(OrderStatus.RECEIVED, OrderStatus.SHIPPED);
        assertTransitionRejected(OrderStatus.RECEIVED, OrderStatus.DELIVERED);
        assertTransitionRejected(OrderStatus.PREPARING, OrderStatus.DELIVERED);
        assertTransitionRejected(OrderStatus.SHIPPED, OrderStatus.CANCELED);
        assertTransitionRejected(OrderStatus.DELIVERED, OrderStatus.CANCELED);
        assertTransitionRejected(OrderStatus.CANCELED, OrderStatus.PREPARING);
    }

    @Test
    void rejectsTransitionToSameStatus() {
        UUID id = UUID.randomUUID();
        when(orderRepository.findById(id)).thenReturn(Optional.of(order(id, OrderStatus.RECEIVED)));

        assertThatThrownBy(() -> service.updateStatus(id, OrderStatus.RECEIVED))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void refusesToDeleteOrderNotCanceled() {
        UUID id = UUID.randomUUID();
        when(orderRepository.findById(id)).thenReturn(Optional.of(order(id, OrderStatus.DELIVERED)));

        assertThatThrownBy(() -> service.delete(id)).isInstanceOf(ResourceInUseException.class);
        verify(orderRepository, never()).delete(any(Order.class));
    }

    @Test
    void refusesToDeleteOrderWithTickets() {
        UUID id = UUID.randomUUID();
        when(orderRepository.findById(id)).thenReturn(Optional.of(order(id, OrderStatus.CANCELED)));
        when(ticketRepository.countByOrder_Id(id)).thenReturn(1L);

        assertThatThrownBy(() -> service.delete(id)).isInstanceOf(ResourceInUseException.class);
        verify(orderRepository, never()).delete(any(Order.class));
    }

    @Test
    void deletesCanceledOrderWithoutTickets() {
        UUID id = UUID.randomUUID();
        Order order = order(id, OrderStatus.CANCELED);
        when(orderRepository.findById(id)).thenReturn(Optional.of(order));
        when(ticketRepository.countByOrder_Id(id)).thenReturn(0L);

        service.delete(id);

        verify(orderItemRepository).deleteByOrder_Id(id);
        verify(orderRepository).delete(order);
    }

    private void assertTransitionAccepted(OrderStatus from, OrderStatus to) {
        UUID id = UUID.randomUUID();
        Order order = order(id, from);
        when(orderRepository.findById(id)).thenReturn(Optional.of(order));

        OrderDetailResponse result = service.updateStatus(id, to);

        assertThat(result.status()).isEqualTo(to);
    }

    private void assertTransitionRejected(OrderStatus from, OrderStatus to) {
        UUID id = UUID.randomUUID();
        when(orderRepository.findById(id)).thenReturn(Optional.of(order(id, from)));

        assertThatThrownBy(() -> service.updateStatus(id, to))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining(from.name())
                .hasMessageContaining(to.name());
    }

    private UpdateOrderRequest orderRequest(UUID customerId, UUID branchId,
            List<CreateOrderItemRequest> items) {
        return new UpdateOrderRequest(customerId, branchId, LocalDateTime.of(2026, 1, 15, 10, 0),
                LocalDate.of(2026, 1, 20), items);
    }

    private CreateOrderItemRequest item(UUID productId, int quantity) {
        return new CreateOrderItemRequest(productId, quantity);
    }

    private Customer customer(UUID id) {
        Customer customer = new Customer();
        customer.setId(id);
        customer.setName("Hotel Miramar");
        customer.setType(CustomerType.HOTEL);
        customer.setEmail("contacto@miramar.test");
        return customer;
    }

    private Branch branch(UUID id) {
        Branch branch = new Branch();
        branch.setId(id);
        branch.setCode("SUC-01");
        branch.setName("Sucursal Centro");
        branch.setCity("CDMX");
        branch.setActive(true);
        return branch;
    }

    private Product product(UUID id, String price) {
        Product product = new Product();
        product.setId(id);
        product.setSku("SKU-" + id.toString().substring(0, 4));
        product.setName("Producto");
        product.setUnit("ml");
        product.setPrice(new BigDecimal(price));
        return product;
    }

    private Order order(UUID id, OrderStatus status) {
        Order order = new Order();
        order.setId(id);
        order.setCode("ORD-20260101-0001");
        order.setCustomer(customer(UUID.randomUUID()));
        order.setBranch(branch(UUID.randomUUID()));
        order.setStatus(status);
        order.setOrderDate(LocalDateTime.of(2026, 1, 1, 10, 0));
        order.setEstimatedDeliveryDate(LocalDate.of(2026, 1, 10));
        order.setTotal(new BigDecimal("100.00"));
        return order;
    }
}
