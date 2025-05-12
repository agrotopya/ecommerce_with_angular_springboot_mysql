// ÖNEMLİ: ReviewSpecifications Sınıfı
 // infrastructure/persistence/specification altına
 package com.fibiyo.ecommerce.infrastructure.persistence.specification;

 import com.fibiyo.ecommerce.domain.entity.Review;
 import org.springframework.data.jpa.domain.Specification;
 
  public class ReviewSpecifications {
     public static Specification<Review> isApproved(boolean isApproved) {
          return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("isApproved"), isApproved);
     }
     // TODO: CustomerId, ProductId, Date Range gibi filtreler eklenebilir
     // public static Specification<Review> hasCustomer(Long customerId) { ... }
 }