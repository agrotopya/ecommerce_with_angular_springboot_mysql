package com.fibiyo.ecommerce.infrastructure.persistence.specification;

import com.fibiyo.ecommerce.domain.entity.Product;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public final class ProductSpecifications {

    private ProductSpecifications() {}

    public static Specification<Product> isActive(boolean active) {
        return (root, query, cb) -> cb.equal(root.get("isActive"), active);
    }

    public static Specification<Product> isApproved(boolean approved) {
        return (root, query, cb) -> cb.equal(root.get("isApproved"), approved);
    }

    public static Specification<Product> hasCategory(Long categoryId) {
        return (root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId);
    }

    public static Specification<Product> hasSeller(Long sellerId) {
        return (root, query, cb) -> cb.equal(root.get("seller").get("id"), sellerId);
    }

    public static Specification<Product> nameOrDescriptionContains(String keyword) {
        if (!StringUtils.hasText(keyword)) return null;
        return (root, query, cb) -> {
            String like = "%" + keyword.toLowerCase() + "%";
            List<Predicate> preds = new ArrayList<>();
            preds.add(cb.like(cb.lower(root.get("name")), like));
            preds.add(cb.like(cb.lower(root.get("description")), like));
            return cb.or(preds.toArray(Predicate[]::new));
        };
    }
}
