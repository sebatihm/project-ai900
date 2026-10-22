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
import info.rio.project_ai901.models.Branch;
import info.rio.project_ai901.models.Customer;
import info.rio.project_ai901.models.Order;
import info.rio.project_ai901.models.Ticket;
import info.rio.project_ai901.models.TicketMessage;
import info.rio.project_ai901.models.User;
import info.rio.project_ai901.models.enums.HandledBy;
import info.rio.project_ai901.models.enums.RoleName;
import info.rio.project_ai901.models.enums.TicketStatus;
import info.rio.project_ai901.models.enums.TicketTopic;
import info.rio.project_ai901.repositories.CustomerRepository;
import info.rio.project_ai901.repositories.OrderRepository;
import info.rio.project_ai901.repositories.TicketMessageRepository;
import info.rio.project_ai901.repositories.TicketRepository;
import info.rio.project_ai901.repositories.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class TicketService {

    /**
     * Roles autorizados para tomar tickets. WAREHOUSE_HEAD queda excluido: su
     * funcion es la operacion de almacen, no la atencion de tickets.
     */
    private static final Set<RoleName> ASSIGNABLE_ROLES = EnumSet.of(RoleName.ADMIN_SUPPORT,
            RoleName.BRANCH_MANAGER, RoleName.ADMIN, RoleName.BOSS);

    private static final Map<TicketStatus, Set<TicketStatus>> TRANSITIONS = Map.of(
            TicketStatus.OPEN, EnumSet.of(TicketStatus.IN_PROGRESS, TicketStatus.RESOLVED),
            TicketStatus.IN_PROGRESS, EnumSet.of(TicketStatus.RESOLVED),
            TicketStatus.RESOLVED, EnumSet.noneOf(TicketStatus.class));

    private final TicketRepository ticketRepository;
    private final TicketMessageRepository ticketMessageRepository;
    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final TicketMapper mapper;

    public TicketService(TicketRepository ticketRepository, TicketMessageRepository ticketMessageRepository,
            CustomerRepository customerRepository, OrderRepository orderRepository,
            UserRepository userRepository, TicketMapper mapper) {
        this.ticketRepository = ticketRepository;
        this.ticketMessageRepository = ticketMessageRepository;
        this.customerRepository = customerRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<TicketSummaryResponse> list(UUID customerId, TicketStatus status, TicketTopic topic,
            HandledBy handledBy, UUID assignedToId, long offset, int limit) {
        int size = clampLimit(limit);
        long safeOffset = Math.max(offset, 0);
        PageRequest pageable = PageRequest.of((int) (safeOffset / size), size,
                Sort.by("createdAt").descending());
        Page<Ticket> pageResult = ticketRepository.search(customerId, status, topic, handledBy, assignedToId,
                pageable);
        List<TicketSummaryResponse> items = pageResult.getContent().stream()
                .map(mapper::toSummary)
                .toList();
        return new PaginatedResponse<>(items, pageResult.getTotalElements(), safeOffset, size);
    }

    @Transactional(readOnly = true)
    public TicketDetailResponse detail(UUID id) {
        Ticket ticket = find(id);
        return mapper.toDetail(ticket, ticketMessageRepository.countByTicket_Id(id));
    }

    @Transactional
    public TicketDetailResponse create(CreateTicketRequest request) {
        Customer customer = resolveCustomer(request.customerId());
        Order order = resolveOrder(request.orderId());

        Ticket ticket = new Ticket();
        ticket.setCustomer(customer);
        ticket.setOrder(order);
        ticket.setSubject(request.subject().trim());
        ticket.setChannel(request.channel());
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setTopic(request.topic());
        ticket.setSentiment(request.sentiment());
        ticket.setHandledBy(request.handledBy() != null ? request.handledBy() : HandledBy.HUMAN);
        ticket.setRequiresHumanReview(Boolean.TRUE.equals(request.requiresHumanReview()));
        ticket.setCreatedAt(LocalDateTime.now());
        ticketRepository.save(ticket);

        if (request.initialMessage() != null) {
            saveMessage(ticket, request.initialMessage());
        }
        return mapper.toDetail(ticket, ticketMessageRepository.countByTicket_Id(ticket.getId()));
    }

    @Transactional
    public TicketDetailResponse update(UUID id, UpdateTicketRequest request) {
        Ticket ticket = find(id);
        ticket.setSubject(request.subject().trim());
        ticket.setChannel(request.channel());
        ticket.setOrder(resolveOrder(request.orderId()));
        ticket.setTopic(request.topic());
        ticket.setSentiment(request.sentiment());
        ticket.setRequiresHumanReview(request.requiresHumanReview());
        ticketRepository.save(ticket);
        return mapper.toDetail(ticket, ticketMessageRepository.countByTicket_Id(id));
    }

    @Transactional
    public TicketDetailResponse updateStatus(UUID id, TicketStatus newStatus) {
        Ticket ticket = find(id);
        TicketStatus current = ticket.getStatus();
        if (current == newStatus) {
            throw new InvalidRequestException("El ticket ya se encuentra en el estado " + newStatus);
        }
        Set<TicketStatus> allowed = TRANSITIONS.getOrDefault(current, EnumSet.noneOf(TicketStatus.class));
        if (!allowed.contains(newStatus)) {
            throw new InvalidRequestException(
                    "Transicion de estado no permitida: " + current + " -> " + newStatus);
        }
        ticket.setStatus(newStatus);
        ticketRepository.save(ticket);
        return mapper.toDetail(ticket, ticketMessageRepository.countByTicket_Id(id));
    }

    /**
     * Asigna el ticket a un usuario del equipo de soporte o lo desasigna
     * (enviando {@code assignedToId} nulo). Al asignar, el ticket pasa a
     * {@code IN_PROGRESS} y a {@code HANDLED_BY.HUMAN}; al desasignar vuelve a
     * {@code OPEN} y {@code HANDLED_BY.AGENT}.
     */
    @Transactional
    public TicketDetailResponse assign(UUID id, AssignTicketRequest request) {
        Ticket ticket = find(id);
        User assignee = resolveAssignee(request.assignedToId(), ticket);
        ticket.setAssignedTo(assignee);
        if (assignee != null) {
            ticket.setHandledBy(HandledBy.HUMAN);
            if (ticket.getStatus() == TicketStatus.OPEN) {
                ticket.setStatus(TicketStatus.IN_PROGRESS);
            }
        } else if (ticket.getStatus() == TicketStatus.IN_PROGRESS) {
            ticket.setStatus(TicketStatus.OPEN);
            ticket.setHandledBy(HandledBy.AGENT);
        }
        ticketRepository.save(ticket);
        return mapper.toDetail(ticket, ticketMessageRepository.countByTicket_Id(id));
    }

    @Transactional
    public TicketDetailResponse updateRating(UUID id, UpdateTicketRatingRequest request) {
        Ticket ticket = find(id);
        if (ticket.getStatus() != TicketStatus.RESOLVED) {
            throw new InvalidRequestException(
                    "Solo se puede calificar un ticket RESUELTO (estado actual: " + ticket.getStatus() + ")");
        }
        ticket.setRating(request.rating());
        ticketRepository.save(ticket);
        return mapper.toDetail(ticket, ticketMessageRepository.countByTicket_Id(id));
    }

    @Transactional
    public void delete(UUID id) {
        Ticket ticket = find(id);
        if (ticket.getStatus() != TicketStatus.RESOLVED) {
            throw new ResourceInUseException(
                    "Solo se puede eliminar un ticket RESUELTO (estado actual: " + ticket.getStatus() + ")");
        }
        ticketMessageRepository.deleteByTicket_Id(id);
        ticketRepository.delete(ticket);
    }

    @Transactional(readOnly = true)
    public List<TicketMessageResponse> listMessages(UUID id) {
        find(id);
        return ticketMessageRepository.findByTicket_IdOrderBySentAtAsc(id).stream()
                .map(mapper::toMessage)
                .toList();
    }

    @Transactional
    public TicketMessageResponse addMessage(UUID id, CreateTicketMessageRequest request) {
        Ticket ticket = find(id);
        TicketMessage message = saveMessage(ticket, request);
        return mapper.toMessage(message);
    }

    private TicketMessage saveMessage(Ticket ticket, CreateTicketMessageRequest request) {
        TicketMessage message = new TicketMessage();
        message.setTicket(ticket);
        message.setAuthor(request.author());
        message.setContent(request.content().trim());
        message.setAction(request.action());
        message.setSources(request.sources());
        message.setSentAt(LocalDateTime.now());
        return ticketMessageRepository.save(message);
    }

    private Customer resolveCustomer(UUID customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado: " + customerId));
    }

    private Order resolveOrder(UUID orderId) {
        if (orderId == null) {
            return null;
        }
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada: " + orderId));
    }

    private User resolveAssignee(UUID assignedToId, Ticket ticket) {
        if (assignedToId == null) {
            return null;
        }
        User user = userRepository.findById(assignedToId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + assignedToId));
        if (!user.isActive()) {
            throw new InvalidRequestException("El usuario no esta activo: " + assignedToId);
        }
        RoleName role = user.getRole() != null ? user.getRole().getName() : null;
        if (role == null || !ASSIGNABLE_ROLES.contains(role)) {
            throw new InvalidRequestException("El rol " + role + " no puede tomar tickets");
        }
        if (role == RoleName.BRANCH_MANAGER) {
            validateBranchScope(user, ticket);
        }
        return user;
    }

    /**
     * Un BRANCH_MANAGER solo puede tomar tickets de clientes de su propia
     * sucursal.
     */
    private void validateBranchScope(User user, Ticket ticket) {
        Branch userBranch = user.getBranch();
        Branch customerBranch = ticket.getCustomer() != null ? ticket.getCustomer().getBranch() : null;
        if (userBranch == null || customerBranch == null || !userBranch.getId().equals(customerBranch.getId())) {
            throw new InvalidRequestException(
                    "El usuario solo puede tomar tickets de clientes de su sucursal");
        }
    }

    private Ticket find(UUID id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket no encontrado: " + id));
    }

    private int clampLimit(int limit) {
        if (limit <= 0) {
            return 20;
        }
        return Math.min(limit, 100);
    }
}
