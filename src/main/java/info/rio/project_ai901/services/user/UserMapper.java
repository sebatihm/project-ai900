package info.rio.project_ai901.services.user;

import info.rio.project_ai901.api.dto.UserDetailResponse;
import info.rio.project_ai901.api.dto.UserSummaryResponse;
import info.rio.project_ai901.models.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserSummaryResponse toSummary(User user) {
        return new UserSummaryResponse(user.getId(), user.getName(), user.getEmail(), user.getRole().getName(),
                user.getBranch() == null ? null : user.getBranch().getId(),
                user.getBranch() == null ? null : user.getBranch().getName(),
                user.getWarehouse() == null ? null : user.getWarehouse().getId(),
                user.getWarehouse() == null ? null : user.getWarehouse().getName());
    }

    public UserDetailResponse toDetail(User user) {
        return new UserDetailResponse(user.getId(), user.getName(), user.getEmail(), user.getRole().getName(),
                user.getBranch() == null ? null : user.getBranch().getId(),
                user.getBranch() == null ? null : user.getBranch().getName(),
                user.getWarehouse() == null ? null : user.getWarehouse().getId(),
                user.getWarehouse() == null ? null : user.getWarehouse().getName(),
                user.isActive(), user.getCreatedAt(), user.getUpdatedAt());
    }
}
