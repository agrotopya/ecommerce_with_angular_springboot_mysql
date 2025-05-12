package com.fibiyo.ecommerce.infrastructure.persistence.repository;

import com.fibiyo.ecommerce.domain.entity.Order;
import com.fibiyo.ecommerce.domain.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor; // Sipariş filtreleme için
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> { // Specification Executor eklendi

    // Müşterinin siparişlerini tarihe göre tersten sıralı ve sayfalı getirme
    Page<Order> findByCustomerIdOrderByOrderDateDesc(Long customerId, Pageable pageable);

    // Müşterinin belirli durumdaki siparişleri
    Page<Order> findByCustomerIdAndStatus(Long customerId, OrderStatus status, Pageable pageable);

    // Belirli bir durumdaki tüm siparişler (Admin paneli için)
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    // TODO: Belirli bir satıcının ürünlerini içeren siparişleri bulmak için daha karmaşık bir sorgu (JPQL veya Specification) gerekebilir.
    // Örnek JPQL (performansı test edilmeli):
    // @Query("SELECT DISTINCT o FROM Order o JOIN o.orderItems oi JOIN oi.product p WHERE p.seller.id = :sellerId")
    // Page<Order> findOrdersBySellerProduct(@Param("sellerId") Long sellerId, Pageable pageable);

    Optional<Order> findFirstByCustomerIdAndOrderItems_Product_IdAndStatusOrderByOrderDateDesc(
        Long customerId, Long productId, OrderStatus status
    );
    
        
}