package com.fibiyo.ecommerce.infrastructure.persistence.repository;

import com.fibiyo.ecommerce.application.dto.MonthlySalesDto;
import com.fibiyo.ecommerce.domain.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    // Belirli bir siparişe ait tüm kalemler
    List<OrderItem> findByOrderId(Long orderId);

    // Belirli bir ürünü içeren tüm sipariş kalemleri (Ürün satış analizi için)
    List<OrderItem> findByProductId(Long productId);

    // Belirli bir satıcıya ait aylık satış özetini getirir
    // Not: DATE_FORMAT MySQL'e özgüdür. Veritabanı bağımsızlığı için Java tarafında formatlama daha iyi olabilir.
    // Ancak, gruplama için veritabanı fonksiyonu kullanmak genellikle daha performanslıdır.
    // OrderItem'ın product alanının sellerId'ye sahip olduğunu varsayıyoruz.
    // OrderItem'ın order alanının orderDate (veya createdAt) alanına sahip olduğunu varsayıyoruz.
    @Query("SELECT FUNCTION('DATE_FORMAT', oi.order.orderDate, '%Y-%m') as monthYear, SUM(oi.priceAtPurchase * oi.quantity) as totalSales " +
           "FROM OrderItem oi WHERE oi.product.seller.id = :sellerId " +
           "GROUP BY monthYear " + // Alias ile gruplama
           "ORDER BY monthYear ASC")
    List<Object[]> findMonthlySalesDataBySellerId(@Param("sellerId") Long sellerId); // Metod adı ve dönüş tipi değişti
}
