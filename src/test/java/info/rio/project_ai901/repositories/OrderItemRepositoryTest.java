package info.rio.project_ai901.repositories;

import info.rio.project_ai901.models.Branch;
import info.rio.project_ai901.models.Category;
import info.rio.project_ai901.models.Customer;
import info.rio.project_ai901.models.Order;
import info.rio.project_ai901.models.OrderItem;
import info.rio.project_ai901.models.Product;
import info.rio.project_ai901.models.enums.CustomerType;
import info.rio.project_ai901.models.enums.OrderStatus;
import info.rio.project_ai901.support.AbstractRepositoryTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OrderItemRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Test
    void listaItemsDeUnaOrden() {
        Order orden = orden("ORD-0001");
        Product producto1 = producto("SKU-001");
        Product producto2 = producto("SKU-002");
        orderItemRepository.saveAndFlush(item(orden, producto1, 2));
        orderItemRepository.saveAndFlush(item(orden, producto2, 3));
        orderItemRepository.flush();

        List<OrderItem> result = orderItemRepository.findByOrder_Id(orden.getId());

        assertThat(result).hasSize(2);
        assertThat(result).extracting(OrderItem::getQuantity).containsExactlyInAnyOrder(2, 3);
    }

    @Test
    void noDevuelveItemsDeOtraOrden() {
        Order orden1 = orden("ORD-0001");
        Order orden2 = orden("ORD-0002");
        Product producto = producto("SKU-001");
        orderItemRepository.saveAndFlush(item(orden1, producto, 2));
        orderItemRepository.saveAndFlush(item(orden2, producto, 5));
        orderItemRepository.flush();

        assertThat(orderItemRepository.findByOrder_Id(orden1.getId())).hasSize(1);
        assertThat(orderItemRepository.countByOrder_Id(orden1.getId())).isEqualTo(1);
        assertThat(orderItemRepository.countByOrder_Id(orden2.getId())).isEqualTo(1);
        assertThat(orderItemRepository.countByOrder_Id(UUID.randomUUID())).isZero();
    }

    @Test
    void cuentaItemsPorProducto() {
        Order orden = orden("ORD-0001");
        Product producto = producto("SKU-001");
        Product otro = producto("SKU-002");
        orderItemRepository.saveAndFlush(item(orden, producto, 2));
        orderItemRepository.saveAndFlush(item(orden, otro, 1));
        orderItemRepository.flush();

        assertThat(orderItemRepository.countByProduct_Id(producto.getId())).isEqualTo(1);
        assertThat(orderItemRepository.countByProduct_Id(otro.getId())).isEqualTo(1);
        assertThat(orderItemRepository.countByProduct_Id(UUID.randomUUID())).isZero();
    }

    @Test
    void eliminaItemsDeUnaOrdenSinTocarLasDemas() {
        Order orden1 = orden("ORD-0001");
        Order orden2 = orden("ORD-0002");
        Product producto = producto("SKU-001");
        orderItemRepository.saveAndFlush(item(orden1, producto, 2));
        orderItemRepository.saveAndFlush(item(orden1, producto, 3));
        orderItemRepository.saveAndFlush(item(orden2, producto, 4));
        orderItemRepository.flush();

        orderItemRepository.deleteByOrder_Id(orden1.getId());
        orderItemRepository.flush();

        assertThat(orderItemRepository.findByOrder_Id(orden1.getId())).isEmpty();
        assertThat(orderItemRepository.countByOrder_Id(orden1.getId())).isZero();
        assertThat(orderItemRepository.countByOrder_Id(orden2.getId())).isEqualTo(1);
    }

    private Order orden(String code) {
        Branch branch = new Branch();
        branch.setCode("SUC-" + code.substring(code.length() - 1));
        branch.setName("Sucursal");
        branch.setCity("CDMX");
        branchRepository.saveAndFlush(branch);

        Customer customer = new Customer();
        customer.setName("Cliente " + code);
        customer.setType(CustomerType.HOTEL);
        customer.setEmail(code.toLowerCase() + "@test.local");
        customerRepository.saveAndFlush(customer);

        Order order = new Order();
        order.setCode(code);
        order.setCustomer(customer);
        order.setBranch(branch);
        order.setStatus(OrderStatus.RECEIVED);
        order.setOrderDate(LocalDateTime.of(2026, 1, 1, 10, 0));
        order.setEstimatedDeliveryDate(LocalDate.of(2026, 2, 1));
        order.setTotal(new BigDecimal("100.00"));
        return orderRepository.saveAndFlush(order);
    }

    private Product producto(String sku) {
        Category category = categoryRepository.findByNameIgnoreCase("Generica")
                .orElseGet(() -> {
                    Category nueva = new Category();
                    nueva.setName("Generica");
                    return categoryRepository.saveAndFlush(nueva);
                });

        Product product = new Product();
        product.setSku(sku);
        product.setName("Producto " + sku);
        product.setUnit("kg");
        product.setPrice(new BigDecimal("10.00"));
        product.setCategory(category);
        return productRepository.saveAndFlush(product);
    }

    private OrderItem item(Order order, Product product, int quantity) {
        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setProduct(product);
        item.setQuantity(quantity);
        item.setUnitPrice(product.getPrice());
        return item;
    }
}
