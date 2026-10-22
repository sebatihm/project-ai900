package info.rio.project_ai901.repositories;

import info.rio.project_ai901.models.Branch;
import info.rio.project_ai901.models.Customer;
import info.rio.project_ai901.models.Order;
import info.rio.project_ai901.models.Role;
import info.rio.project_ai901.models.Ticket;
import info.rio.project_ai901.models.User;
import info.rio.project_ai901.models.enums.CustomerType;
import info.rio.project_ai901.models.enums.HandledBy;
import info.rio.project_ai901.models.enums.OrderStatus;
import info.rio.project_ai901.models.enums.RoleName;
import info.rio.project_ai901.models.enums.TicketChannel;
import info.rio.project_ai901.models.enums.TicketStatus;
import info.rio.project_ai901.models.enums.TicketTopic;
import info.rio.project_ai901.support.AbstractRepositoryTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TicketRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Test
    void buscaTicketsSinFiltros() {
        Customer customer = customer("Cliente Sin Filtros");
        ticketRepository.saveAndFlush(ticket(customer, null, TicketStatus.OPEN, TicketTopic.DELAY,
                HandledBy.HUMAN, LocalDateTime.of(2026, 1, 1, 10, 0)));

        Page<Ticket> result = ticketRepository.search(null, null, null, null, null, pageable());

        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void buscaTicketsPorClienteEstadoTopicoYHandledBy() {
        Customer cliente1 = customer("Cliente Uno");
        Customer cliente2 = customer("Cliente Dos");
        ticketRepository.saveAndFlush(ticket(cliente1, null, TicketStatus.OPEN, TicketTopic.DELAY,
                HandledBy.HUMAN, LocalDateTime.of(2026, 1, 1, 10, 0)));
        ticketRepository.saveAndFlush(ticket(cliente1, null, TicketStatus.IN_PROGRESS, TicketTopic.BILLING,
                HandledBy.AGENT, LocalDateTime.of(2026, 1, 2, 10, 0)));
        ticketRepository.saveAndFlush(ticket(cliente2, null, TicketStatus.OPEN, TicketTopic.DELAY,
                HandledBy.AGENT, LocalDateTime.of(2026, 1, 3, 10, 0)));

        assertThat(ticketRepository.search(cliente1.getId(), null, null, null, null, pageable())).hasSize(2);
        assertThat(ticketRepository.search(null, TicketStatus.OPEN, null, null, null, pageable())).hasSize(2);
        assertThat(ticketRepository.search(null, null, TicketTopic.DELAY, null, null, pageable())).hasSize(2);
        assertThat(ticketRepository.search(null, null, null, HandledBy.AGENT, null, pageable())).hasSize(2);
        assertThat(ticketRepository.search(cliente1.getId(), TicketStatus.OPEN, TicketTopic.DELAY,
                HandledBy.HUMAN, null, pageable())).hasSize(1);
        assertThat(ticketRepository.search(cliente2.getId(), TicketStatus.OPEN, TicketTopic.BILLING,
                HandledBy.HUMAN, null, pageable())).hasSize(0);
    }

    @Test
    void buscaYCuentaTicketsPorUsuarioAsignado() {
        Customer customer = customer("Cliente Asignado");
        User soporte = user("soporte@ai901.local", RoleName.ADMIN_SUPPORT);
        User otro = user("otro@ai901.local", RoleName.ADMIN);

        Ticket asignado = ticket(customer, null, TicketStatus.IN_PROGRESS, TicketTopic.DELAY, HandledBy.HUMAN,
                LocalDateTime.of(2026, 1, 1, 10, 0));
        asignado.setAssignedTo(soporte);
        ticketRepository.saveAndFlush(asignado);

        Ticket asignadoResuelto = ticket(customer, null, TicketStatus.RESOLVED, TicketTopic.BILLING,
                HandledBy.HUMAN, LocalDateTime.of(2026, 1, 2, 10, 0));
        asignadoResuelto.setAssignedTo(soporte);
        ticketRepository.saveAndFlush(asignadoResuelto);

        Ticket asignadoOtro = ticket(customer, null, TicketStatus.IN_PROGRESS, TicketTopic.DELAY,
                HandledBy.HUMAN, LocalDateTime.of(2026, 1, 3, 10, 0));
        asignadoOtro.setAssignedTo(otro);
        ticketRepository.saveAndFlush(asignadoOtro);

        assertThat(ticketRepository.search(null, null, null, null, soporte.getId(), pageable())).hasSize(2);
        assertThat(ticketRepository.search(null, null, null, null, otro.getId(), pageable())).hasSize(1);
        assertThat(ticketRepository.search(null, null, null, null, UUID.randomUUID(), pageable())).hasSize(0);

        assertThat(ticketRepository.countByAssignedTo_IdAndStatusIn(soporte.getId(),
                List.of(TicketStatus.OPEN, TicketStatus.IN_PROGRESS))).isEqualTo(1);
        assertThat(ticketRepository.countByAssignedTo_IdAndStatusIn(soporte.getId(),
                List.of(TicketStatus.RESOLVED))).isEqualTo(1);
        assertThat(ticketRepository.countByAssignedTo_IdAndStatusIn(UUID.randomUUID(),
                List.of(TicketStatus.OPEN))).isZero();
    }

    @Test
    void ordenaTicketsPorFechaDeCreacionDescendente() {
        Customer customer = customer("Cliente Orden");
        ticketRepository.saveAndFlush(ticket(customer, null, TicketStatus.OPEN, null, HandledBy.HUMAN,
                LocalDateTime.of(2026, 1, 1, 10, 0)));
        ticketRepository.saveAndFlush(ticket(customer, null, TicketStatus.OPEN, null, HandledBy.HUMAN,
                LocalDateTime.of(2026, 1, 3, 10, 0)));
        ticketRepository.saveAndFlush(ticket(customer, null, TicketStatus.OPEN, null, HandledBy.HUMAN,
                LocalDateTime.of(2026, 1, 2, 10, 0)));

        Page<Ticket> result = ticketRepository.search(null, null, null, null, null, pageable());

        assertThat(result.getContent())
                .extracting(Ticket::getCreatedAt)
                .containsExactly(LocalDateTime.of(2026, 1, 3, 10, 0),
                        LocalDateTime.of(2026, 1, 2, 10, 0),
                        LocalDateTime.of(2026, 1, 1, 10, 0));
    }

    @Test
    void cuentaTicketsPorCliente() {
        Customer cliente1 = customer("Cliente Contable Uno");
        Customer cliente2 = customer("Cliente Contable Dos");
        ticketRepository.saveAndFlush(ticket(cliente1, null, TicketStatus.OPEN, null, HandledBy.HUMAN,
                LocalDateTime.of(2026, 1, 1, 10, 0)));
        ticketRepository.saveAndFlush(ticket(cliente1, null, TicketStatus.RESOLVED, null, HandledBy.HUMAN,
                LocalDateTime.of(2026, 1, 2, 10, 0)));
        ticketRepository.saveAndFlush(ticket(cliente2, null, TicketStatus.OPEN, null, HandledBy.HUMAN,
                LocalDateTime.of(2026, 1, 3, 10, 0)));

        assertThat(ticketRepository.countByCustomer_Id(cliente1.getId())).isEqualTo(2);
        assertThat(ticketRepository.countByCustomer_Id(cliente2.getId())).isEqualTo(1);
        assertThat(ticketRepository.countByCustomer_Id(UUID.randomUUID())).isZero();
    }

    @Test
    void cuentaTicketsPorOrden() {
        Branch branch = branch();
        Customer customer = customer("Cliente Por Orden");
        Order orden1 = order("ORD-0001", customer, branch);
        Order orden2 = order("ORD-0002", customer, branch);
        ticketRepository.saveAndFlush(ticket(customer, orden1, TicketStatus.OPEN, null, HandledBy.HUMAN,
                LocalDateTime.of(2026, 1, 1, 10, 0)));
        ticketRepository.saveAndFlush(ticket(customer, orden1, TicketStatus.RESOLVED, null, HandledBy.HUMAN,
                LocalDateTime.of(2026, 1, 2, 10, 0)));
        ticketRepository.saveAndFlush(ticket(customer, null, TicketStatus.OPEN, null, HandledBy.HUMAN,
                LocalDateTime.of(2026, 1, 3, 10, 0)));

        assertThat(ticketRepository.countByOrder_Id(orden1.getId())).isEqualTo(2);
        assertThat(ticketRepository.countByOrder_Id(orden2.getId())).isZero();
        assertThat(ticketRepository.countByOrder_Id(UUID.randomUUID())).isZero();
    }

    private PageRequest pageable() {
        return PageRequest.of(0, 10, Sort.by("createdAt").descending());
    }

    private Branch branch() {
        Branch branch = new Branch();
        branch.setCode("SUC-TCK");
        branch.setName("Sucursal Tickets");
        branch.setCity("CDMX");
        branch.setActive(true);
        return branchRepository.saveAndFlush(branch);
    }

    private Customer customer(String name) {
        Customer customer = new Customer();
        customer.setName(name);
        customer.setType(CustomerType.HOTEL);
        customer.setEmail(name.toLowerCase().replace(' ', '.') + "@test.local");
        customer.setBranch(null);
        return customerRepository.saveAndFlush(customer);
    }

    private Order order(String code, Customer customer, Branch branch) {
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

    private Ticket ticket(Customer customer, Order order, TicketStatus status, TicketTopic topic, HandledBy handledBy,
            LocalDateTime createdAt) {
        Ticket ticket = new Ticket();
        ticket.setCustomer(customer);
        ticket.setOrder(order);
        ticket.setSubject("Asunto del ticket");
        ticket.setChannel(TicketChannel.EMAIL);
        ticket.setStatus(status);
        ticket.setTopic(topic);
        ticket.setHandledBy(handledBy);
        ticket.setRequiresHumanReview(false);
        ticket.setCreatedAt(createdAt);
        return ticket;
    }

    private User user(String email, RoleName roleName) {
        Role role = roleRepository.findByName(roleName).orElseGet(() -> {
            Role nuevo = new Role();
            nuevo.setName(roleName);
            return roleRepository.saveAndFlush(nuevo);
        });
        User user = new User();
        user.setName("Usuario " + roleName);
        user.setEmail(email);
        user.setPassword("hash-de-prueba");
        user.setRole(role);
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.saveAndFlush(user);
    }
}
