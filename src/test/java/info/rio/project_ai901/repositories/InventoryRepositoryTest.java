package info.rio.project_ai901.repositories;

import info.rio.project_ai901.models.Branch;
import info.rio.project_ai901.models.Category;
import info.rio.project_ai901.models.Inventory;
import info.rio.project_ai901.models.Product;
import info.rio.project_ai901.models.Warehouse;
import info.rio.project_ai901.support.AbstractRepositoryTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InventoryRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Test
    void buscaInventarioPorBodegaYProducto() {
        Warehouse bodega = bodega("ALM-01");
        Product producto = producto("SKU-001");
        inventoryRepository.saveAndFlush(inventario(bodega, producto, 25));

        Optional<Inventory> result = inventoryRepository.findByWarehouse_IdAndProduct_Id(bodega.getId(),
                producto.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getQuantity()).isEqualTo(25);
    }

    @Test
    void verificaExistenciaDeInventarioPorBodegaYProducto() {
        Warehouse bodega = bodega("ALM-01");
        Product producto = producto("SKU-001");
        inventoryRepository.saveAndFlush(inventario(bodega, producto, 25));
        inventoryRepository.flush();

        assertThat(inventoryRepository.existsByWarehouse_IdAndProduct_Id(bodega.getId(), producto.getId())).isTrue();
        assertThat(inventoryRepository.existsByWarehouse_IdAndProduct_Id(bodega.getId(), producto("SKU-999").getId()))
                .isFalse();
    }

    @Test
    void listaInventarioPorBodegaYProducto() {
        Warehouse bodega1 = bodega("ALM-01");
        Warehouse bodega2 = bodega("ALM-02");
        Product producto1 = producto("SKU-001");
        Product producto2 = producto("SKU-002");
        inventoryRepository.saveAndFlush(inventario(bodega1, producto1, 10));
        inventoryRepository.saveAndFlush(inventario(bodega1, producto2, 20));
        inventoryRepository.saveAndFlush(inventario(bodega2, producto1, 30));
        inventoryRepository.flush();

        assertThat(inventoryRepository.findByWarehouse_Id(bodega1.getId(), PageRequest.of(0, 10)))
                .hasSize(2);
        assertThat(inventoryRepository.findByProduct_Id(producto1.getId(), PageRequest.of(0, 10)))
                .hasSize(2);
        assertThat(inventoryRepository.findByWarehouse_IdAndProduct_Id(bodega1.getId(), producto1.getId(),
                PageRequest.of(0, 10))).hasSize(1);
    }

    @Test
    void cuentaInventarioPorProductoYBodega() {
        Warehouse bodega = bodega("ALM-01");
        Product producto = producto("SKU-001");
        Product otro = producto("SKU-002");
        inventoryRepository.saveAndFlush(inventario(bodega, producto, 5));
        inventoryRepository.saveAndFlush(inventario(bodega, otro, 7));
        inventoryRepository.flush();

        assertThat(inventoryRepository.countByProduct_Id(producto.getId())).isEqualTo(1);
        assertThat(inventoryRepository.countByWarehouse_Id(bodega.getId())).isEqualTo(2);
        assertThat(inventoryRepository.countByProduct_Id(UUID.randomUUID())).isZero();
        assertThat(inventoryRepository.countByWarehouse_Id(UUID.randomUUID())).isZero();
    }

    @Test
    void rechazaParBodegaProductoDuplicado() {
        Warehouse bodega = bodega("ALM-01");
        Product producto = producto("SKU-001");
        inventoryRepository.saveAndFlush(inventario(bodega, producto, 10));

        assertThatThrownBy(() -> inventoryRepository.saveAndFlush(inventario(bodega, producto, 99)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private Warehouse bodega(String code) {
        Branch branch = new Branch();
        branch.setCode("SUC-" + code.substring(code.length() - 2));
        branch.setName("Sucursal " + code);
        branch.setCity("CDMX");
        branchRepository.saveAndFlush(branch);

        Warehouse warehouse = new Warehouse();
        warehouse.setCode(code);
        warehouse.setName("Almacen " + code);
        warehouse.setActive(true);
        warehouse.setBranch(branch);
        return warehouseRepository.saveAndFlush(warehouse);
    }

    private Product producto(String sku) {
        Category category = categoryRepository.findByNameIgnoreCase("Generica")
                .orElseGet(() -> {
                    Category nueva = new Category();
                    nueva.setName("Generica");
                    return categoryRepository.saveAndFlush(nueva);
                });

        Product product = new Product();
        product.setSku(sku);
        product.setName("Producto " + sku);
        product.setUnit("kg");
        product.setPrice(new BigDecimal("10.00"));
        product.setCategory(category);
        return productRepository.saveAndFlush(product);
    }

    private Inventory inventario(Warehouse warehouse, Product product, int quantity) {
        Inventory inventory = new Inventory();
        inventory.setWarehouse(warehouse);
        inventory.setProduct(product);
        inventory.setQuantity(quantity);
        return inventory;
    }
}
