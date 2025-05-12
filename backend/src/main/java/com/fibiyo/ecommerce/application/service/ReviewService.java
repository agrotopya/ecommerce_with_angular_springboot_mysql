package com.fibiyo.ecommerce.application.service;

import com.fibiyo.ecommerce.application.dto.ReviewRequest;
import com.fibiyo.ecommerce.application.dto.ReviewResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReviewService {

    // --- Customer Operations ---
    ReviewResponse addReview(Long productId, ReviewRequest reviewRequest);
    // ReviewResponse updateMyReview(Long reviewId, ReviewRequest reviewRequest); // Opsiyonel: Güncelleme olacaksa
    void deleteMyReview(Long reviewId); // Kendi yorumunu silme

    // --- Public Operations ---
    Page<ReviewResponse> findReviewsByProduct(Long productId, Pageable pageable, boolean approvedOnly); // Onaylı/Tümünü getirme seçeneği

    // --- Admin Operations ---
    Page<ReviewResponse> findAllReviews(Pageable pageable, Boolean isApproved); // Tüm yorumları admin için listeleme
    ReviewResponse approveReview(Long reviewId);
    ReviewResponse rejectReview(Long reviewId); // Veya sadece isApproved = false yapabilir
    void deleteReviewByAdmin(Long reviewId); // Adminin herhangi bir yorumu silmesi
}