package com.fibiyo.ecommerce.infrastructure.persistence.repository;

import com.fibiyo.ecommerce.domain.entity.Feel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeelRepository extends JpaRepository<Feel, Long>, JpaSpecificationExecutor<Feel> {

    Page<Feel> findByProductIdAndActiveTrue(Long productId, Pageable pageable);

    Page<Feel> findBySellerIdAndActiveTrue(Long sellerId, Pageable pageable);

    Page<Feel> findBySellerId(Long sellerId, Pageable pageable);

    Page<Feel> findAllByActiveTrue(Pageable pageable);

    boolean existsByProductId(Long productId); // EKLENDİ
}
