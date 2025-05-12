package com.fibiyo.ecommerce.infrastructure.persistence.repository;

import com.fibiyo.ecommerce.domain.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying; // Update/Delete sorguları için
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional; // @Modifying ile @Transactional gerekli

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // Kullanıcının bildirimlerini tarihe göre tersten sayfalı getir
    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    // Kullanıcının okunmamış bildirimlerini getir
    List<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(Long userId);

    // Kullanıcının okunmamış bildirim sayısı
    long countByUserIdAndIsReadFalse(Long userId);

    // Belirli bir kullanıcının tüm bildirimlerini okundu olarak işaretle
    @Transactional // Yazma işlemi olduğu için Transactional olmalı
    @Modifying // Veriyi değiştiren bir sorgu olduğunu belirtir
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user.id = :userId AND n.isRead = false")
    int markAllAsReadByUserId(@Param("userId") Long userId);
}