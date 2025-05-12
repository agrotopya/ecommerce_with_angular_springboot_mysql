package com.fibiyo.ecommerce.infrastructure.persistence.repository;

import com.fibiyo.ecommerce.domain.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query; // JOIN FETCH için
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    // Kullanıcı ID'sine göre sepeti bul (Sepet kalemleri ile birlikte yükle - Optimizasyon)
    @Query("SELECT c FROM Cart c LEFT JOIN FETCH c.items ci LEFT JOIN FETCH ci.product p LEFT JOIN FETCH p.category WHERE c.user.id = :userId")
    Optional<Cart> findByUserIdWithItems(Long userId);

    // Basitçe kullanıcı ID'sine göre bul (item'ları LAZY yükler)
    Optional<Cart> findByUserId(Long userId);

    boolean existsByUserId(Long userId);
}