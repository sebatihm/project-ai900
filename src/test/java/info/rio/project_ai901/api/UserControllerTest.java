package info.rio.project_ai901.api;

import info.rio.project_ai901.api.dto.PaginatedResponse;
import info.rio.project_ai901.api.dto.UserDetailResponse;
import info.rio.project_ai901.api.dto.UserSummaryResponse;
import info.rio.project_ai901.config.SecurityConfig;
import info.rio.project_ai901.exceptions.DuplicateResourceException;
import info.rio.project_ai901.exceptions.ResourceInUseException;
import info.rio.project_ai901.exceptions.ResourceNotFoundException;
import info.rio.project_ai901.models.enums.RoleName;
import info.rio.project_ai901.services.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc
@Import(SecurityConfig.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void listsUsersForAdmin() throws Exception {
        UUID id = UUID.randomUUID();
        when(userService.list(null, null, null, false, 0L, 20)).thenReturn(new PaginatedResponse<>(
                List.of(new UserSummaryResponse(id, "Ana", "ana@ai901.local", RoleName.ADMIN, null, null, null, null)),
                1L, 0L, 20));

        mockMvc.perform(get("/user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.items[0].email").value("ana@ai901.local"))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    @WithMockUser(roles = "BOSS")
    void listsUsersForBoss() throws Exception {
        when(userService.list(null, null, null, false, 0L, 20)).thenReturn(new PaginatedResponse<>(List.of(), 0L, 0L, 20));

        mockMvc.perform(get("/user"))
                .andExpect(status().isOk());
    }

    @Test
    void returns401WhenNoTokenIsProvided() throws Exception {
        mockMvc.perform(get("/user"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "WAREHOUSE_HEAD")
    void returns403ForRolesWithoutPermission() throws Exception {
        mockMvc.perform(get("/user"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void returnsUserDetail() throws Exception {
        UUID id = UUID.randomUUID();
        when(userService.detail(id)).thenReturn(detail(id));

        mockMvc.perform(get("/user/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("ana@ai901.local"))
                .andExpect(jsonPath("$.data.role").value("ADMIN"))
                .andExpect(jsonPath("$.data.active").value(true));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void returns404WhenUserDoesNotExist() throws Exception {
        UUID id = UUID.randomUUID();
        when(userService.detail(id)).thenThrow(new ResourceNotFoundException("Usuario no encontrado: " + id));

        mockMvc.perform(get("/user/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso no encontrado"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createsUserWith201() throws Exception {
        UUID id = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        when(userService.create(any())).thenReturn(detail(id));

        mockMvc.perform(post("/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ana\",\"email\":\"ana@ai901.local\",\"password\":\"secreto123\","
                                + "\"roleId\":\"" + roleId + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.data.email").value("ana@ai901.local"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void rejectsInvalidEmailWith400() throws Exception {
        mockMvc.perform(post("/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ana\",\"email\":\"correo-invalido\",\"password\":\"secreto123\","
                                + "\"roleId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validacion fallida"));

        verify(userService, never()).create(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void rejectsShortPasswordWith400() throws Exception {
        mockMvc.perform(post("/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ana\",\"email\":\"ana@ai901.local\",\"password\":\"corta\","
                                + "\"roleId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isBadRequest());

        verify(userService, never()).create(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void returns409WhenEmailAlreadyExists() throws Exception {
        when(userService.create(any())).thenThrow(new DuplicateResourceException("Ya existe"));

        mockMvc.perform(post("/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ana\",\"email\":\"ana@ai901.local\",\"password\":\"secreto123\","
                                + "\"roleId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Recurso duplicado"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updatesUser() throws Exception {
        UUID id = UUID.randomUUID();
        when(userService.update(any(), any())).thenReturn(detail(id));

        mockMvc.perform(put("/user/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ana Dos\",\"email\":\"ana@ai901.local\",\"roleId\":\""
                                + UUID.randomUUID() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("ana@ai901.local"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void changesPassword() throws Exception {
        UUID id = UUID.randomUUID();
        when(userService.changePassword(any(), any())).thenReturn(detail(id));

        mockMvc.perform(put("/user/{id}/password", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"oldPassword\":\"vieja123\",\"newPassword\":\"nueva1234\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deactivatesUser() throws Exception {
        UUID id = UUID.randomUUID();
        UserDetailResponse inactive = new UserDetailResponse(id, "Ana", "ana@ai901.local", RoleName.ADMIN, null, null,
                null, null, false, LocalDateTime.now(), LocalDateTime.now());
        when(userService.deactivate(id)).thenReturn(inactive);

        mockMvc.perform(delete("/user/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(false));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void returns409WhenUserHasPendingInvoices() throws Exception {
        UUID id = UUID.randomUUID();
        when(userService.deactivate(id)).thenThrow(new ResourceInUseException("Tiene facturas pendientes"));

        mockMvc.perform(delete("/user/{id}", id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Recurso en uso"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void activatesUser() throws Exception {
        UUID id = UUID.randomUUID();
        when(userService.activate(id)).thenReturn(detail(id));

        mockMvc.perform(put("/user/{id}/activate", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(true));
    }

    private UserDetailResponse detail(UUID id) {
        return new UserDetailResponse(id, "Ana", "ana@ai901.local", RoleName.ADMIN, null, null, null, null, true,
                LocalDateTime.now(), LocalDateTime.now());
    }
}
