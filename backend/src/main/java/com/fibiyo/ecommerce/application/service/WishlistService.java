package com.fibiyo.ecommerce.application.service;

import com.fibiyo.ecommerce.application.dto.ProductResponse; // Dönen tip Product bilgisi olacak
import com.fibiyo.ecommerce.application.dto.WishlistRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface WishlistService {

    ProductResponse addProductToWishlist(WishlistRequest request);

    void removeProductFromWishlist(Long productId);

    Page<ProductResponse> getMyWishlist(Pageable pageable);

    boolean isProductInWishlist(Long productId); // Bir ürünün listede olup olmadığını kontrol et
}