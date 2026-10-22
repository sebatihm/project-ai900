package info.rio.project_ai901.services.product;

import info.rio.project_ai901.api.dto.ProductDetailResponse;
import info.rio.project_ai901.api.dto.ProductSummaryResponse;
import info.rio.project_ai901.models.Product;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ProductMapper {

    public ProductSummaryResponse toSummary(Product product) {
        String categoryName = product.getCategory() != null ? product.getCategory().getName() : null;
        UUID categoryId = product.getCategory() != null ? product.getCategory().getId() : null;
        return new ProductSummaryResponse(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getUnit(),
                product.getPrice(),
                categoryId,
                categoryName);
    }

    public ProductDetailResponse toDetail(Product product) {
        String categoryName = product.getCategory() != null ? product.getCategory().getName() : null;
        UUID categoryId = product.getCategory() != null ? product.getCategory().getId() : null;
        return new ProductDetailResponse(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getUnit(),
                product.getPrice(),
                categoryId,
                categoryName);
    }
}
