package com.fibiyo.ecommerce.application.mapper;

import com.fibiyo.ecommerce.application.dto.ReviewRequest;
import com.fibiyo.ecommerce.application.dto.ReviewResponse;
import com.fibiyo.ecommerce.domain.entity.Review;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget; // Update için gerekebilir (eğer update olacaksa)
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ReviewMapper {

    // Review -> ReviewResponse
    @Mapping(source = "product.id", target = "productId")
    @Mapping(source = "product.name", target = "productName")
    @Mapping(source = "customer.id", target = "customerId")
    @Mapping(source = "customer.username", target = "customerUsername")
    ReviewResponse toReviewResponse(Review review);

    List<ReviewResponse> toReviewResponseList(List<Review> reviews);

    // ReviewRequest -> Review (Create için)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "approved", ignore = true) // Serviste atanacak
    @Mapping(target = "product", ignore = true)   // Serviste atanacak
    @Mapping(target = "customer", ignore = true)  // Serviste atanacak
    @Mapping(target = "order", ignore = true)     // Şimdilik yok
    Review toReview(ReviewRequest reviewRequest);

    // Eğer yorum güncelleme olacaksa:
    /*
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "isApproved", ignore = true)
    @Mapping(target = "product", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "order", ignore = true)
    void updateReviewFromRequest(ReviewRequest request, @MappingTarget Review review);
    */
}