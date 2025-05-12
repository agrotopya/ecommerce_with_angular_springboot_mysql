package com.fibiyo.ecommerce.infrastructure.persistence.repository;

import com.fibiyo.ecommerce.domain.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> ,JpaSpecificationExecutor<Coupon>{


    
    Optional<Coupon> findByCode(String code);

    // Aktif ve süresi dolmamış kuponları bulma
    List<Coupon> findByIsActiveTrueAndExpiryDateAfter(LocalDateTime now);

    // Belirli bir kodla aktif ve geçerli kuponu bulma (sepete uygulama için)
    Optional<Coupon> findByCodeAndIsActiveTrueAndExpiryDateAfter(String code, LocalDateTime now);

    Boolean existsByCode(String code);
}