package info.rio.project_ai901.api;

import info.rio.project_ai901.api.dto.ApiResponse;
import info.rio.project_ai901.api.dto.RoleResponse;
import info.rio.project_ai901.repositories.RoleRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/role")
public class RoleController {

    private final RoleRepository roleRepository;

    public RoleController(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<RoleResponse>>> list() {
        List<RoleResponse> roles = roleRepository.findAllByOrderByNameAsc().stream()
                .map(role -> new RoleResponse(role.getId(), role.getName()))
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(roles, "Listado de roles obtenido correctamente"));
    }
}
