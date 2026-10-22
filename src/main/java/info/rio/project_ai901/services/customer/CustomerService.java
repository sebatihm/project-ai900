package info.rio.project_ai901.services.customer;

import info.rio.project_ai901.api.dto.CreateCustomerRequest;
import info.rio.project_ai901.api.dto.CustomerDetailResponse;
import info.rio.project_ai901.api.dto.CustomerSummaryResponse;
import info.rio.project_ai901.api.dto.PaginatedResponse;
import info.rio.project_ai901.api.dto.UpdateCustomerRequest;
import info.rio.project_ai901.exceptions.DuplicateResourceException;
import info.rio.project_ai901.exceptions.ResourceInUseException;
import info.rio.project_ai901.exceptions.ResourceNotFoundException;
import info.rio.project_ai901.models.Branch;
import info.rio.project_ai901.models.Customer;
import info.rio.project_ai901.models.enums.CustomerType;
import info.rio.project_ai901.repositories.BranchRepository;
import info.rio.project_ai901.repositories.CustomerRepository;
import info.rio.project_ai901.repositories.OrderRepository;
import info.rio.project_ai901.repositories.TicketRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final BranchRepository branchRepository;
    private final OrderRepository orderRepository;
    private final TicketRepository ticketRepository;
    private final CustomerMapper mapper;

    public CustomerService(CustomerRepository customerRepository, BranchRepository branchRepository,
            OrderRepository orderRepository, TicketRepository ticketRepository, CustomerMapper mapper) {
        this.customerRepository = customerRepository;
        this.branchRepository = branchRepository;
        this.orderRepository = orderRepository;
        this.ticketRepository = ticketRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<CustomerSummaryResponse> list(CustomerType type, UUID branchId, long offset, int limit) {
        int size = clampLimit(limit);
        long safeOffset = Math.max(offset, 0);
        PageRequest pageable = PageRequest.of((int) (safeOffset / size), size, Sort.by("name").ascending());
        Page<Customer> pageResult = fetch(type, branchId, pageable);
        List<CustomerSummaryResponse> items = pageResult.getContent().stream()
                .map(mapper::toSummary)
                .toList();
        return new PaginatedResponse<>(items, pageResult.getTotalElements(), safeOffset, size);
    }

    @Transactional(readOnly = true)
    public CustomerDetailResponse detail(UUID id) {
        return mapper.toDetail(find(id));
    }

    @Transactional
    public CustomerDetailResponse create(CreateCustomerRequest request) {
        String email = normalizeEmail(request.email());
        if (email != null && customerRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("Ya existe un cliente con el correo: " + email);
        }
        Customer customer = new Customer();
        apply(customer, request.name(), request.type(), email, request.phone(), resolveBranch(request.branchId()));
        customerRepository.save(customer);
        return mapper.toDetail(customer);
    }

    @Transactional
    public CustomerDetailResponse update(UUID id, UpdateCustomerRequest request) {
        Customer customer = find(id);
        String email = normalizeEmail(request.email());
        if (email != null && customerRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new DuplicateResourceException("Ya existe un cliente con el correo: " + email);
        }
        apply(customer, request.name(), request.type(), email, request.phone(), resolveBranch(request.branchId()));
        customerRepository.save(customer);
        return mapper.toDetail(customer);
    }

    @Transactional
    public void delete(UUID id) {
        Customer customer = find(id);
        if (orderRepository.countByCustomer_Id(id) > 0) {
            throw new ResourceInUseException("No se puede eliminar el cliente porque tiene ordenes asociadas");
        }
        if (ticketRepository.countByCustomer_Id(id) > 0) {
            throw new ResourceInUseException("No se puede eliminar el cliente porque tiene tickets asociados");
        }
        customerRepository.delete(customer);
    }

    private void apply(Customer customer, String name, CustomerType type, String email, String phone, Branch branch) {
        customer.setName(name.trim());
        customer.setType(type);
        customer.setEmail(email);
        customer.setPhone(phone == null ? null : phone.trim());
        customer.setBranch(branch);
    }

    private Page<Customer> fetch(CustomerType type, UUID branchId, PageRequest pageable) {
        if (type != null && branchId != null) {
            return customerRepository.findByTypeAndBranch_Id(type, branchId, pageable);
        }
        if (type != null) {
            return customerRepository.findByType(type, pageable);
        }
        if (branchId != null) {
            return customerRepository.findByBranch_Id(branchId, pageable);
        }
        return customerRepository.findAll(pageable);
    }

    private Branch resolveBranch(UUID branchId) {
        if (branchId == null) {
            return null;
        }
        return branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada: " + branchId));
    }

    private Customer find(UUID id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado: " + id));
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        String trimmed = email.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private int clampLimit(int limit) {
        if (limit <= 0) {
            return 20;
        }
        return Math.min(limit, 100);
    }
}
