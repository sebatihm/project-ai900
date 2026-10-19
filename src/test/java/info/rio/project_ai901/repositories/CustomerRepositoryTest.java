package info.rio.project_ai901.repositories;

import info.rio.project_ai901.models.Branch;
import info.rio.project_ai901.models.Customer;
import info.rio.project_ai901.models.enums.CustomerType;
import info.rio.project_ai901.support.AbstractRepositoryTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Test
    void verificaExistenciaDeEmailIgnorandoMayusculas() {
        customerRepository.saveAndFlush(customer("Hotel Miramar", CustomerType.HOTEL, "contacto@miramar.test", null));

        assertThat(customerRepository.existsByEmailIgnoreCase("CONTACTO@MIRAMAR.TEST")).isTrue();
        assertThat(customerRepository.existsByEmailIgnoreCase("otro@miramar.test")).isFalse();
    }

    @Test
    void verificaExistenciaDeEmailExcluyendoElPropioId() {
        Customer guardado = customerRepository.saveAndFlush(
                customer("Hotel Miramar", CustomerType.HOTEL, "contacto@miramar.test", null));
        customerRepository.saveAndFlush(customer("Hotel Norte", CustomerType.HOTEL, "norte@miramar.test", null));

        assertThat(customerRepository.existsByEmailIgnoreCaseAndIdNot("contacto@miramar.test", guardado.getId()))
                .isFalse();
        assertThat(customerRepository.existsByEmailIgnoreCaseAndIdNot("norte@miramar.test", guardado.getId()))
                .isTrue();
    }

    @Test
    void listaClientesPorTipo() {
        customerRepository.saveAndFlush(customer("Hotel Miramar", CustomerType.HOTEL, "a@x.test", null));
        customerRepository.saveAndFlush(customer("Escuela Norte", CustomerType.SCHOOL, "b@x.test", null));
        customerRepository.saveAndFlush(customer("Tienda Sur", CustomerType.STORE, "c@x.test", null));
        customerRepository.flush();

        assertThat(customerRepository.findByType(CustomerType.HOTEL, PageRequest.of(0, 10))).hasSize(1);
        assertThat(customerRepository.findByType(CustomerType.STORE, PageRequest.of(0, 10))).hasSize(1);
    }

    @Test
    void listaClientesPorSucursal() {
        Branch sucursal = branch("SUC-01");
        branchRepository.saveAndFlush(sucursal);
        customerRepository.saveAndFlush(customer("Cliente Local", CustomerType.HOTEL, "local@x.test", sucursal));
        customerRepository.saveAndFlush(customer("Cliente Web", CustomerType.STORE, "web@x.test", null));
        customerRepository.flush();

        assertThat(customerRepository.findByBranch_Id(sucursal.getId(), PageRequest.of(0, 10))).hasSize(1);
    }

    @Test
    void listaClientesPorTipoYSucursal() {
        Branch sucursal = branch("SUC-01");
        branchRepository.saveAndFlush(sucursal);
        customerRepository.saveAndFlush(customer("Hotel Miramar", CustomerType.HOTEL, "a@x.test", sucursal));
        customerRepository.saveAndFlush(customer("Hotel Norte", CustomerType.HOTEL, "b@x.test", null));
        customerRepository.saveAndFlush(customer("Tienda Local", CustomerType.STORE, "c@x.test", sucursal));
        customerRepository.flush();

        assertThat(customerRepository.findByTypeAndBranch_Id(CustomerType.HOTEL, sucursal.getId(),
                PageRequest.of(0, 10))).hasSize(1);
    }

    private Branch branch(String code) {
        Branch branch = new Branch();
        branch.setCode(code);
        branch.setName("Sucursal " + code);
        branch.setCity("CDMX");
        branch.setActive(true);
        return branch;
    }

    private Customer customer(String name, CustomerType type, String email, Branch branch) {
        Customer customer = new Customer();
        customer.setName(name);
        customer.setType(type);
        customer.setEmail(email);
        customer.setPhone("555");
        customer.setBranch(branch);
        return customer;
    }
}
