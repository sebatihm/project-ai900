package info.rio.project_ai901.services.customer;

import info.rio.project_ai901.api.dto.CreateCustomerRequest;
import info.rio.project_ai901.api.dto.CustomerDetailResponse;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CustomerServiceTest {

    private CustomerRepository customerRepository;
    private BranchRepository branchRepository;
    private OrderRepository orderRepository;
    private TicketRepository ticketRepository;
    private CustomerService service;

    @BeforeEach
    void setUp() {
        customerRepository = Mockito.mock(CustomerRepository.class);
        branchRepository = Mockito.mock(BranchRepository.class);
        orderRepository = Mockito.mock(OrderRepository.class);
        ticketRepository = Mockito.mock(TicketRepository.class);
        service = new CustomerService(customerRepository, branchRepository, orderRepository, ticketRepository,
                new CustomerMapper());
    }

    @Test
    void createsCustomerWithResolvedBranch() {
        UUID branchId = UUID.randomUUID();
        when(customerRepository.existsByEmailIgnoreCase("contacto@miramar.test")).thenReturn(false);
        when(branchRepository.findById(branchId)).thenReturn(Optional.of(branch(branchId)));

        CustomerDetailResponse result = service.create(new CreateCustomerRequest("Hotel Miramar",
                CustomerType.HOTEL, " contacto@miramar.test ", " 555 ", branchId));

        assertThat(result.name()).isEqualTo("Hotel Miramar");
        assertThat(result.email()).isEqualTo("contacto@miramar.test");
        assertThat(result.phone()).isEqualTo("555");
        assertThat(result.branchId()).isEqualTo(branchId);
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    void createsCustomerWithoutBranch() {
        CustomerDetailResponse result = service.create(
                new CreateCustomerRequest("Cliente Web", CustomerType.STORE, null, null, null));

        assertThat(result.branchId()).isNull();
        verify(branchRepository, never()).findById(any(UUID.class));
    }

    @Test
    void rejectsDuplicatedEmail() {
        when(customerRepository.existsByEmailIgnoreCase("contacto@miramar.test")).thenReturn(true);

        assertThatThrownBy(() -> service.create(new CreateCustomerRequest("Otro Hotel", CustomerType.HOTEL,
                "contacto@miramar.test", null, null)))
                .isInstanceOf(DuplicateResourceException.class);
        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    void rejectsUnknownBranch() {
        UUID branchId = UUID.randomUUID();
        when(customerRepository.existsByEmailIgnoreCase(any())).thenReturn(false);
        when(branchRepository.findById(branchId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(new CreateCustomerRequest("Hotel Miramar", CustomerType.HOTEL,
                "contacto@miramar.test", null, branchId)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updatesCustomerSuccessfully() {
        UUID id = UUID.randomUUID();
        when(customerRepository.findById(id)).thenReturn(Optional.of(customer(id)));
        when(customerRepository.existsByEmailIgnoreCaseAndIdNot("nuevo@miramar.test", id)).thenReturn(false);

        CustomerDetailResponse result = service.update(id, new UpdateCustomerRequest("Hotel Miramar Norte",
                CustomerType.HOTEL, "nuevo@miramar.test", "666", null));

        assertThat(result.id()).isEqualTo(id);
        assertThat(result.email()).isEqualTo("nuevo@miramar.test");
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    void throwsNotFoundForUnknownCustomer() {
        UUID id = UUID.randomUUID();
        when(customerRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.detail(id)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.delete(id)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void refusesToDeleteCustomerWithOrders() {
        UUID id = UUID.randomUUID();
        when(customerRepository.findById(id)).thenReturn(Optional.of(customer(id)));
        when(orderRepository.countByCustomer_Id(id)).thenReturn(3L);

        assertThatThrownBy(() -> service.delete(id)).isInstanceOf(ResourceInUseException.class);
        verify(customerRepository, never()).delete(any(Customer.class));
    }

    @Test
    void refusesToDeleteCustomerWithTickets() {
        UUID id = UUID.randomUUID();
        when(customerRepository.findById(id)).thenReturn(Optional.of(customer(id)));
        when(orderRepository.countByCustomer_Id(id)).thenReturn(0L);
        when(ticketRepository.countByCustomer_Id(id)).thenReturn(1L);

        assertThatThrownBy(() -> service.delete(id)).isInstanceOf(ResourceInUseException.class);
        verify(customerRepository, never()).delete(any(Customer.class));
    }

    @Test
    void deletesCustomerWithoutDependencies() {
        UUID id = UUID.randomUUID();
        Customer customer = customer(id);
        when(customerRepository.findById(id)).thenReturn(Optional.of(customer));
        when(orderRepository.countByCustomer_Id(id)).thenReturn(0L);
        when(ticketRepository.countByCustomer_Id(id)).thenReturn(0L);

        service.delete(id);

        verify(customerRepository).delete(customer);
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

    private Customer customer(UUID id) {
        Customer customer = new Customer();
        customer.setId(id);
        customer.setName("Hotel Miramar");
        customer.setType(CustomerType.HOTEL);
        customer.setEmail("contacto@miramar.test");
        customer.setPhone("555");
        return customer;
    }
}
