package info.rio.project_ai901.repositories;

import info.rio.project_ai901.models.Branch;
import info.rio.project_ai901.models.Warehouse;
import info.rio.project_ai901.support.AbstractRepositoryTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WarehouseRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Test
    void buscaWarehousePorCodigoIgnorandoMayusculas() {
        warehouseRepository.saveAndFlush(warehouse("alm-01", "Almacen Norte", true, branch("SUC-01")));

        Optional<Warehouse> result = warehouseRepository.findFirstByCodeIgnoreCase("ALM-01");

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Almacen Norte");
    }

    @Test
    void verificaExistenciaPorCodigo() {
        warehouseRepository.saveAndFlush(warehouse("ALM-01", "Almacen Norte", true, branch("SUC-01")));

        assertThat(warehouseRepository.existsByCodeIgnoreCase("alm-01")).isTrue();
        assertThat(warehouseRepository.existsByCodeIgnoreCase("ALM-99")).isFalse();
    }

    @Test
    void verificaExistenciaPorCodigoExcluyendoElPropioId() {
        Branch sucursal = branch("SUC-01");
        Warehouse guardado = warehouseRepository.saveAndFlush(
                warehouse("ALM-01", "Almacen Norte", true, sucursal));
        warehouseRepository.saveAndFlush(warehouse("ALM-02", "Almacen Sur", true, sucursal));

        assertThat(warehouseRepository.existsByCodeIgnoreCaseAndIdNot("ALM-01", guardado.getId())).isFalse();
        assertThat(warehouseRepository.existsByCodeIgnoreCaseAndIdNot("ALM-02", guardado.getId())).isTrue();
    }

    @Test
    void verificaYCuentaAlmacenesActivosPorSucursal() {
        Branch sucursal = branch("SUC-01");
        warehouseRepository.saveAndFlush(warehouse("ALM-01", "Activo", true, sucursal));
        warehouseRepository.saveAndFlush(warehouse("ALM-02", "Inactivo", false, sucursal));
        warehouseRepository.flush();

        assertThat(warehouseRepository.existsByActiveTrueAndBranch_Id(sucursal.getId())).isTrue();
        assertThat(warehouseRepository.countByActiveTrueAndBranch_Id(sucursal.getId())).isEqualTo(1);
    }

    @Test
    void listaAlmacenesPorActivoYSucursal() {
        Branch sucursal = branch("SUC-01");
        warehouseRepository.saveAndFlush(warehouse("ALM-01", "Activo", true, sucursal));
        warehouseRepository.saveAndFlush(warehouse("ALM-02", "Inactivo", false, sucursal));
        warehouseRepository.flush();

        assertThat(warehouseRepository.findByActive(true, PageRequest.of(0, 10))).hasSize(1);
        assertThat(warehouseRepository.findByActiveAndBranch_Id(false, sucursal.getId(), PageRequest.of(0, 10)))
                .hasSize(1);
        assertThat(warehouseRepository.findByBranch_Id(sucursal.getId(), PageRequest.of(0, 10))).hasSize(2);
    }

    @Test
    void listaAlmacenesDeUnaSucursalVacia() {
        Branch sucursal = branch("SUC-VACIA");
        warehouseRepository.flush();

        assertThat(warehouseRepository.findByBranch_Id(sucursal.getId(), PageRequest.of(0, 10))).isEmpty();
        assertThat(warehouseRepository.countByActiveTrueAndBranch_Id(sucursal.getId())).isZero();
    }

    @Test
    void rechazaCodigoDuplicado() {
        Branch sucursal = branch("SUC-01");
        warehouseRepository.saveAndFlush(warehouse("ALM-01", "Almacen Norte", true, sucursal));

        assertThatThrownBy(() -> warehouseRepository
                .saveAndFlush(warehouse("ALM-01", "Otro", true, sucursal)))
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

    private Warehouse warehouse(String code, String name, boolean active, Branch branch) {
        Warehouse warehouse = new Warehouse();
        warehouse.setCode(code);
        warehouse.setName(name);
        warehouse.setActive(active);
        warehouse.setBranch(branch);
        return warehouse;
    }
}
