package com.fibiyo.ecommerce.infrastructure.web.controller;

import com.fibiyo.ecommerce.application.dto.*; // DTOs
import com.fibiyo.ecommerce.application.service.CartService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart") // Sepet endpoint'leri
// @PreAuthorize("isAuthenticated()") // Tüm sepet işlemleri login gerektirir
public class CartController {

    private static final Logger logger = LoggerFactory.getLogger(CartController.class);

    private final CartService cartService;

    @Autowired
    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    // Mevcut kullanıcının sepetini getir
    @GetMapping
    public ResponseEntity<CartResponse> getMyCart() {
        logger.info("GET /api/cart requested");
        CartResponse cart = cartService.getCartForCurrentUser();
        return ResponseEntity.ok(cart);
    }

    // Sepete ürün ekle
    @PostMapping("/items")
    public ResponseEntity<CartResponse> addItemToCart(@Valid @RequestBody AddToCartRequest request) {
         logger.info("POST /api/cart/items requested for product ID: {}", request.getProductId());
         CartResponse updatedCart = cartService.addItemToCart(request);
         // Genellikle ekleme/güncelleme sonrası güncel sepet döner
        return ResponseEntity.ok(updatedCart);
    }

    // Sepetteki ürünün miktarını güncelle
    @PutMapping("/items/{productId}")
    public ResponseEntity<CartResponse> updateCartItemQuantity(
            @PathVariable Long productId,
            @Valid @RequestBody UpdateCartItemRequest request) {
        logger.info("PUT /api/cart/items/{} requested with quantity: {}", productId, request.getQuantity());
        CartResponse updatedCart = cartService.updateCartItemQuantity(productId, request);
        return ResponseEntity.ok(updatedCart);
    }


    // Sepetten ürünü kaldır
    @DeleteMapping("/items/{productId}")
    public ResponseEntity<CartResponse> removeItemFromCart(@PathVariable Long productId) {
        logger.warn("DELETE /api/cart/items/{} requested", productId);
         CartResponse updatedCart = cartService.removeItemFromCart(productId);
        return ResponseEntity.ok(updatedCart); // Silme sonrası güncel sepeti dön
    }

    // Sepeti tamamen temizle
    @DeleteMapping
    public ResponseEntity<ApiResponse> clearMyCart() {
         logger.warn("DELETE /api/cart requested (clear cart)");
         cartService.clearCart();
        // Başarılı olursa basit bir onay mesajı dönebiliriz
         return ResponseEntity.ok(new ApiResponse(true, "Sepet başarıyla temizlendi."));
         // Veya ResponseEntity.noContent().build();
    }

}