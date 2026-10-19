package info.rio.project_ai901.repositories;

import info.rio.project_ai901.models.Branch;
import info.rio.project_ai901.support.AbstractRepositoryTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BranchRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private BranchRepository branchRepository;

    @Test
    void buscaBranchPorCodigoIgnorandoMayusculas() {
        branchRepository.saveAndFlush(branch("suc-01", "Sucursal Centro", true));

        Optional<Branch> result = branchRepository.findFirstByCodeIgnoreCase("SUC-01");

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Sucursal Centro");
    }

    @Test
    void verificaExistenciaPorCodigo() {
        branchRepository.saveAndFlush(branch("SUC-01", "Sucursal Centro", true));

        assertThat(branchRepository.existsByCodeIgnoreCase("suc-01")).isTrue();
        assertThat(branchRepository.existsByCodeIgnoreCase("SUC-99")).isFalse();
    }

    @Test
    void verificaExistenciaPorCodigoExcluyendoElPropioId() {
        Branch guardada = branchRepository.saveAndFlush(branch("SUC-01", "Sucursal Centro", true));
        branchRepository.saveAndFlush(branch("SUC-02", "Sucursal Norte", true));

        assertThat(branchRepository.existsByCodeIgnoreCaseAndIdNot("SUC-01", guardada.getId())).isFalse();
        assertThat(branchRepository.existsByCodeIgnoreCaseAndIdNot("SUC-02", guardada.getId())).isTrue();
    }

    @Test
    void listaBranchesPorActivo() {
        branchRepository.saveAndFlush(branch("SUC-01", "Activa", true));
        branchRepository.saveAndFlush(branch("SUC-02", "Inactiva", false));
        branchRepository.flush();

        assertThat(branchRepository.findByActive(true, PageRequest.of(0, 10))).hasSize(1);
        assertThat(branchRepository.findByActive(false, PageRequest.of(0, 10))).hasSize(1);
    }

    @Test
    void rechazaCodigoDuplicado() {
        branchRepository.saveAndFlush(branch("SUC-01", "Sucursal Centro", true));

        assertThatThrownBy(() -> branchRepository.saveAndFlush(branch("SUC-01", "Otra", true)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private Branch branch(String code, String name, boolean active) {
        Branch branch = new Branch();
        branch.setCode(code);
        branch.setName(name);
        branch.setCity("CDMX");
        branch.setActive(active);
        return branch;
    }
}
