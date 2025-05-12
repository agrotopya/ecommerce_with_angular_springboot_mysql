package com.fibiyo.ecommerce.infrastructure.persistence.specification;

import com.fibiyo.ecommerce.domain.entity.Coupon;
import jakarta.persistence.criteria.Predicate; // Predicate için
import org.springframework.data.jpa.domain.Specification;

public class CouponSpecifications {

    /**
     * Aktiflik durumuna göre filtreler.
     * @param isActive Filtre değeri (true, false veya null - null ise filtre uygulanmaz)
     * @return Specification nesnesi
     */
    public static Specification<Coupon> isActive(Boolean isActive) {
        return (root, query, criteriaBuilder) -> {
            if (isActive == null) {
                return criteriaBuilder.conjunction(); // Filtre yoksa her zaman true dön
            }
            return criteriaBuilder.equal(root.get("isActive"), isActive);
        };
    }

    // İleride eklenebilecek diğer filtreler için örnekler:
    /*
    public static Specification<Coupon> codeContains(String codeFragment) {
        return (root, query, criteriaBuilder) -> {
             if (codeFragment == null || codeFragment.isBlank()) {
                 return criteriaBuilder.conjunction();
             }
            return criteriaBuilder.like(criteriaBuilder.lower(root.get("code")), "%" + codeFragment.toLowerCase() + "%");
        };
    }

     public static Specification<Coupon> expiresAfter(java.time.LocalDateTime dateTime) {
        return (root, query, criteriaBuilder) ->
            criteriaBuilder.greaterThanOrEqualTo(root.get("expiryDate"), dateTime);
    }
    */

}