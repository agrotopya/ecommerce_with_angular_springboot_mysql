package com.fibiyo.ecommerce.infrastructure.web.controller;

import com.fibiyo.ecommerce.application.dto.ApiResponse;
import com.fibiyo.ecommerce.application.dto.ReviewRequest;
import com.fibiyo.ecommerce.application.dto.ReviewResponse;
import com.fibiyo.ecommerce.application.service.ReviewService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private static final Logger logger = LoggerFactory.getLogger(ReviewController.class);

    private final ReviewService reviewService;

    @Autowired
    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    // --- Public Endpoint ---

    // Belirli bir ürünün ONAYLI yorumlarını getir
    @GetMapping("/product/{productId}")
    public ResponseEntity<Page<ReviewResponse>> getProductReviews(
            @PathVariable Long productId,
            // Yorumları oluşturulma tarihine göre en yeniden eskiye sırala
            @PageableDefault(size = 5, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
             @RequestParam(defaultValue = "true") boolean approvedOnly) { 
            
                
                
                // Varsayılan olarak sadece onaylıları getir
        logger.info("GET /api/reviews/product/{} requested. ApprovedOnly: {}", productId, approvedOnly);
         // TODO: Admin rolü yoksa approvedOnly=true zorunlu kılınabilir.
         //Admin olmayan kullanıcıların approvedOnly=false ile tüm yorumları çekmemesi için:
         if (!SecurityContextHolder.getContext().getAuthentication().getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"))) {
            approvedOnly = true;
        }
        Page<ReviewResponse> reviews = reviewService.findReviewsByProduct(productId, pageable, approvedOnly);
        return ResponseEntity.ok(reviews);
    }

    // --- Customer Endpoints ---

    // Yeni yorum ekleme (CUSTOMER rolü gerekli)
    @PostMapping("/product/{productId}")
    public ResponseEntity<ReviewResponse> addReview(
            @PathVariable Long productId,
            @Valid @RequestBody ReviewRequest reviewRequest) {
        logger.info("POST /api/reviews/product/{} requested", productId);
        ReviewResponse createdReview = reviewService.addReview(productId, reviewRequest);
        return new ResponseEntity<>(createdReview, HttpStatus.CREATED);
    }

    // Kendi yorumunu silme (CUSTOMER rolü gerekli)
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<ApiResponse> deleteMyReview(@PathVariable Long reviewId) {
         logger.warn("DELETE /api/reviews/{} requested by customer", reviewId);
        reviewService.deleteMyReview(reviewId);
        return ResponseEntity.ok(new ApiResponse(true, "Yorum başarıyla silindi."));
    }

    // --- Admin Endpoints ---

    // Tüm yorumları listele (Admin)
     @GetMapping("/admin/all")
     @PreAuthorize("hasRole('ADMIN')")
     public ResponseEntity<Page<ReviewResponse>> getAllReviewsAdmin(
             @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
             @RequestParam(required = false) Boolean isApproved) {
          logger.info("GET /api/reviews/admin/all requested");
         Page<ReviewResponse> reviews = reviewService.findAllReviews(pageable, isApproved);
         return ResponseEntity.ok(reviews);
     }

    // Yorumu onayla (Admin)
    @PatchMapping("/admin/{reviewId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReviewResponse> approveReview(@PathVariable Long reviewId) {
        logger.info("PATCH /api/reviews/admin/{}/approve requested", reviewId);
        ReviewResponse review = reviewService.approveReview(reviewId);
        return ResponseEntity.ok(review);
    }

     // Yorumu reddet (onayını kaldır) (Admin)
    @PatchMapping("/admin/{reviewId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
     public ResponseEntity<ReviewResponse> rejectReview(@PathVariable Long reviewId) {
         logger.info("PATCH /api/reviews/admin/{}/reject requested", reviewId);
         ReviewResponse review = reviewService.rejectReview(reviewId);
         return ResponseEntity.ok(review);
    }


    // Yorumu sil (Admin) - Customer ile aynı URL'i kullanabilir veya ayırabiliriz.
    // Ayrım yetkilendirme ile yapılıyor zaten. Servis metodunu çağıralım.
     @DeleteMapping("/admin/{reviewId}")
     @PreAuthorize("hasRole('ADMIN')")
     public ResponseEntity<ApiResponse> deleteReviewAdmin(@PathVariable Long reviewId) {
         logger.warn("DELETE /api/reviews/admin/{} requested by admin", reviewId);
        reviewService.deleteReviewByAdmin(reviewId);
          return ResponseEntity.ok(new ApiResponse(true, "Yorum admin tarafından silindi."));
      }

}