package info.rio.project_ai901.services.category;

import info.rio.project_ai901.api.dto.CategoryDetailResponse;
import info.rio.project_ai901.api.dto.CategorySummaryResponse;
import info.rio.project_ai901.models.Category;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {

    public CategorySummaryResponse toSummary(Category category) {
        return new CategorySummaryResponse(category.getId(), category.getName());
    }

    public CategoryDetailResponse toDetail(Category category) {
        return new CategoryDetailResponse(category.getId(), category.getName());
    }
}
