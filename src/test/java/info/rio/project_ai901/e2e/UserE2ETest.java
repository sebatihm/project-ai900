package info.rio.project_ai901.e2e;

import com.jayway.jsonpath.JsonPath;
import info.rio.project_ai901.models.enums.RoleName;
import info.rio.project_ai901.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Flujo completo del modulo de usuarios con autenticacion JWT real: login que
 * emite token, autorizacion por rol sobre /user y /role, CRUD de usuarios y
 * cambio de contrasena.
 */
class UserE2ETest extends AbstractIntegrationTest {

    private static final String PASSWORD = "Admin@123";

    @Test
    void loginEmiteTokenParaElAdministrador() throws Exception {
        createUser("Administrador", "admin@ai901.local", PASSWORD, RoleName.ADMIN);

        String response = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@ai901.local\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token = JsonPath.parse(response).read("$.data.token", String.class);
        assertThat(token).isNotBlank();
        assertThat(JsonPath.parse(response).read("$.data.tokenType", String.class)).isEqualTo("Bearer");
        assertThat(JsonPath.parse(response).read("$.data.user.role", String.class)).isEqualTo("ADMIN");
    }

    @Test
    void loginConCredencialesInvalidasRetorna401() throws Exception {
        createUser("Administrador", "admin@ai901.local", PASSWORD, RoleName.ADMIN);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@ai901.local\",\"password\":\"incorrecta\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rechazaOperacionesSobreUsuariosSinToken() throws Exception {
        mockMvc.perform(get("/user"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rechazaOperacionesSobreUsuariosConRolNoAutorizado() throws Exception {
        createUser("Jefe", "jefe@ai901.local", PASSWORD, RoleName.WAREHOUSE_HEAD);
        String token = login("jefe@ai901.local", PASSWORD);

        mockMvc.perform(get("/user").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void elAdministradorCreaUsuariosYLosLista() throws Exception {
        UUID roleId = createRole(RoleName.ADMIN);
        createUser("Administrador", "admin@ai901.local", PASSWORD, RoleName.ADMIN);
        String token = login("admin@ai901.local", PASSWORD);

        String created = postJsonWithToken("/user",
                "{\"name\":\"Ana\",\"email\":\"ana@ai901.local\",\"password\":\"secreto123\",\"roleId\":\""
                        + roleId + "\"}",
                token);
        assertThat(JsonPath.parse(created).read("$.data.email", String.class)).isEqualTo("ana@ai901.local");
        assertThat(JsonPath.parse(created).read("$.data.active", Boolean.class)).isTrue();

        String list = getJsonWithToken("/user", token);
        assertThat(JsonPath.parse(list).read("$.data.total", Integer.class)).isEqualTo(2);
        assertThat(list).contains("ana@ai901.local");
    }

    @Test
    void elAdministradorAccedeAlListadoDeRoles() throws Exception {
        createUser("Administrador", "admin@ai901.local", PASSWORD, RoleName.ADMIN);
        String token = login("admin@ai901.local", PASSWORD);

        String roles = getJsonWithToken("/role", token);

        assertThat(JsonPath.parse(roles).read("$.data.length()", Integer.class)).isEqualTo(1);
        assertThat(JsonPath.parse(roles).read("$.data[0].name", String.class)).isEqualTo("ADMIN");
    }

    @Test
    void cambiaLaContrasenaYPuedeIniciarSesionConLaNueva() throws Exception {
        UUID userId = createUser("Ana", "ana@ai901.local", PASSWORD, RoleName.ADMIN);
        createUser("Administrador", "admin@ai901.local", PASSWORD, RoleName.ADMIN);
        String token = login("admin@ai901.local", PASSWORD);

        putJsonWithToken("/user/" + userId + "/password",
                "{\"oldPassword\":\"" + PASSWORD + "\",\"newPassword\":\"NuevaClave123\"}", token);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ana@ai901.local\",\"password\":\"NuevaClave123\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ana@ai901.local\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void elPasswordNoSeExponeEnLasRespuestas() throws Exception {
        UUID roleId = createRole(RoleName.ADMIN);
        createUser("Administrador", "admin@ai901.local", PASSWORD, RoleName.ADMIN);
        String token = login("admin@ai901.local", PASSWORD);

        String created = postJsonWithToken("/user",
                "{\"name\":\"Ana\",\"email\":\"ana@ai901.local\",\"password\":\"secreto123\",\"roleId\":\""
                        + roleId + "\"}",
                token);

        assertThat(created).doesNotContain("secreto123");
        assertThat(JsonPath.parse(created).read("$.data", Map.class)).doesNotContainKey("password");
    }

    @Test
    void desactivaElUsuarioYLoOcultaDelListadoPorDefecto() throws Exception {
        UUID userId = createUser("Ana", "ana@ai901.local", PASSWORD, RoleName.ADMIN);
        createUser("Administrador", "admin@ai901.local", PASSWORD, RoleName.ADMIN);
        String token = login("admin@ai901.local", PASSWORD);

        String deactivated = deleteWithToken("/user/" + userId, token);
        assertThat(JsonPath.parse(deactivated).read("$.data.active", Boolean.class)).isFalse();

        String list = getJsonWithToken("/user", token);
        assertThat(JsonPath.parse(list).read("$.data.total", Integer.class)).isEqualTo(1);
        assertThat(list).doesNotContain("ana@ai901.local");

        String listWithInactive = getJsonWithToken("/user?includeInactive=true", token);
        assertThat(JsonPath.parse(listWithInactive).read("$.data.total", Integer.class)).isEqualTo(2);
    }

    @Test
    void unUsuarioDesactivadoNoPuedeIniciarSesion() throws Exception {
        UUID userId = createUser("Ana", "ana@ai901.local", PASSWORD, RoleName.ADMIN);
        createUser("Administrador", "admin@ai901.local", PASSWORD, RoleName.ADMIN);
        String token = login("admin@ai901.local", PASSWORD);

        deleteWithToken("/user/" + userId, token);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ana@ai901.local\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void reactivaElUsuarioDesactivado() throws Exception {
        UUID userId = createUser("Ana", "ana@ai901.local", PASSWORD, RoleName.ADMIN);
        createUser("Administrador", "admin@ai901.local", PASSWORD, RoleName.ADMIN);
        String token = login("admin@ai901.local", PASSWORD);

        deleteWithToken("/user/" + userId, token);
        String activated = putJsonWithToken("/user/" + userId + "/activate", "", token);

        assertThat(JsonPath.parse(activated).read("$.data.active", Boolean.class)).isTrue();
        login("ana@ai901.local", PASSWORD);
    }

    @Test
    void noPermiteCrearUsuariosConEmailDuplicado() throws Exception {
        UUID roleId = createRole(RoleName.ADMIN);
        createUser("Ana", "ana@ai901.local", PASSWORD, RoleName.ADMIN);
        createUser("Administrador", "admin@ai901.local", PASSWORD, RoleName.ADMIN);
        String token = login("admin@ai901.local", PASSWORD);

        mockMvc.perform(post("/user")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Otra\",\"email\":\"ana@ai901.local\",\"password\":\"secreto123\","
                                + "\"roleId\":\"" + roleId + "\"}"))
                .andExpect(status().isConflict());
    }
}
