package info.rio.project_ai901.repositories;

import info.rio.project_ai901.models.Category;
import info.rio.project_ai901.support.AbstractRepositoryTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CategoryRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void buscaCategoriaPorNombreIgnorandoMayusculas() {
        categoryRepository.saveAndFlush(category("Bebidas"));

        Optional<Category> result = categoryRepository.findByNameIgnoreCase("BEBIDAS");

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Bebidas");
    }

    @Test
    void noEncuentraCategoriaInexistente() {
        Optional<Category> result = categoryRepository.findByNameIgnoreCase("NoExiste");

        assertThat(result).isEmpty();
    }

    @Test
    void verificaExistenciaPorNombre() {
        categoryRepository.saveAndFlush(category("Bebidas"));

        assertThat(categoryRepository.existsByNameIgnoreCase("bebidas")).isTrue();
        assertThat(categoryRepository.existsByNameIgnoreCase("Snacks")).isFalse();
    }

    @Test
    void verificaExistenciaPorNombreExcluyendoElPropioId() {
        Category guardada = categoryRepository.saveAndFlush(category("Bebidas"));
        categoryRepository.saveAndFlush(category("Snacks"));

        assertThat(categoryRepository.existsByNameIgnoreCaseAndIdNot("Bebidas", guardada.getId())).isFalse();
        assertThat(categoryRepository.existsByNameIgnoreCaseAndIdNot("Snacks", guardada.getId())).isTrue();
    }

    @Test
    void rechazaNombreDuplicado() {
        categoryRepository.saveAndFlush(category("Bebidas"));

        assertThatThrownBy(() -> categoryRepository.saveAndFlush(category("Bebidas")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private Category category(String name) {
        Category category = new Category();
        category.setName(name);
        return category;
    }
}
