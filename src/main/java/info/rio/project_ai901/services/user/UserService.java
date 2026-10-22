package info.rio.project_ai901.services.user;

import info.rio.project_ai901.api.dto.CreateUserRequest;
import info.rio.project_ai901.api.dto.ChangePasswordRequest;
import info.rio.project_ai901.api.dto.PaginatedResponse;
import info.rio.project_ai901.api.dto.UpdateUserRequest;
import info.rio.project_ai901.api.dto.UserDetailResponse;
import info.rio.project_ai901.api.dto.UserSummaryResponse;
import info.rio.project_ai901.exceptions.DuplicateResourceException;
import info.rio.project_ai901.exceptions.InvalidRequestException;
import info.rio.project_ai901.exceptions.ResourceInUseException;
import info.rio.project_ai901.exceptions.ResourceNotFoundException;
import info.rio.project_ai901.models.Branch;
import info.rio.project_ai901.models.Role;
import info.rio.project_ai901.models.User;
import info.rio.project_ai901.models.Warehouse;
import info.rio.project_ai901.models.enums.InvoiceStatus;
import info.rio.project_ai901.models.enums.RoleName;
import info.rio.project_ai901.repositories.BranchRepository;
import info.rio.project_ai901.repositories.InvoiceRepository;
import info.rio.project_ai901.repositories.RoleRepository;
import info.rio.project_ai901.repositories.UserRepository;
import info.rio.project_ai901.repositories.WarehouseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    private static final List<InvoiceStatus> PENDING_INVOICE_STATUSES = List.of(InvoiceStatus.VALID,
            InvoiceStatus.SUSPICIOUS);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final BranchRepository branchRepository;
    private final WarehouseRepository warehouseRepository;
    private final InvoiceRepository invoiceRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper mapper;

    public UserService(UserRepository userRepository, RoleRepository roleRepository,
            BranchRepository branchRepository, WarehouseRepository warehouseRepository,
            InvoiceRepository invoiceRepository, PasswordEncoder passwordEncoder, UserMapper mapper) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.branchRepository = branchRepository;
        this.warehouseRepository = warehouseRepository;
        this.invoiceRepository = invoiceRepository;
        this.passwordEncoder = passwordEncoder;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<UserSummaryResponse> list(UUID roleId, UUID branchId, UUID warehouseId,
            boolean includeInactive, long offset, int limit) {
        int size = clampLimit(limit);
        long safeOffset = Math.max(offset, 0);
        PageRequest pageable = PageRequest.of((int) (safeOffset / size), size, Sort.by("name").ascending());
        Page<User> pageResult = userRepository.search(roleId, branchId, warehouseId, includeInactive, pageable);
        List<UserSummaryResponse> items = pageResult.getContent().stream()
                .map(mapper::toSummary)
                .toList();
        return new PaginatedResponse<>(items, pageResult.getTotalElements(), safeOffset, size);
    }

    @Transactional(readOnly = true)
    public UserDetailResponse detail(UUID id) {
        return mapper.toDetail(find(id));
    }

    @Transactional
    public UserDetailResponse create(CreateUserRequest request) {
        String email = request.email().trim();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("Ya existe un usuario con el email: " + email);
        }
        Role role = resolveRole(request.roleId());
        Branch branch = resolveBranch(request.branchId());
        Warehouse warehouse = resolveWarehouse(request.warehouseId());
        validateScope(role, branch, warehouse);

        LocalDateTime now = LocalDateTime.now();
        User user = new User();
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(role);
        user.setBranch(branch);
        user.setWarehouse(warehouse);
        user.setActive(true);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        userRepository.save(user);
        return mapper.toDetail(user);
    }

    @Transactional
    public UserDetailResponse update(UUID id, UpdateUserRequest request) {
        User user = find(id);
        String email = request.email().trim();
        if (userRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new DuplicateResourceException("Ya existe un usuario con el email: " + email);
        }
        Role role = resolveRole(request.roleId());
        Branch branch = resolveBranch(request.branchId());
        Warehouse warehouse = resolveWarehouse(request.warehouseId());
        validateScope(role, branch, warehouse);

        user.setName(request.name().trim());
        user.setEmail(email);
        user.setRole(role);
        user.setBranch(branch);
        user.setWarehouse(warehouse);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        return mapper.toDetail(user);
    }

    @Transactional
    public UserDetailResponse changePassword(UUID id, ChangePasswordRequest request) {
        User user = find(id);
        if (user.getPassword() == null || !passwordEncoder.matches(request.oldPassword(), user.getPassword())) {
            throw new InvalidRequestException("La contrasena actual no coincide");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        return mapper.toDetail(user);
    }

    @Transactional
    public UserDetailResponse deactivate(UUID id) {
        User user = find(id);
        if (!user.isActive()) {
            throw new InvalidRequestException("El usuario ya se encuentra inactivo: " + id);
        }
        long pending = invoiceRepository.countByLoadedBy_IdAndStatusIn(id, PENDING_INVOICE_STATUSES)
                + invoiceRepository.countByReviewedBy_IdAndStatusIn(id, PENDING_INVOICE_STATUSES);
        if (pending > 0) {
            throw new ResourceInUseException(
                    "No se puede desactivar el usuario porque tiene facturas pendientes de revision");
        }
        user.setActive(false);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        return mapper.toDetail(user);
    }

    @Transactional
    public UserDetailResponse activate(UUID id) {
        User user = find(id);
        if (user.isActive()) {
            throw new InvalidRequestException("El usuario ya se encuentra activo: " + id);
        }
        user.setActive(true);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        return mapper.toDetail(user);
    }

    private User find(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + id));
    }

    private Role resolveRole(UUID roleId) {
        return roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado: " + roleId));
    }

    private Branch resolveBranch(UUID branchId) {
        if (branchId == null) {
            return null;
        }
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada: " + branchId));
        if (!branch.isActive()) {
            throw new InvalidRequestException("La sucursal no esta activa: " + branchId);
        }
        return branch;
    }

    private Warehouse resolveWarehouse(UUID warehouseId) {
        if (warehouseId == null) {
            return null;
        }
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("Almacen no encontrado: " + warehouseId));
        if (!warehouse.isActive()) {
            throw new InvalidRequestException("El almacen no esta activo: " + warehouseId);
        }
        return warehouse;
    }

    private void validateScope(Role role, Branch branch, Warehouse warehouse) {
        if (role.getName() == RoleName.BRANCH_MANAGER && branch == null) {
            throw new InvalidRequestException("El rol BRANCH_MANAGER requiere una sucursal asignada");
        }
        if (role.getName() == RoleName.WAREHOUSE_HEAD && warehouse == null) {
            throw new InvalidRequestException("El rol WAREHOUSE_HEAD requiere un almacen asignado");
        }
    }

    private int clampLimit(int limit) {
        if (limit <= 0) {
            return 20;
        }
        return Math.min(limit, 100);
    }
}
