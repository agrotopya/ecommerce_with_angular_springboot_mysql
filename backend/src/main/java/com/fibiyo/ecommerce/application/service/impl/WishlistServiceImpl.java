package com.fibiyo.ecommerce.application.service.impl;

import com.fibiyo.ecommerce.application.dto.ProductResponse;
import com.fibiyo.ecommerce.application.dto.WishlistRequest;
import com.fibiyo.ecommerce.application.exception.BadRequestException;
import com.fibiyo.ecommerce.application.exception.ForbiddenException;
import com.fibiyo.ecommerce.application.exception.ResourceNotFoundException;
import com.fibiyo.ecommerce.application.mapper.ProductMapper; // Product mapper'ı kullanalım
import com.fibiyo.ecommerce.application.service.WishlistService;
import com.fibiyo.ecommerce.domain.entity.Product;
import com.fibiyo.ecommerce.domain.entity.User;
import com.fibiyo.ecommerce.domain.entity.WishlistItem;
import com.fibiyo.ecommerce.infrastructure.persistence.repository.ProductRepository;
import com.fibiyo.ecommerce.infrastructure.persistence.repository.UserRepository;
import com.fibiyo.ecommerce.infrastructure.persistence.repository.WishlistItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl; // Page implementasyonu için
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Yazma işlemleri Transactional olmalı

import java.util.List;
import java.util.stream.Collectors;


@Service
public class WishlistServiceImpl implements WishlistService {

    private static final Logger logger = LoggerFactory.getLogger(WishlistServiceImpl.class);

    private final WishlistItemRepository wishlistItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ProductMapper productMapper; // Direkt ProductResponse dönmek için

     // Helper Methods (getCurrentUser etc.)
      private User getCurrentUser() {
          Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
         if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
             throw new ForbiddenException("Bu işlem için giriş yapmalısınız."); // İstek listesi için login gerekli
          }
         String username = authentication.getName();
         return userRepository.findByUsername(username)
                  .orElseThrow(() -> new ResourceNotFoundException("Current user not found: " + username));
      }


    @Autowired
    public WishlistServiceImpl(WishlistItemRepository wishlistItemRepository, ProductRepository productRepository, UserRepository userRepository, ProductMapper productMapper) {
        this.wishlistItemRepository = wishlistItemRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.productMapper = productMapper;
    }

    @Override
    @Transactional // Veritabanına yazma işlemi
    public ProductResponse addProductToWishlist(WishlistRequest request) {
        User currentUser = getCurrentUser();
        Long productId = request.getProductId();
         logger.info("User ID: {} attempting to add product ID: {} to wishlist", currentUser.getId(), productId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        // Ürün zaten istek listesinde mi?
        if (wishlistItemRepository.existsByUserIdAndProductId(currentUser.getId(), productId)) {
            logger.warn("Product ID: {} already exists in wishlist for User ID: {}", productId, currentUser.getId());
            throw new BadRequestException("Bu ürün zaten istek listenizde.");
        }

        WishlistItem newItem = new WishlistItem();
        newItem.setUser(currentUser);
        newItem.setProduct(product);
        // newItem.setAddedAt(); // @CreationTimestamp hallediyor

        wishlistItemRepository.save(newItem);
        logger.info("Product ID: {} added to wishlist for User ID: {}", productId, currentUser.getId());

         // Eklenen ürünün bilgisini döndür
         return productMapper.toProductResponse(product);
    }

    @Override
    @Transactional // Veritabanından silme işlemi
    public void removeProductFromWishlist(Long productId) {
         User currentUser = getCurrentUser();
        logger.warn("User ID: {} attempting to remove product ID: {} from wishlist", currentUser.getId(), productId);

        // Ürün istek listesinde var mı diye kontrol etmek şart değil, silme işlemi ID ile yapılır.
         // Eğer yoksa hata fırlatmak istersen:
         /*
         WishlistItem itemToRemove = wishlistItemRepository.findByUserIdAndProductId(currentUser.getId(), productId)
             .orElseThrow(() -> new ResourceNotFoundException("Product not found in wishlist"));
         wishlistItemRepository.delete(itemToRemove);
         */

         // Direkt silme metodu (varsa siler, yoksa etkilemez veya etkilenen satır sayısı döner)
        long deletedCount = wishlistItemRepository.deleteByUserIdAndProductId(currentUser.getId(), productId); // Bu metod repo'da tanımlı olmalı

         if (deletedCount > 0) {
            logger.info("Product ID: {} removed from wishlist for User ID: {}", productId, currentUser.getId());
        } else {
             logger.warn("Product ID: {} not found in wishlist for User ID: {} or already removed.", productId, currentUser.getId());
            // Opsiyonel: Ürün listede yoksa hata fırlatılabilir
            throw new ResourceNotFoundException("Ürün istek listenizde bulunamadı.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getMyWishlist(Pageable pageable) {
         User currentUser = getCurrentUser();
          logger.debug("Fetching wishlist for User ID: {}", currentUser.getId());
          Page<WishlistItem> wishlistPage = wishlistItemRepository.findByUserIdOrderByAddedAtDesc(currentUser.getId(), pageable);

         // WishlistItem listesini Product listesine çevir
          List<ProductResponse> productResponses = wishlistPage.getContent().stream()
                .map(WishlistItem::getProduct) // Her item'dan Product nesnesini al
                 .map(productMapper::toProductResponse) // Product'ı ProductResponse'a map'le
                .collect(Collectors.toList());

         // Yeni bir Page nesnesi oluşturarak dön
          return new PageImpl<>(productResponses, pageable, wishlistPage.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isProductInWishlist(Long productId) {
         User currentUser = getCurrentUser(); // Giriş yapmış olmalı
          logger.debug("Checking if product ID: {} is in wishlist for User ID: {}", productId, currentUser.getId());
        return wishlistItemRepository.existsByUserIdAndProductId(currentUser.getId(), productId);
    }
}