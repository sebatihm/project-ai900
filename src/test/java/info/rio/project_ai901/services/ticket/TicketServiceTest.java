package info.rio.project_ai901.services.ticket;

import info.rio.project_ai901.api.dto.AssignTicketRequest;
import info.rio.project_ai901.api.dto.CreateTicketMessageRequest;
import info.rio.project_ai901.api.dto.CreateTicketRequest;
import info.rio.project_ai901.api.dto.PaginatedResponse;
import info.rio.project_ai901.api.dto.TicketDetailResponse;
import info.rio.project_ai901.api.dto.TicketMessageResponse;
import info.rio.project_ai901.api.dto.TicketSummaryResponse;
import info.rio.project_ai901.api.dto.UpdateTicketRatingRequest;
import info.rio.project_ai901.api.dto.UpdateTicketRequest;
import info.rio.project_ai901.exceptions.InvalidRequestException;
import info.rio.project_ai901.exceptions.ResourceInUseException;
import info.rio.project_ai901.exceptions.ResourceNotFoundException;
import info.rio.project_ai901.models.Customer;
import info.rio.project_ai901.models.Branch;
import info.rio.project_ai901.models.Role;
import info.rio.project_ai901.models.Ticket;
import info.rio.project_ai901.models.TicketMessage;
import info.rio.project_ai901.models.User;
import info.rio.project_ai901.models.enums.CustomerType;
import info.rio.project_ai901.models.enums.HandledBy;
import info.rio.project_ai901.models.enums.MessageAction;
import info.rio.project_ai901.models.enums.MessageAuthor;
import info.rio.project_ai901.models.enums.RoleName;
import info.rio.project_ai901.models.enums.TicketChannel;
import info.rio.project_ai901.models.enums.TicketSentiment;
import info.rio.project_ai901.models.enums.TicketStatus;
import info.rio.project_ai901.models.enums.TicketTopic;
import info.rio.project_ai901.repositories.CustomerRepository;
import info.rio.project_ai901.repositories.OrderRepository;
import info.rio.project_ai901.repositories.TicketMessageRepository;
import info.rio.project_ai901.repositories.TicketRepository;
import info.rio.project_ai901.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TicketServiceTest {

    private TicketRepository ticketRepository;
    private TicketMessageRepository ticketMessageRepository;
    private CustomerRepository customerRepository;
    private OrderRepository orderRepository;
    private UserRepository userRepository;
    private TicketService service;

    @BeforeEach
    void setUp() {
        ticketRepository = Mockito.mock(TicketRepository.class);
        ticketMessageRepository = Mockito.mock(TicketMessageRepository.class);
        customerRepository = Mockito.mock(CustomerRepository.class);
        orderRepository = Mockito.mock(OrderRepository.class);
        userRepository = Mockito.mock(UserRepository.class);
        service = new TicketService(ticketRepository, ticketMessageRepository, customerRepository, orderRepository,
                userRepository, new TicketMapper());
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> {
            Ticket ticket = invocation.getArgument(0);
            if (ticket.getId() == null) {
                ticket.setId(UUID.randomUUID());
            }
            return ticket;
        });
        when(ticketMessageRepository.save(any(TicketMessage.class))).thenAnswer(invocation -> {
            TicketMessage message = invocation.getArgument(0);
            if (message.getId() == null) {
                message.setId(UUID.randomUUID());
            }
            return message;
        });
    }

    @Test
    void creaTicketConEstadoOpenYHandledByHumano() {
        UUID customerId = UUID.randomUUID();
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer(customerId)));

        TicketDetailResponse result = service.create(createRequest(customerId, null, "Paquete retrasado",
                TicketChannel.EMAIL, TicketTopic.DELAY, TicketSentiment.NEGATIVE, null, null, null));

        assertThat(result.status()).isEqualTo(TicketStatus.OPEN);
        assertThat(result.handledBy()).isEqualTo(HandledBy.HUMAN);
        assertThat(result.requiresHumanReview()).isFalse();
        assertThat(result.rating()).isNull();
        assertThat(result.messageCount()).isZero();
        assertThat(result.customerName()).isEqualTo("Hotel Miramar");
        assertThat(result.createdAt()).isNotNull();
        verify(ticketRepository).save(any(Ticket.class));
    }

    @Test
    void creaTicketAgenteConClasificacionParaLaIa() {
        UUID customerId = UUID.randomUUID();
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer(customerId)));
        when(ticketMessageRepository.countByTicket_Id(any(UUID.class))).thenReturn(1L);

        TicketDetailResponse result = service.create(createRequest(customerId, null, "Factura con error",
                TicketChannel.PORTAL, TicketTopic.INVOICE, TicketSentiment.NEGATIVE, HandledBy.AGENT, true,
                new CreateTicketMessageRequest(MessageAuthor.AGENT, "El cliente reporta error en la factura",
                        MessageAction.CONSULT_ORDER, "politica-facturacion#12")));

        assertThat(result.handledBy()).isEqualTo(HandledBy.AGENT);
        assertThat(result.requiresHumanReview()).isTrue();
        assertThat(result.topic()).isEqualTo(TicketTopic.INVOICE);
        assertThat(result.messageCount()).isEqualTo(1);
        ArgumentCaptor<TicketMessage> captor = ArgumentCaptor.forClass(TicketMessage.class);
        verify(ticketMessageRepository).save(captor.capture());
        assertThat(captor.getValue().getAuthor()).isEqualTo(MessageAuthor.AGENT);
        assertThat(captor.getValue().getAction()).isEqualTo(MessageAction.CONSULT_ORDER);
        assertThat(captor.getValue().getSources()).isEqualTo("politica-facturacion#12");
        assertThat(captor.getValue().getSentAt()).isNotNull();
    }

    @Test
    void creaTicketConMensajeInicialDeCliente() {
        UUID customerId = UUID.randomUUID();
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer(customerId)));
        when(ticketMessageRepository.countByTicket_Id(any(UUID.class))).thenReturn(1L);

        TicketDetailResponse result = service.create(createRequest(customerId, null, "Pedido perdido",
                TicketChannel.VOICE, null, null, null, null,
                new CreateTicketMessageRequest(MessageAuthor.CUSTOMER, "Mi pedido aun no llega", null, null)));

        assertThat(result.messageCount()).isEqualTo(1);
        ArgumentCaptor<TicketMessage> captor = ArgumentCaptor.forClass(TicketMessage.class);
        verify(ticketMessageRepository).save(captor.capture());
        assertThat(captor.getValue().getContent()).isEqualTo("Mi pedido aun no llega");
        assertThat(captor.getValue().getAuthor()).isEqualTo(MessageAuthor.CUSTOMER);
    }

    @Test
    void rechazaClienteDesconocidoAlCrearTicket() {
        UUID customerId = UUID.randomUUID();
        when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(createRequest(customerId, null, "Asunto",
                TicketChannel.EMAIL, null, null, null, null, null)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Cliente no encontrado");
        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    @Test
    void rechazaOrdenDesconocidaAlCrearTicket() {
        UUID customerId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer(customerId)));
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(createRequest(customerId, orderId, "Asunto",
                TicketChannel.EMAIL, null, null, null, null, null)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Orden no encontrada");
        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    @Test
    void lanzaNotFoundParaOperacionesConIdInexistente() {
        UUID id = UUID.randomUUID();
        when(ticketRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.detail(id)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.delete(id)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.listMessages(id)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.addMessage(id, messageRequest(MessageAuthor.CUSTOMER, "Contenido")))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.updateStatus(id, TicketStatus.RESOLVED))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.updateRating(id, new UpdateTicketRatingRequest(5)))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(ticketRepository, never()).delete(any(Ticket.class));
    }

    @Test
    void actualizaElTicketSinTocarEstadoCalificacionNiCliente() {
        UUID id = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        Ticket ticket = ticket(id, customerId, TicketStatus.RESOLVED);
        ticket.setRating(5);
        when(ticketRepository.findById(id)).thenReturn(Optional.of(ticket));

        TicketDetailResponse result = service.update(id, new UpdateTicketRequest("Asunto actualizado",
                TicketChannel.VOICE, null, TicketTopic.BILLING, TicketSentiment.POSITIVE, true));

        assertThat(result.subject()).isEqualTo("Asunto actualizado");
        assertThat(result.status()).isEqualTo(TicketStatus.RESOLVED);
        assertThat(result.rating()).isEqualTo(5);
        assertThat(result.customerId()).isEqualTo(customerId);
        assertThat(result.requiresHumanReview()).isTrue();
        verify(ticketRepository).save(ticket);
    }

    @Test
    void aceptaTransicionesDeEstadoValidas() {
        assertTransitionAccepted(TicketStatus.OPEN, TicketStatus.IN_PROGRESS);
        assertTransitionAccepted(TicketStatus.OPEN, TicketStatus.RESOLVED);
        assertTransitionAccepted(TicketStatus.IN_PROGRESS, TicketStatus.RESOLVED);
    }

    @Test
    void rechazaTransicionesDeEstadoInvalidas() {
        assertTransitionRejected(TicketStatus.IN_PROGRESS, TicketStatus.OPEN);
        assertTransitionRejected(TicketStatus.RESOLVED, TicketStatus.OPEN);
        assertTransitionRejected(TicketStatus.RESOLVED, TicketStatus.IN_PROGRESS);
    }

    @Test
    void rechazaTransicionAlMismoEstado() {
        UUID id = UUID.randomUUID();
        when(ticketRepository.findById(id)).thenReturn(Optional.of(ticket(id, UUID.randomUUID(), TicketStatus.OPEN)));

        assertThatThrownBy(() -> service.updateStatus(id, TicketStatus.OPEN))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("ya se encuentra en el estado OPEN");
    }

    @Test
    void rechazaCalificacionCuandoElTicketNoEstaResuelto() {
        UUID id = UUID.randomUUID();
        when(ticketRepository.findById(id)).thenReturn(Optional.of(ticket(id, UUID.randomUUID(), TicketStatus.OPEN)));

        assertThatThrownBy(() -> service.updateRating(id, new UpdateTicketRatingRequest(5)))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Solo se puede calificar un ticket RESUELTO");
        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    @Test
    void registraCalificacionCuandoElTicketEstaResuelto() {
        UUID id = UUID.randomUUID();
        Ticket ticket = ticket(id, UUID.randomUUID(), TicketStatus.RESOLVED);
        when(ticketRepository.findById(id)).thenReturn(Optional.of(ticket));

        TicketDetailResponse result = service.updateRating(id, new UpdateTicketRatingRequest(4));

        assertThat(result.rating()).isEqualTo(4);
        assertThat(result.status()).isEqualTo(TicketStatus.RESOLVED);
        verify(ticketRepository).save(ticket);
    }

    @Test
    void eliminaTicketResueltoJuntoConSusMensajes() {
        UUID id = UUID.randomUUID();
        Ticket ticket = ticket(id, UUID.randomUUID(), TicketStatus.RESOLVED);
        when(ticketRepository.findById(id)).thenReturn(Optional.of(ticket));

        service.delete(id);

        verify(ticketMessageRepository).deleteByTicket_Id(id);
        verify(ticketRepository).delete(ticket);
    }

    @Test
    void rechazaEliminarTicketQueNoEstaResuelto() {
        UUID id = UUID.randomUUID();
        when(ticketRepository.findById(id)).thenReturn(Optional.of(ticket(id, UUID.randomUUID(), TicketStatus.OPEN)));

        assertThatThrownBy(() -> service.delete(id)).isInstanceOf(ResourceInUseException.class)
                .hasMessageContaining("Solo se puede eliminar un ticket RESUELTO");
        verify(ticketRepository, never()).delete(any(Ticket.class));
        verify(ticketMessageRepository, never()).deleteByTicket_Id(any(UUID.class));
    }

    @Test
    void agregaMensajeAUnTicketExistente() {
        UUID id = UUID.randomUUID();
        when(ticketRepository.findById(id)).thenReturn(Optional.of(ticket(id, UUID.randomUUID(), TicketStatus.OPEN)));

        TicketMessageResponse result = service.addMessage(id,
                messageRequest(MessageAuthor.CUSTOMER, "Sigo esperando mi paquete"));

        assertThat(result.ticketId()).isEqualTo(id);
        assertThat(result.author()).isEqualTo(MessageAuthor.CUSTOMER);
        assertThat(result.content()).isEqualTo("Sigo esperando mi paquete");
        assertThat(result.sentAt()).isNotNull();
        ArgumentCaptor<TicketMessage> captor = ArgumentCaptor.forClass(TicketMessage.class);
        verify(ticketMessageRepository).save(captor.capture());
        assertThat(captor.getValue().getTicket().getId()).isEqualTo(id);
    }

    @Test
    void listaTicketsPaginadosAcotandoElLimite() {
        UUID customerId = UUID.randomUUID();
        Pageable requested = PageRequest.of(0, 100, Sort.by("createdAt").descending());
        when(ticketRepository.search(eq(customerId), eq(TicketStatus.OPEN), eq(TicketTopic.DELAY),
                eq(HandledBy.AGENT), eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(ticket(UUID.randomUUID(), customerId, TicketStatus.OPEN)),
                        requested, 1));

        PaginatedResponse<TicketSummaryResponse> result = service.list(customerId, TicketStatus.OPEN,
                TicketTopic.DELAY, HandledBy.AGENT, null, 0L, 500);

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().getFirst().subject()).isEqualTo("Asunto del ticket");
        assertThat(result.total()).isEqualTo(1);
        assertThat(result.limit()).isEqualTo(100);
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(ticketRepository).search(eq(customerId), eq(TicketStatus.OPEN), eq(TicketTopic.DELAY),
                eq(HandledBy.AGENT), eq(null), captor.capture());
        assertThat(captor.getValue().getSort().getOrderFor("createdAt")).isNotNull();
        assertThat(captor.getValue().getSort().getOrderFor("createdAt").isDescending()).isTrue();
    }

    @Test
    void filtraTicketsPorUsuarioAsignado() {
        UUID customerId = UUID.randomUUID();
        UUID assignedToId = UUID.randomUUID();
        Pageable requested = PageRequest.of(0, 20, Sort.by("createdAt").descending());
        Ticket asignado = ticket(UUID.randomUUID(), customerId, TicketStatus.IN_PROGRESS);
        asignado.setAssignedTo(user(assignedToId, "Soporte Uno", RoleName.ADMIN_SUPPORT, true, null));
        when(ticketRepository.search(eq(null), eq(null), eq(null), eq(null), eq(assignedToId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(asignado), requested, 1));

        PaginatedResponse<TicketSummaryResponse> result = service.list(null, null, null, null, assignedToId, 0L, 20);

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().getFirst().assignedToId()).isEqualTo(assignedToId);
        assertThat(result.items().getFirst().assignedToName()).isEqualTo("Soporte Uno");
    }

    @Test
    void asignaTicketAUsuarioDeSoporteYLoPoneEnProgreso() {
        UUID id = UUID.randomUUID();
        UUID assignedToId = UUID.randomUUID();
        Ticket ticket = ticket(id, UUID.randomUUID(), TicketStatus.OPEN);
        ticket.setHandledBy(HandledBy.AGENT);
        when(ticketRepository.findById(id)).thenReturn(Optional.of(ticket));
        when(userRepository.findById(assignedToId))
                .thenReturn(Optional.of(user(assignedToId, "Soporte Uno", RoleName.ADMIN_SUPPORT, true, null)));

        TicketDetailResponse result = service.assign(id, new AssignTicketRequest(assignedToId));

        assertThat(result.assignedToId()).isEqualTo(assignedToId);
        assertThat(result.assignedToName()).isEqualTo("Soporte Uno");
        assertThat(result.status()).isEqualTo(TicketStatus.IN_PROGRESS);
        assertThat(result.handledBy()).isEqualTo(HandledBy.HUMAN);
        verify(ticketRepository).save(ticket);
    }

    @Test
    void desasignaTicketYLoRegresaAAbierto() {
        UUID id = UUID.randomUUID();
        UUID assignedToId = UUID.randomUUID();
        Ticket ticket = ticket(id, UUID.randomUUID(), TicketStatus.IN_PROGRESS);
        ticket.setAssignedTo(user(assignedToId, "Soporte Uno", RoleName.ADMIN_SUPPORT, true, null));
        when(ticketRepository.findById(id)).thenReturn(Optional.of(ticket));

        TicketDetailResponse result = service.assign(id, new AssignTicketRequest(null));

        assertThat(result.assignedToId()).isNull();
        assertThat(result.assignedToName()).isNull();
        assertThat(result.status()).isEqualTo(TicketStatus.OPEN);
        assertThat(result.handledBy()).isEqualTo(HandledBy.AGENT);
        verify(ticketRepository).save(ticket);
    }

    @Test
    void rechazaAsignarTicketAUsuarioDesconocido() {
        UUID id = UUID.randomUUID();
        UUID assignedToId = UUID.randomUUID();
        when(ticketRepository.findById(id)).thenReturn(Optional.of(ticket(id, UUID.randomUUID(), TicketStatus.OPEN)));
        when(userRepository.findById(assignedToId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.assign(id, new AssignTicketRequest(assignedToId)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Usuario no encontrado");
        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    @Test
    void rechazaAsignarTicketAUsuarioInactivo() {
        UUID id = UUID.randomUUID();
        UUID assignedToId = UUID.randomUUID();
        when(ticketRepository.findById(id)).thenReturn(Optional.of(ticket(id, UUID.randomUUID(), TicketStatus.OPEN)));
        when(userRepository.findById(assignedToId))
                .thenReturn(Optional.of(user(assignedToId, "Soporte Uno", RoleName.ADMIN_SUPPORT, false, null)));

        assertThatThrownBy(() -> service.assign(id, new AssignTicketRequest(assignedToId)))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("no esta activo");
        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    @Test
    void rechazaAsignarTicketAUsuarioConRolNoAsignable() {
        UUID id = UUID.randomUUID();
        UUID assignedToId = UUID.randomUUID();
        when(ticketRepository.findById(id)).thenReturn(Optional.of(ticket(id, UUID.randomUUID(), TicketStatus.OPEN)));
        when(userRepository.findById(assignedToId))
                .thenReturn(Optional.of(user(assignedToId, "Jefe Almacen", RoleName.WAREHOUSE_HEAD, true, null)));

        assertThatThrownBy(() -> service.assign(id, new AssignTicketRequest(assignedToId)))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("no puede tomar tickets");
        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    @Test
    void rechazaAsignarTicketAEncargadoDeSucursalDeOtraSucursal() {
        UUID id = UUID.randomUUID();
        UUID assignedToId = UUID.randomUUID();
        Ticket ticket = ticket(id, UUID.randomUUID(), TicketStatus.OPEN);
        ticket.getCustomer().setBranch(branch(UUID.randomUUID(), "SUC-01"));
        when(ticketRepository.findById(id)).thenReturn(Optional.of(ticket));
        when(userRepository.findById(assignedToId)).thenReturn(Optional.of(user(assignedToId, "Encargado Norte",
                RoleName.BRANCH_MANAGER, true, branch(UUID.randomUUID(), "SUC-02"))));

        assertThatThrownBy(() -> service.assign(id, new AssignTicketRequest(assignedToId)))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("sucursal");
        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    @Test
    void asignaTicketAEncargadoDeLaMismaSucursal() {
        UUID id = UUID.randomUUID();
        UUID assignedToId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        Ticket ticket = ticket(id, UUID.randomUUID(), TicketStatus.OPEN);
        ticket.getCustomer().setBranch(branch(branchId, "SUC-01"));
        when(ticketRepository.findById(id)).thenReturn(Optional.of(ticket));
        when(userRepository.findById(assignedToId)).thenReturn(Optional.of(user(assignedToId, "Encargada Centro",
                RoleName.BRANCH_MANAGER, true, branch(branchId, "SUC-01"))));

        TicketDetailResponse result = service.assign(id, new AssignTicketRequest(assignedToId));

        assertThat(result.assignedToId()).isEqualTo(assignedToId);
        assertThat(result.status()).isEqualTo(TicketStatus.IN_PROGRESS);
    }

    @Test
    void listaMensajesDelTicket() {
        UUID id = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        Ticket ticket = ticket(id, customerId, TicketStatus.OPEN);
        when(ticketRepository.findById(id)).thenReturn(Optional.of(ticket));
        TicketMessage primero = new TicketMessage();
        primero.setId(UUID.randomUUID());
        primero.setTicket(ticket);
        primero.setAuthor(MessageAuthor.CUSTOMER);
        primero.setContent("Queja inicial");
        primero.setSentAt(LocalDateTime.of(2026, 1, 1, 10, 0));
        TicketMessage segundo = new TicketMessage();
        segundo.setId(UUID.randomUUID());
        segundo.setTicket(ticket);
        segundo.setAuthor(MessageAuthor.EMPLOYEE);
        segundo.setContent("Respuesta");
        segundo.setSentAt(LocalDateTime.of(2026, 1, 1, 11, 0));
        when(ticketMessageRepository.findByTicket_IdOrderBySentAtAsc(id)).thenReturn(List.of(primero, segundo));

        List<TicketMessageResponse> result = service.listMessages(id);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).content()).isEqualTo("Queja inicial");
        assertThat(result.get(1).content()).isEqualTo("Respuesta");
        assertThat(result.get(1).ticketId()).isEqualTo(id);
    }

    private void assertTransitionAccepted(TicketStatus from, TicketStatus to) {
        UUID id = UUID.randomUUID();
        Ticket ticket = ticket(id, UUID.randomUUID(), from);
        when(ticketRepository.findById(id)).thenReturn(Optional.of(ticket));

        TicketDetailResponse result = service.updateStatus(id, to);

        assertThat(result.status()).isEqualTo(to);
    }

    private void assertTransitionRejected(TicketStatus from, TicketStatus to) {
        UUID id = UUID.randomUUID();
        when(ticketRepository.findById(id)).thenReturn(Optional.of(ticket(id, UUID.randomUUID(), from)));

        assertThatThrownBy(() -> service.updateStatus(id, to))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining(from.name())
                .hasMessageContaining(to.name());
    }

    private CreateTicketRequest createRequest(UUID customerId, UUID orderId, String subject, TicketChannel channel,
            TicketTopic topic, TicketSentiment sentiment, HandledBy handledBy, Boolean requiresHumanReview,
            CreateTicketMessageRequest initialMessage) {
        return new CreateTicketRequest(customerId, orderId, subject, channel, topic, sentiment, handledBy,
                requiresHumanReview, initialMessage);
    }

    private CreateTicketMessageRequest messageRequest(MessageAuthor author, String content) {
        return new CreateTicketMessageRequest(author, content, null, null);
    }

    private Customer customer(UUID id) {
        Customer customer = new Customer();
        customer.setId(id);
        customer.setName("Hotel Miramar");
        customer.setType(CustomerType.HOTEL);
        customer.setEmail("contacto@miramar.test");
        return customer;
    }

    private Branch branch(UUID id, String code) {
        Branch branch = new Branch();
        branch.setId(id);
        branch.setCode(code);
        branch.setName("Sucursal " + code);
        branch.setCity("CDMX");
        branch.setActive(true);
        return branch;
    }

    private User user(UUID id, String name, RoleName roleName, boolean active, Branch branch) {
        Role role = new Role();
        role.setId(UUID.randomUUID());
        role.setName(roleName);

        User user = new User();
        user.setId(id);
        user.setName(name);
        user.setEmail(name.toLowerCase().replaceAll("[^a-z0-9]+", ".") + "@ai901.local");
        user.setPassword("$2a$10$hash");
        user.setRole(role);
        user.setBranch(branch);
        user.setActive(active);
        return user;
    }

    private Ticket ticket(UUID id, UUID customerId, TicketStatus status) {
        Ticket ticket = new Ticket();
        ticket.setId(id);
        ticket.setCustomer(customer(customerId));
        ticket.setSubject("Asunto del ticket");
        ticket.setChannel(TicketChannel.EMAIL);
        ticket.setStatus(status);
        ticket.setTopic(TicketTopic.DELAY);
        ticket.setSentiment(TicketSentiment.NEGATIVE);
        ticket.setHandledBy(HandledBy.HUMAN);
        ticket.setRequiresHumanReview(false);
        ticket.setCreatedAt(LocalDateTime.of(2026, 1, 1, 10, 0));
        return ticket;
    }
}
