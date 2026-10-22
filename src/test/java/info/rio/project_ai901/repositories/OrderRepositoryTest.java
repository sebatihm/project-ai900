package info.rio.project_ai901.repositories;

import info.rio.project_ai901.models.Branch;
import info.rio.project_ai901.models.Customer;
import info.rio.project_ai901.models.Order;
import info.rio.project_ai901.models.enums.CustomerType;
import info.rio.project_ai901.models.enums.OrderStatus;
import info.rio.project_ai901.support.AbstractRepositoryTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Test
    void verificaExistenciaPorCodigoIgnorandoMayusculas() {
        Branch branch = branch("SUC-01");
        Customer customer = customer("Hotel Miramar", null);
        orderRepository.saveAndFlush(order("ord-0001", customer, branch, OrderStatus.RECEIVED,
                LocalDateTime.of(2026, 1, 1, 10, 0)));

        assertThat(orderRepository.existsByCodeIgnoreCase("ORD-0001")).isTrue();
        assertThat(orderRepository.existsByCodeIgnoreCase("ORD-9999")).isFalse();
    }

    @Test
    void cuentaOrdenesPorCliente() {
        Branch branch = branch("SUC-01");
        Customer cliente1 = customer("Cliente Uno", null);
        Customer cliente2 = customer("Cliente Dos", null);
        orderRepository.saveAndFlush(order("ORD-0001", cliente1, branch, OrderStatus.RECEIVED,
                LocalDateTime.of(2026, 1, 1, 10, 0)));
        orderRepository.saveAndFlush(order("ORD-0002", cliente1, branch, OrderStatus.DELIVERED,
                LocalDateTime.of(2026, 1, 2, 10, 0)));
        orderRepository.saveAndFlush(order("ORD-0003", cliente2, branch, OrderStatus.RECEIVED,
                LocalDateTime.of(2026, 1, 3, 10, 0)));
        orderRepository.flush();

        assertThat(orderRepository.countByCustomer_Id(cliente1.getId())).isEqualTo(2);
        assertThat(orderRepository.countByCustomer_Id(cliente2.getId())).isEqualTo(1);
        assertThat(orderRepository.countByCustomer_Id(UUID.randomUUID())).isZero();
    }

    @Test
    void cuentaOrdenesPorRangoDeFecha() {
        Branch branch = branch("SUC-01");
        Customer customer = customer("Hotel Miramar", null);
        orderRepository.saveAndFlush(order("ORD-0001", customer, branch, OrderStatus.RECEIVED,
                LocalDateTime.of(2026, 1, 1, 10, 0)));
        orderRepository.saveAndFlush(order("ORD-0002", customer, branch, OrderStatus.RECEIVED,
                LocalDateTime.of(2026, 1, 15, 12, 0)));
        orderRepository.saveAndFlush(order("ORD-0003", customer, branch, OrderStatus.RECEIVED,
                LocalDateTime.of(2026, 2, 5, 9, 0)));
        orderRepository.flush();

        LocalDateTime inicio = LocalDate.of(2026, 1, 1).atStartOfDay();
        LocalDateTime fin = LocalDate.of(2026, 1, 31).atTime(23, 59, 59);

        assertThat(orderRepository.countByOrderDateBetween(inicio, fin)).isEqualTo(2);
        assertThat(orderRepository.countByOrderDateBetween(inicio, fin.plusMonths(2))).isEqualTo(3);
    }

    @Test
    void buscaOrdenesSinFiltros() {
        Branch branch = branch("SUC-01");
        Customer customer = customer("Hotel Miramar", null);
        orderRepository.saveAndFlush(order("ORD-0001", customer, branch, OrderStatus.RECEIVED,
                LocalDateTime.of(2026, 1, 1, 10, 0)));
        orderRepository.flush();

        Page<Order> result = orderRepository.search(null, null, null, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void buscaOrdenesPorClienteSucursalYEstado() {
        Branch branch1 = branch("SUC-01");
        Branch branch2 = branch("SUC-02");
        Customer cliente1 = customer("Cliente Uno", null);
        Customer cliente2 = customer("Cliente Dos", null);
        orderRepository.saveAndFlush(order("ORD-0001", cliente1, branch1, OrderStatus.RECEIVED,
                LocalDateTime.of(2026, 1, 1, 10, 0)));
        orderRepository.saveAndFlush(order("ORD-0002", cliente1, branch1, OrderStatus.CANCELED,
                LocalDateTime.of(2026, 1, 2, 10, 0)));
        orderRepository.saveAndFlush(order("ORD-0003", cliente2, branch2, OrderStatus.RECEIVED,
                LocalDateTime.of(2026, 1, 3, 10, 0)));
        orderRepository.flush();

        assertThat(orderRepository.search(cliente1.getId(), null, null, PageRequest.of(0, 10)))
                .hasSize(2);
        assertThat(orderRepository.search(null, branch1.getId(), null, PageRequest.of(0, 10)))
                .hasSize(2);
        assertThat(orderRepository.search(null, null, OrderStatus.RECEIVED, PageRequest.of(0, 10)))
                .hasSize(2);
        assertThat(orderRepository.search(cliente1.getId(), branch1.getId(), OrderStatus.CANCELED,
                PageRequest.of(0, 10))).hasSize(1);
        assertThat(orderRepository.search(cliente2.getId(), branch1.getId(), OrderStatus.RECEIVED,
                PageRequest.of(0, 10))).hasSize(0);
        assertThat(orderRepository.search(null, null, OrderStatus.DELIVERED, PageRequest.of(0, 10)))
                .hasSize(0);
    }

    @Test
    void rechazaCodigoDuplicado() {
        Branch branch = branch("SUC-01");
        Customer customer = customer("Hotel Miramar", null);
        orderRepository.saveAndFlush(order("ORD-0001", customer, branch, OrderStatus.RECEIVED,
                LocalDateTime.of(2026, 1, 1, 10, 0)));

        assertThatThrownBy(() -> orderRepository.saveAndFlush(order("ORD-0001", customer, branch,
                OrderStatus.RECEIVED, LocalDateTime.of(2026, 1, 2, 10, 0))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private Branch branch(String code) {
        Branch branch = new Branch();
        branch.setCode(code);
        branch.setName("Sucursal " + code);
        branch.setCity("CDMX");
        branch.setActive(true);
        return branchRepository.saveAndFlush(branch);
    }

    private Customer customer(String name, Branch branch) {
        Customer customer = new Customer();
        customer.setName(name);
        customer.setType(CustomerType.HOTEL);
        customer.setEmail(name.toLowerCase().replace(' ', '.') + "@test.local");
        customer.setBranch(branch);
        return customerRepository.saveAndFlush(customer);
    }

    private Order order(String code, Customer customer, Branch branch, OrderStatus status,
            LocalDateTime orderDate) {
        Order order = new Order();
        order.setCode(code);
        order.setCustomer(customer);
        order.setBranch(branch);
        order.setStatus(status);
        order.setOrderDate(orderDate);
        order.setEstimatedDeliveryDate(LocalDate.of(2026, 2, 1));
        order.setTotal(new BigDecimal("100.00"));
        return order;
    }
}
