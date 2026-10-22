package info.rio.project_ai901.repositories;

import info.rio.project_ai901.models.Category;
import info.rio.project_ai901.models.Product;
import info.rio.project_ai901.support.AbstractRepositoryTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void verificaExistenciaPorSkuIgnorandoMayusculas() {
        productRepository.saveAndFlush(product("sku-001", "Producto A", category("Bebidas")));

        assertThat(productRepository.existsBySkuIgnoreCase("SKU-001")).isTrue();
        assertThat(productRepository.existsBySkuIgnoreCase("SKU-999")).isFalse();
    }

    @Test
    void verificaExistenciaPorSkuExcluyendoElPropioId() {
        Product guardado = productRepository.saveAndFlush(product("SKU-001", "Producto A", category("Bebidas")));
        productRepository.saveAndFlush(product("SKU-002", "Producto B", category("Bebidas")));

        assertThat(productRepository.existsBySkuIgnoreCaseAndIdNot("SKU-001", guardado.getId())).isFalse();
        assertThat(productRepository.existsBySkuIgnoreCaseAndIdNot("SKU-002", guardado.getId())).isTrue();
    }

    @Test
    void cuentaProductosPorCategoria() {
        Category bebidas = categoryRepository.saveAndFlush(category("Bebidas"));
        Category snacks = categoryRepository.saveAndFlush(category("Snacks"));
        productRepository.saveAndFlush(product("SKU-001", "Producto A", bebidas));
        productRepository.saveAndFlush(product("SKU-002", "Producto B", bebidas));
        productRepository.saveAndFlush(product("SKU-003", "Producto C", snacks));
        productRepository.flush();

        assertThat(productRepository.countByCategory_Id(bebidas.getId())).isEqualTo(2);
        assertThat(productRepository.countByCategory_Id(snacks.getId())).isEqualTo(1);
        assertThat(productRepository.countByCategory_Id(java.util.UUID.randomUUID())).isZero();
    }

    @Test
    void rechazaSkuDuplicado() {
        productRepository.saveAndFlush(product("SKU-001", "Producto A", category("Bebidas")));

        assertThatThrownBy(() -> productRepository.saveAndFlush(product("SKU-001", "Otro", category("Bebidas"))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private Category category(String name) {
        return categoryRepository.findByNameIgnoreCase(name).orElseGet(() -> {
            Category nueva = new Category();
            nueva.setName(name);
            return categoryRepository.saveAndFlush(nueva);
        });
    }

    private Product product(String sku, String name, Category category) {
        Product product = new Product();
        product.setSku(sku);
        product.setName(name);
        product.setUnit("kg");
        product.setPrice(new BigDecimal("10.00"));
        product.setCategory(category);
        return product;
    }
}
