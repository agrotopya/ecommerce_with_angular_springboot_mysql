package com.fibiyo.ecommerce.infrastructure.persistence.repository;

import com.fibiyo.ecommerce.domain.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor; // Yorum filtreleme (onay durumu vb.)
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long>, JpaSpecificationExecutor<Review> {

    Page<Review> findByProductId(Long productId, Pageable pageable);

    Page<Review> findByCustomerId(Long customerId, Pageable pageable);

    // Belirli bir ürün için onaylanmış yorumlar
    Page<Review> findByProductIdAndIsApprovedTrue(Long productId, Pageable pageable);

    Optional<Review> findByProductIdAndCustomerId(Long productId, Long customerId);

    boolean existsByProductIdAndCustomerId(Long productId, Long customerId);

    // Bir ürünün ortalama puanını hesaplamak için JPQL (Serviste de yapılabilir)
    @Query("SELECT COALESCE(AVG(r.rating), 0.0) FROM Review r WHERE r.product.id = :productId AND r.isApproved = true")
    BigDecimal calculateAverageRatingByProductId(@Param("productId") Long productId);

    // Bir ürünün toplam onaylı yorum sayısını hesaplamak için
    long countByProductIdAndIsApprovedTrue(Long productId);
}