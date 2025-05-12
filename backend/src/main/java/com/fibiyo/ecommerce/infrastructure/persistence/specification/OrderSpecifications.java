package com.fibiyo.ecommerce.infrastructure.persistence.specification;

import com.fibiyo.ecommerce.domain.entity.Order;
import com.fibiyo.ecommerce.domain.entity.OrderItem; // JOIN için
import com.fibiyo.ecommerce.domain.entity.Product; // JOIN için
import com.fibiyo.ecommerce.domain.entity.User; // JOIN için
import com.fibiyo.ecommerce.domain.enums.OrderStatus;
import jakarta.persistence.criteria.*; // Criteria API importları
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

public class OrderSpecifications {

    /**
     * Belirli bir müşteriye ait siparişleri filtreler.
     *
     * @param customerId Müşteri ID'si (null ise filtre uygulanmaz)
     * @return Specification<Order>
     */
    public static Specification<Order> hasCustomer(Long customerId) {
        return (root, query, criteriaBuilder) -> {
            if (customerId == null) {
                return criteriaBuilder.conjunction(); // Her zaman true (filtre yok)
            }
            // root (Order) üzerinden customer alanına (User) ve onun id'sine git
            return criteriaBuilder.equal(root.get("customer").get("id"), customerId);
        };
    }

    /**
     * Belirli bir sipariş durumuna sahip siparişleri filtreler.
     *
     * @param status Sipariş durumu (null ise filtre uygulanmaz)
     * @return Specification<Order>
     */
    public static Specification<Order> hasStatus(OrderStatus status) {
        return (root, query, criteriaBuilder) -> {
            if (status == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("status"), status);
        };
    }

    /**
     * Belirli bir tarih aralığındaki siparişleri filtreler.
     *
     * @param startDate Başlangıç tarihi (dahil, null ise filtre uygulanmaz)
     * @param endDate Bitiş tarihi (dahil, null ise filtre uygulanmaz)
     * @return Specification<Order>
     */
    public static Specification<Order> orderDateBetween(LocalDateTime startDate, LocalDateTime endDate) {
        return (root, query, criteriaBuilder) -> {
            Predicate predicate = criteriaBuilder.conjunction(); // Başlangıç Predicate'i
            if (startDate != null) {
                // orderDate >= startDate
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.greaterThanOrEqualTo(root.get("orderDate"), startDate));
            }
            if (endDate != null) {
                // orderDate <= endDate
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.lessThanOrEqualTo(root.get("orderDate"), endDate));
            }
            return predicate;
        };
    }


    /**
     * Belirli bir satıcının ürününü içeren siparişleri filtreler.
     * Bu sorgu JOIN gerektirir.
     *
     * @param sellerId Satıcı ID'si (null ise filtre uygulanmaz)
     * @return Specification<Order>
     */
    public static Specification<Order> hasSellerProduct(Long sellerId) {
        return (root, query, criteriaBuilder) -> {
            if (sellerId == null) {
                return criteriaBuilder.conjunction();
            }
            // Subquery veya JOIN kullanılabilir. JOIN genellikle daha performanslıdır.
            // Order -> orderItems (Join) -> product (Join) -> seller (Join) -> id
            // Dikkat: distinct() kullanmak gerekebilir, çünkü bir siparişte aynı satıcının birden fazla ürünü olabilir.
            query.distinct(true); // Sonuçları tekilleştir

            Join<Order, OrderItem> orderItems = root.join("orderItems", JoinType.INNER); // Order ile OrderItem'ı join et
            Join<OrderItem, Product> product = orderItems.join("product", JoinType.INNER); // OrderItem ile Product'ı join et
            Join<Product, User> seller = product.join("seller", JoinType.INNER); // Product ile User'ı (seller) join et

            // Seller ID'sine göre filtrele
            return criteriaBuilder.equal(seller.get("id"), sellerId);
        };
    }


    // İleride eklenebilecek diğer filtreler:
    // public static Specification<Order> paymentStatusIs(PaymentStatus status) { ... }
    // public static Specification<Order> totalPriceGreaterThan(BigDecimal amount) { ... }
    // public static Specification<Order> containsProduct(Long productId) { ... }

}