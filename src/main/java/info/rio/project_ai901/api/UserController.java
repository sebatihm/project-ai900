package info.rio.project_ai901.api;

import info.rio.project_ai901.api.dto.ApiResponse;
import info.rio.project_ai901.api.dto.ChangePasswordRequest;
import info.rio.project_ai901.api.dto.CreateUserRequest;
import info.rio.project_ai901.api.dto.PaginatedResponse;
import info.rio.project_ai901.api.dto.UpdateUserRequest;
import info.rio.project_ai901.api.dto.UserDetailResponse;
import info.rio.project_ai901.api.dto.UserSummaryResponse;
import info.rio.project_ai901.services.user.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Validated
@RestController
@RequestMapping("/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PaginatedResponse<UserSummaryResponse>>> list(
            @RequestParam(value = "roleId", required = false) UUID roleId,
            @RequestParam(value = "branchId", required = false) UUID branchId,
            @RequestParam(value = "warehouseId", required = false) UUID warehouseId,
            @RequestParam(value = "includeInactive", defaultValue = "false") boolean includeInactive,
            @RequestParam(value = "offset", defaultValue = "0") long offset,
            @RequestParam(value = "limit", defaultValue = "20") int limit) {
        PaginatedResponse<UserSummaryResponse> result = userService.list(roleId, branchId, warehouseId,
                includeInactive, offset, limit);
        return ResponseEntity.ok(ApiResponse.ok(result, "Listado de usuarios obtenido correctamente"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDetailResponse>> detail(@PathVariable("id") UUID id) {
        UserDetailResponse result = userService.detail(id);
        return ResponseEntity.ok(ApiResponse.ok(result, "Detalle de usuario obtenido correctamente"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserDetailResponse>> create(@Valid @RequestBody CreateUserRequest request) {
        UserDetailResponse result = userService.create(request);
        return ResponseEntity.status(201).body(ApiResponse.created(result, "Usuario creado correctamente"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDetailResponse>> update(@PathVariable("id") UUID id,
            @Valid @RequestBody UpdateUserRequest request) {
        UserDetailResponse result = userService.update(id, request);
        return ResponseEntity.ok(ApiResponse.ok(result, "Usuario actualizado correctamente"));
    }

    @PutMapping("/{id}/password")
    public ResponseEntity<ApiResponse<UserDetailResponse>> changePassword(@PathVariable("id") UUID id,
            @Valid @RequestBody ChangePasswordRequest request) {
        UserDetailResponse result = userService.changePassword(id, request);
        return ResponseEntity.ok(ApiResponse.ok(result, "Contrasena actualizada correctamente"));
    }

    @PutMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<UserDetailResponse>> activate(@PathVariable("id") UUID id) {
        UserDetailResponse result = userService.activate(id);
        return ResponseEntity.ok(ApiResponse.ok(result, "Usuario activado correctamente"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDetailResponse>> deactivate(@PathVariable("id") UUID id) {
        UserDetailResponse result = userService.deactivate(id);
        return ResponseEntity.ok(ApiResponse.ok(result, "Usuario desactivado correctamente"));
    }
}
