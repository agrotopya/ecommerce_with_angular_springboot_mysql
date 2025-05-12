package com.fibiyo.ecommerce.infrastructure.web.controller;

import com.fibiyo.ecommerce.application.dto.ApiResponse;
import com.fibiyo.ecommerce.application.dto.ProductResponse;
import com.fibiyo.ecommerce.application.dto.WishlistRequest;
import com.fibiyo.ecommerce.application.service.WishlistService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize; // Login gerekli
import org.springframework.web.bind.annotation.*;

import java.util.Map; // isProductInWishlist için

@RestController
@RequestMapping("/api/wishlist") // /api/users/me/wishlist gibi de olabilirdi ama şimdilik ayrı tutalım
@PreAuthorize("hasRole('CUSTOMER') or hasRole('SELLER')") // İstek listesi login gerektirir
public class WishlistController {

    private static final Logger logger = LoggerFactory.getLogger(WishlistController.class);

    private final WishlistService wishlistService;

    @Autowired
    public WishlistController(WishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    // İstek listesini getir (Sayfalı)
    @GetMapping
    public ResponseEntity<Page<ProductResponse>> getMyWishlist(
            @PageableDefault(size = 12, sort = "addedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        logger.info("GET /api/wishlist requested");
        Page<ProductResponse> wishlist = wishlistService.getMyWishlist(pageable);
        return ResponseEntity.ok(wishlist);
    }

    // İstek listesine ürün ekle
    @PostMapping
    public ResponseEntity<ProductResponse> addProductToWishlist(@Valid @RequestBody WishlistRequest request) {
        logger.info("POST /api/wishlist requested for product ID: {}", request.getProductId());
        ProductResponse addedProductInfo = wishlistService.addProductToWishlist(request);
        // 201 Created yerine 200 OK dönmek de kabul edilebilir, eklenen ürün bilgisini dönsün.
        return ResponseEntity.ok(addedProductInfo); // veya HttpStatus.CREATED
    }

    // İstek listesinden ürün çıkar
    @DeleteMapping("/{productId}")
    public ResponseEntity<ApiResponse> removeProductFromWishlist(@PathVariable Long productId) {
        logger.warn("DELETE /api/wishlist/{} requested", productId);
        wishlistService.removeProductFromWishlist(productId);
        return ResponseEntity.ok(new ApiResponse(true, "Ürün istek listesinden kaldırıldı."));
        // Veya ResponseEntity.noContent().build();
    }

    // Belirli bir ürün istek listesinde mi? (Özellikle ürün detay sayfasında kalp ikonunu doldurmak için kullanışlı)
    @GetMapping("/check/{productId}")
     public ResponseEntity<Map<String, Boolean>> checkProductInWishlist(@PathVariable Long productId) {
          logger.debug("GET /api/wishlist/check/{} requested", productId);
          boolean isInWishlist = wishlistService.isProductInWishlist(productId);
         return ResponseEntity.ok(Map.of("isInWishlist", isInWishlist)); // Basit bir Map ile boolean değeri dön
     }

}