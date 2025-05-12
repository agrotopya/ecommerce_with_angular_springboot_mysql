package com.fibiyo.ecommerce.infrastructure.persistence.repository;

import com.fibiyo.ecommerce.domain.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    // Belirli bir siparişe ait tüm kalemler
    List<OrderItem> findByOrderId(Long orderId);

    // Belirli bir ürünü içeren tüm sipariş kalemleri (Ürün satış analizi için)
    List<OrderItem> findByProductId(Long productId);
}