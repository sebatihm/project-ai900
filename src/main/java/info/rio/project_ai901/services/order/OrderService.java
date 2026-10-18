package info.rio.project_ai901.services.order;

import info.rio.project_ai901.api.dto.CreateOrderItemRequest;
import info.rio.project_ai901.api.dto.CreateOrderRequest;
import info.rio.project_ai901.api.dto.OrderDetailResponse;
import info.rio.project_ai901.api.dto.OrderSummaryResponse;
import info.rio.project_ai901.api.dto.PaginatedResponse;
import info.rio.project_ai901.api.dto.UpdateOrderRequest;
import info.rio.project_ai901.exceptions.InvalidRequestException;
import info.rio.project_ai901.exceptions.ResourceInUseException;
import info.rio.project_ai901.exceptions.ResourceNotFoundException;
import info.rio.project_ai901.models.Branch;
import info.rio.project_ai901.models.Customer;
import info.rio.project_ai901.models.Order;
import info.rio.project_ai901.models.OrderItem;
import info.rio.project_ai901.models.Product;
import info.rio.project_ai901.models.enums.OrderStatus;
import info.rio.project_ai901.repositories.BranchRepository;
import info.rio.project_ai901.repositories.CustomerRepository;
import info.rio.project_ai901.repositories.OrderItemRepository;
import info.rio.project_ai901.repositories.OrderRepository;
import info.rio.project_ai901.repositories.ProductRepository;
import info.rio.project_ai901.repositories.TicketRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class OrderService {

    private static final DateTimeFormatter CODE_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private static final Map<OrderStatus, Set<OrderStatus>> TRANSITIONS = Map.of(
            OrderStatus.RECEIVED, EnumSet.of(OrderStatus.PREPARING, OrderStatus.CANCELED),
            OrderStatus.PREPARING, EnumSet.of(OrderStatus.SHIPPED, OrderStatus.CANCELED),
            OrderStatus.SHIPPED, EnumSet.of(OrderStatus.DELIVERED),
            OrderStatus.DELIVERED, EnumSet.noneOf(OrderStatus.class),
            OrderStatus.CANCELED, EnumSet.noneOf(OrderStatus.class));

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CustomerRepository customerRepository;
    private final BranchRepository branchRepository;
    private final ProductRepository productRepository;
    private final TicketRepository ticketRepository;
    private final OrderMapper mapper;

    public OrderService(OrderRepository orderRepository, OrderItemRepository orderItemRepository,
            CustomerRepository customerRepository, BranchRepository branchRepository,
            ProductRepository productRepository, TicketRepository ticketRepository, OrderMapper mapper) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.customerRepository = customerRepository;
        this.branchRepository = branchRepository;
        this.productRepository = productRepository;
        this.ticketRepository = ticketRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<OrderSummaryResponse> list(UUID customerId, UUID branchId, OrderStatus status,
            long offset, int limit) {
        int size = clampLimit(limit);
        long safeOffset = Math.max(offset, 0);
        PageRequest pageable = PageRequest.of((int) (safeOffset / size), size,
                Sort.by("orderDate").descending());
        Page<Order> pageResult = orderRepository.search(customerId, branchId, status, pageable);
        List<OrderSummaryResponse> items = pageResult.getContent().stream()
                .map(mapper::toSummary)
                .toList();
        return new PaginatedResponse<>(items, pageResult.getTotalElements(), safeOffset, size);
    }

    @Transactional(readOnly = true)
    public OrderDetailResponse detail(UUID id) {
        Order order = find(id);
        return mapper.toDetail(order, orderItemRepository.findByOrder_Id(id));
    }

    @Transactional
    public OrderDetailResponse create(CreateOrderRequest request) {
        Customer customer = resolveCustomer(request.customerId());
        Branch branch = resolveBranch(request.branchId());

        Order order = new Order();
        order.setCode(generateCode());
        order.setCustomer(customer);
        order.setBranch(branch);
        order.setStatus(OrderStatus.RECEIVED);
        order.setOrderDate(request.orderDate() != null ? request.orderDate() : LocalDateTime.now());
        order.setEstimatedDeliveryDate(request.estimatedDeliveryDate());
        order.setTotal(BigDecimal.ZERO);
        orderRepository.save(order);

        BigDecimal total = saveItems(order, request.items());
        order.setTotal(total);
        orderRepository.save(order);

        return mapper.toDetail(order, orderItemRepository.findByOrder_Id(order.getId()));
    }

    @Transactional
    public OrderDetailResponse update(UUID id, UpdateOrderRequest request) {
        Order order = find(id);
        if (order.getStatus() != OrderStatus.RECEIVED) {
            throw new ResourceInUseException(
                    "Solo se puede editar una orden en estado RECEIVED (estado actual: " + order.getStatus() + ")");
        }
        order.setCustomer(resolveCustomer(request.customerId()));
        order.setBranch(resolveBranch(request.branchId()));
        if (request.orderDate() != null) {
            order.setOrderDate(request.orderDate());
        }
        order.setEstimatedDeliveryDate(request.estimatedDeliveryDate());

        orderItemRepository.deleteByOrder_Id(id);
        BigDecimal total = saveItems(order, request.items());
        order.setTotal(total);
        orderRepository.save(order);

        return mapper.toDetail(order, orderItemRepository.findByOrder_Id(order.getId()));
    }

    @Transactional
    public OrderDetailResponse updateStatus(UUID id, OrderStatus newStatus) {
        Order order = find(id);
        OrderStatus current = order.getStatus();
        if (current == newStatus) {
            throw new InvalidRequestException("La orden ya se encuentra en el estado " + newStatus);
        }
        Set<OrderStatus> allowed = TRANSITIONS.getOrDefault(current, EnumSet.noneOf(OrderStatus.class));
        if (!allowed.contains(newStatus)) {
            throw new InvalidRequestException(
                    "Transicion de estado no permitida: " + current + " -> " + newStatus);
        }
        order.setStatus(newStatus);
        orderRepository.save(order);
        return mapper.toDetail(order, orderItemRepository.findByOrder_Id(order.getId()));
    }

    @Transactional
    public void delete(UUID id) {
        Order order = find(id);
        if (order.getStatus() != OrderStatus.CANCELED) {
            throw new ResourceInUseException(
                    "Solo se puede eliminar una orden en estado CANCELED (estado actual: " + order.getStatus() + ")");
        }
        if (ticketRepository.countByOrder_Id(id) > 0) {
            throw new ResourceInUseException("No se puede eliminar la orden porque tiene tickets asociados");
        }
        orderItemRepository.deleteByOrder_Id(id);
        orderRepository.delete(order);
    }

    private BigDecimal saveItems(Order order, List<CreateOrderItemRequest> items) {
        if (items == null || items.isEmpty()) {
            throw new InvalidRequestException("La orden debe tener al menos un item");
        }
        BigDecimal total = BigDecimal.ZERO;
        for (CreateOrderItemRequest itemRequest : items) {
            Product product = productRepository.findById(itemRequest.productId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Producto no encontrado: " + itemRequest.productId()));
            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProduct(product);
            item.setQuantity(itemRequest.quantity());
            item.setUnitPrice(product.getPrice());
            orderItemRepository.save(item);
            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(itemRequest.quantity())));
        }
        return total.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private String generateCode() {
        LocalDateTime now = LocalDateTime.now();
        String prefix = "ORD-" + now.format(CODE_DATE) + "-";
        LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = now.toLocalDate().atTime(LocalTime.MAX);
        long sequence = orderRepository.countByOrderDateBetween(startOfDay, endOfDay) + 1;
        String candidate = prefix + String.format("%04d", sequence);
        while (orderRepository.existsByCodeIgnoreCase(candidate)) {
            sequence++;
            candidate = prefix + String.format("%04d", sequence);
        }
        return candidate;
    }

    private Customer resolveCustomer(UUID customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado: " + customerId));
    }

    private Branch resolveBranch(UUID branchId) {
        return branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada: " + branchId));
    }

    private Order find(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada: " + id));
    }

    private int clampLimit(int limit) {
        if (limit <= 0) {
            return 20;
        }
        return Math.min(limit, 100);
    }
}
