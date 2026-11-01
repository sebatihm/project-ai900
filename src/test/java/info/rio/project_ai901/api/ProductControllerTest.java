package info.rio.project_ai901.api;

import info.rio.project_ai901.api.dto.PaginatedResponse;
import info.rio.project_ai901.api.dto.ProductDetailResponse;
import info.rio.project_ai901.api.dto.ProductSummaryResponse;
import info.rio.project_ai901.config.SecurityConfig;
import info.rio.project_ai901.exceptions.DuplicateResourceException;
import info.rio.project_ai901.exceptions.ResourceInUseException;
import info.rio.project_ai901.exceptions.ResourceNotFoundException;
import info.rio.project_ai901.services.product.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@AutoConfigureMockMvc
@Import(SecurityConfig.class)
@WithMockUser(roles = "ADMIN")
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

        @Test
        void listsProducts() throws Exception {
                UUID id = UUID.randomUUID();
                UUID categoryId = UUID.randomUUID();
                when(productService.list(0L, 20, null)).thenReturn(new PaginatedResponse<>(List.of(
                        new ProductSummaryResponse(id, "SKU-001", "Coca Cola", "ml", new BigDecimal("12.50"),
                                categoryId, "Bebidas")), 1L, 0L, 20));

                mockMvc.perform(get("/product"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.items[0].sku").value("SKU-001"))
                        .andExpect(jsonPath("$.data.items[0].categoryName").value("Bebidas"));
        }

        @Test
        void passesPaginationToService() throws Exception {
                when(productService.list(anyLong(), anyInt(), any())).thenReturn(new PaginatedResponse<>(List.of(), 0L, 20L, 10));

                mockMvc.perform(get("/product?offset=20&limit=10"))
                        .andExpect(status().isOk());

                verify(productService).list(20L, 10, null);
        }

    @Test
    void returnsProductDetail() throws Exception {
        UUID id = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        when(productService.detail(id)).thenReturn(new ProductDetailResponse(id, "SKU-001", "Coca Cola", "ml",
                new BigDecimal("12.50"), categoryId, "Bebidas"));

        mockMvc.perform(get("/product/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sku").value("SKU-001"))
                .andExpect(jsonPath("$.data.price").value(12.50));
    }

    @Test
    void returns404WhenProductDoesNotExist() throws Exception {
        UUID id = UUID.randomUUID();
        when(productService.detail(id)).thenThrow(new ResourceNotFoundException("Producto no encontrado: " + id));

        mockMvc.perform(get("/product/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso no encontrado"));
    }

    @Test
    void createsProductWith201() throws Exception {
        UUID id = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        when(productService.create(any())).thenReturn(new ProductDetailResponse(id, "SKU-001", "Coca Cola", "ml",
                new BigDecimal("12.50"), categoryId, "Bebidas"));

        mockMvc.perform(post("/product")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"SKU-001\",\"name\":\"Coca Cola\",\"categoryId\":\"" + categoryId
                                + "\",\"unit\":\"ml\",\"price\":12.50}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.data.sku").value("SKU-001"));
    }

    @Test
    void rejectsBlankNameWith400() throws Exception {
        mockMvc.perform(post("/product")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"SKU-001\",\"name\":\"\",\"categoryId\":\"" + UUID.randomUUID()
                                + "\",\"unit\":\"ml\",\"price\":12.50}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validacion fallida"))
                .andExpect(jsonPath("$.errors[0]").value("name: El nombre es obligatorio"));

        verify(productService, never()).create(any());
    }

    @Test
    void rejectsNonPositivePriceWith400() throws Exception {
        mockMvc.perform(post("/product")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"SKU-001\",\"name\":\"Coca Cola\",\"categoryId\":\""
                                + UUID.randomUUID() + "\",\"unit\":\"ml\",\"price\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0]").value("price: El precio debe ser mayor a 0"));
    }

    @Test
    void returns409WhenSkuAlreadyExists() throws Exception {
        when(productService.create(any())).thenThrow(new DuplicateResourceException("Ya existe"));

        mockMvc.perform(post("/product")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"SKU-001\",\"name\":\"Otro\",\"categoryId\":\""
                                + UUID.randomUUID() + "\",\"unit\":\"ml\",\"price\":12.50}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Recurso duplicado"));
    }

    @Test
    void returns404WhenCategoryDoesNotExist() throws Exception {
        when(productService.create(any()))
                .thenThrow(new ResourceNotFoundException("Categoria no encontrada"));

        mockMvc.perform(post("/product")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"SKU-001\",\"name\":\"Otro\",\"categoryId\":\""
                                + UUID.randomUUID() + "\",\"unit\":\"ml\",\"price\":12.50}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso no encontrado"));
    }

    @Test
    void updatesProduct() throws Exception {
        UUID id = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        when(productService.update(any(), any())).thenReturn(new ProductDetailResponse(id, "SKU-002",
                "Refresco", "lt", new BigDecimal("9.99"), categoryId, "Bebidas"));

        mockMvc.perform(put("/product/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"SKU-002\",\"name\":\"Refresco\",\"categoryId\":\"" + categoryId
                                + "\",\"unit\":\"lt\",\"price\":9.99}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.price").value(9.99));
    }

    @Test
    void deletesProduct() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/product/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(productService).delete(id);
    }

    @Test
    void returns409WhenProductHasInventory() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new ResourceInUseException("No se puede eliminar el producto porque tiene inventario asociado"))
                .when(productService).delete(id);

        mockMvc.perform(delete("/product/{id}", id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Recurso en uso"));
    }
}
