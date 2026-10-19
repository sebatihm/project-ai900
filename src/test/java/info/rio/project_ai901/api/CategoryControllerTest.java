package info.rio.project_ai901.api;

import info.rio.project_ai901.api.dto.CategoryDetailResponse;
import info.rio.project_ai901.api.dto.CategorySummaryResponse;
import info.rio.project_ai901.api.dto.PaginatedResponse;
import info.rio.project_ai901.config.SecurityConfig;
import info.rio.project_ai901.exceptions.DuplicateResourceException;
import info.rio.project_ai901.exceptions.ResourceInUseException;
import info.rio.project_ai901.exceptions.ResourceNotFoundException;
import info.rio.project_ai901.services.category.CategoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
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

@WebMvcTest(CategoryController.class)
@AutoConfigureMockMvc
@Import(SecurityConfig.class)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryService categoryService;

    @Test
    void listsCategoriesWithPagination() throws Exception {
        UUID id = UUID.randomUUID();
        when(categoryService.list(0L, 20)).thenReturn(new PaginatedResponse<>(
                List.of(new CategorySummaryResponse(id, "Bebidas")), 1L, 0L, 20));

        mockMvc.perform(get("/category"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.items[0].name").value("Bebidas"))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.offset").value(0))
                .andExpect(jsonPath("$.data.limit").value(20));
    }

    @Test
    void passesOffsetAndLimitToService() throws Exception {
        when(categoryService.list(anyLong(), anyInt())).thenReturn(
                new PaginatedResponse<>(List.of(), 0L, 10L, 5));

        mockMvc.perform(get("/category?offset=10&limit=5"))
                .andExpect(status().isOk());

        verify(categoryService).list(10L, 5);
    }

    @Test
    void returnsCategoryDetail() throws Exception {
        UUID id = UUID.randomUUID();
        when(categoryService.detail(id)).thenReturn(new CategoryDetailResponse(id, "Bebidas"));

        mockMvc.perform(get("/category/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(id.toString()))
                .andExpect(jsonPath("$.data.name").value("Bebidas"));
    }

    @Test
    void returns404WhenCategoryDoesNotExist() throws Exception {
        UUID id = UUID.randomUUID();
        when(categoryService.detail(id)).thenThrow(new ResourceNotFoundException("Categoria no encontrada: " + id));

        mockMvc.perform(get("/category/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Recurso no encontrado"))
                .andExpect(jsonPath("$.detail").value("Categoria no encontrada: " + id));
    }

    @Test
    void createsCategoryWith201() throws Exception {
        UUID id = UUID.randomUUID();
        when(categoryService.create(any())).thenReturn(new CategoryDetailResponse(id, "Bebidas"));

        mockMvc.perform(post("/category")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Bebidas\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.data.name").value("Bebidas"));
    }

    @Test
    void rejectsBlankNameWith400() throws Exception {
        mockMvc.perform(post("/category")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Validacion fallida"))
                .andExpect(jsonPath("$.errors[0]").value("name: El nombre es obligatorio"));

        verify(categoryService, never()).create(any());
    }

    @Test
    void returns409WhenNameAlreadyExists() throws Exception {
        when(categoryService.create(any())).thenThrow(new DuplicateResourceException("Ya existe"));

        mockMvc.perform(post("/category")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Bebidas\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.title").value("Recurso duplicado"));
    }

    @Test
    void updatesCategory() throws Exception {
        UUID id = UUID.randomUUID();
        when(categoryService.update(eq(id), any())).thenReturn(new CategoryDetailResponse(id, "Abarrotes"));

        mockMvc.perform(put("/category/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Abarrotes\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Abarrotes"));
    }

    @Test
    void deletesCategory() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/category/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(categoryService).delete(id);
    }

    @Test
    void returns409WhenCategoryHasProducts() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new ResourceInUseException("No se puede eliminar la categoria porque tiene productos asociados"))
                .when(categoryService).delete(id);

        mockMvc.perform(delete("/category/{id}", id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Recurso en uso"));
    }

    @Test
    void returns404WhenDeletingUnknownCategory() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new ResourceNotFoundException("Categoria no encontrada: " + id)).when(categoryService).delete(id);

        mockMvc.perform(delete("/category/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso no encontrado"));
    }
}
