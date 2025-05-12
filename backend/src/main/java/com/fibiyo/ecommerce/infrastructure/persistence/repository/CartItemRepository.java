package com.fibiyo.ecommerce.infrastructure.persistence.repository;

import com.fibiyo.ecommerce.domain.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    // Belirli bir sepette belirli bir ürünü bul
    Optional<CartItem> findByCartIdAndProductId(Long cartId, Long productId);

    // Belirli bir sepetteki tüm kalemleri sil
    @Transactional
    @Modifying // Silme işlemi için gerekli
    void deleteByCartId(Long cartId);

    // Belirli bir sepetteki belirli bir ürünü sil
    @Transactional
    @Modifying
    long deleteByCartIdAndProductId(Long cartId, Long productId); // Kaç adet silindiğini döner

}