package com.fibiyo.ecommerce.application.service.impl;

import com.fibiyo.ecommerce.application.dto.*; // DTO importları
import com.fibiyo.ecommerce.application.exception.*; // Exceptions
import com.fibiyo.ecommerce.application.mapper.*; // Mappers
import com.fibiyo.ecommerce.application.service.CartService;
import com.fibiyo.ecommerce.domain.entity.*; // Entities
import com.fibiyo.ecommerce.infrastructure.persistence.repository.*; // Repositories

import jakarta.persistence.OptimisticLockException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication; // Auth import
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class CartServiceImpl implements CartService {

    private static final Logger logger = LoggerFactory.getLogger(CartServiceImpl.class);

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CartMapper cartMapper;
    // private final CartItemMapper cartItemMapper; // Direkt kullanılmıyor olabilir (CartMapper kullanıyor)

    @Autowired
    public CartServiceImpl(CartRepository cartRepository, CartItemRepository cartItemRepository, ProductRepository productRepository, UserRepository userRepository, CartMapper cartMapper) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.cartMapper = cartMapper;
        // this.cartItemMapper = cartItemMapper; // CartMapper içinden kullanıldığı için inject şart değil gibi
    }

    // Helper - Mevcut giriş yapmış kullanıcıyı getirir
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new ForbiddenException("Bu işlem için giriş yapmalısınız.");
        }
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found: " + username));
    }

    // Helper - Kullanıcının sepetini getir veya yoksa oluştur (Item ve Product ile EAGER yükler)
    // Interface'e eklendiği için Override ve public oldu
    @Override
    @Transactional // Yeni sepet oluşturma ihtimali var
    public Cart getOrCreateCartEntityForUser(User user) {
        return cartRepository.findByUserIdWithItems(user.getId()) // Optimize sorgu ile çekmeyi dene
                .orElseGet(() -> {
                    logger.info("No cart found for user ID: {}. Creating a new cart.", user.getId());
                    Cart newCart = new Cart();
                    newCart.setUser(user);
                    return cartRepository.save(newCart); // Yeni sepeti kaydet ve dön
                });
    }

    @Override
    @Transactional // Genellikle sadece okuma, oluşturma getOrCreateCartEntityForUser içinde
    public CartResponse getCartForCurrentUser() {
        User user = getCurrentUser();
        logger.debug("Fetching cart DTO for user ID: {}", user.getId());
        Cart cart = getOrCreateCartEntityForUser(user); // DB'den al veya oluştur
        return cartMapper.toCartResponse(cart); // DTO'ya çevir ve dön
    }

    @Override
    @Transactional
    public CartResponse addItemToCart(AddToCartRequest request) {
        User user = getCurrentUser();
        // getOrCreate... metodu zaten cartı ve içindekileri (items->product) yüklüyor
        Cart cart = getOrCreateCartEntityForUser(user);
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.getProductId()));
        int requestedQuantity = request.getQuantity();

        logger.info("User ID: {} adding product ID: {} (Qty: {}) to cart ID: {}",
                user.getId(), product.getId(), requestedQuantity, cart.getId());

        // Stok ve ürün durumu kontrolü
        if (!product.isActive() || !product.isApproved()) {
            throw new BadRequestException("Ürün '" + product.getName() + "' şu anda satın alınamaz.");
        }

        // Sepette bu ürün var mı kontrolü (Mevcut item listesi üzerinden daha verimli)
        Optional<CartItem> existingItemOpt = cart.getItems().stream()
                .filter(item -> item.getProduct() != null && item.getProduct().getId().equals(product.getId()))
                .findFirst();

                try {
                    if (existingItemOpt.isPresent()) {
                        CartItem existingItem = existingItemOpt.get();
                        int newQuantity = existingItem.getQuantity() + requestedQuantity;
                        if (product.getStock() < newQuantity) {
                            throw new BadRequestException("Stok yetersiz. Sepetinize en fazla " + (product.getStock() - existingItem.getQuantity()) + " adet daha ekleyebilirsiniz.");
                        }
                        existingItem.setQuantity(newQuantity);
                        cartItemRepository.save(existingItem);
                        productRepository.save(product); // Optimistic Lock kontrolü
                        logger.debug("Updated quantity for product ID: {} in cart ID: {} to {}", product.getId(), cart.getId(), newQuantity);
                    } else {
                        if (product.getStock() < requestedQuantity) {
                            throw new BadRequestException("Stok yetersiz. Bu üründen en fazla " + product.getStock() + " adet ekleyebilirsiniz.");
                        }
                        CartItem newItem = new CartItem();
                        newItem.setProduct(product);
                        newItem.setQuantity(requestedQuantity);
                        cart.addItem(newItem);
                        productRepository.save(product); // Optimistic Lock kontrolü
                        logger.debug("Adding new product ID: {} (Qty: {}) to cart ID: {}", product.getId(), requestedQuantity, cart.getId());
                    }
                } catch (OptimisticLockException e) {
                    logger.error("Stok çakışması: {}", e.getMessage());
                    throw new ConflictException("Stok durumu değişti, lütfen tekrar deneyin.");
                }





        // Sepeti updatedAt için save et (cascade ile yeni item da save edilir)
        Cart updatedCart = cartRepository.save(cart);

        // DTO'yu döndür
        return cartMapper.toCartResponse(updatedCart); // CartMapper total'leri hesaplar
    }







    @Override
    @Transactional
    public CartResponse updateCartItemQuantity(Long productId, UpdateCartItemRequest request) {
        User user = getCurrentUser();
        Cart cart = getOrCreateCartEntityForUser(user); // Cart'ı itemlarıyla yükle
        int newQuantity = request.getQuantity();
        logger.info("User ID: {} updating product ID: {} quantity to {} in cart ID: {}",
                user.getId(), productId, newQuantity, cart.getId());

        CartItem itemToUpdate = cart.getItems().stream()
                .filter(item -> item.getProduct() != null && item.getProduct().getId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Product with id " + productId + " not found in cart"));

        // Stok kontrolü (Product entity'sinin güncel hali gerekli olabilir)
        Product product = productRepository.findById(productId)
            .orElseThrow(()-> new ResourceNotFoundException("Product associated with cart item not found:" + productId)); // Veritabanından taze alalım

        if(product.getStock() < newQuantity){
            logger.warn("Insufficient stock to update quantity for product ID: {}. Cart ID: {}, Requested: {}, Stock: {}",
                      product.getId(), cart.getId(), newQuantity, product.getStock());
              throw new BadRequestException("Stok yetersiz. Bu üründen en fazla " + product.getStock() + " adet seçebilirsiniz.");
        }
        try {
            itemToUpdate.setQuantity(newQuantity);
            cartItemRepository.save(itemToUpdate);
            productRepository.save(product); // Versiyon kontrolü için
        } catch (OptimisticLockException e) {
            logger.error("Stok çakışması: {}", e.getMessage());
            throw new ConflictException("Stok durumu değişti, lütfen tekrar deneyin.");
        }



        logger.debug("Quantity updated for product ID: {} in cart ID: {}", productId, cart.getId());

        // Sepetin updatedAt'ini güncellemek için save etmeye GEREK YOK (Item güncellendi, Cart değişmedi)
        // Ama DTO döndürmek için güncel cart entity lazım, tekrar çekebiliriz veya mevcutu mapleyebiliriz.
        // Şimdilik mevcutu mapleyelim (updatedAt eski kalacak DTO'da).
        // Veya cart'ı tekrar çek: cart = getOrCreateCartEntityForUser(user);
        return cartMapper.toCartResponse(cart);
    }


    @Override
    @Transactional
    public CartResponse removeItemFromCart(Long productId) {
        User user = getCurrentUser();
        Cart cart = getOrCreateCartEntityForUser(user); // Yüklü Cart'ı al
        logger.warn("User ID: {} removing product ID: {} from cart ID: {}", user.getId(), productId, cart.getId());

        // Silinecek item'ı bul
        CartItem itemToRemove = cart.getItems().stream()
             .filter(item -> item.getProduct() != null && item.getProduct().getId().equals(productId))
             .findFirst()
             .orElseThrow(() -> new ResourceNotFoundException("Product with id " + productId + " not found in cart"));

        // CascadeType.ALL ve orphanRemoval=true olduğu için listeden çıkarmak DB'den de silmeli
        cart.removeItem(itemToRemove); // Bu item.setCart(null) yapar
        Cart updatedCart = cartRepository.save(cart); // Cart'ı save edince item silinir mi? Evet orphanRemoval ile.

        // Alternatif (Daha Garanti): Direkt CartItemRepository ile silmek
        // cartItemRepository.delete(itemToRemove);

        logger.info("Removed product ID: {} from cart ID: {}", productId, cart.getId());

        // Dönen yanıt için, save ettiğimiz cart yeterli
        return cartMapper.toCartResponse(updatedCart);
    }

    @Override
    @Transactional
    public void clearCart() {
        User user = getCurrentUser();
        Optional<Cart> cartOpt = cartRepository.findByUserId(user.getId()); // Itemları fetch etmeye gerek yok
        if(cartOpt.isPresent()){
            Cart cart = cartOpt.get();
            logger.warn("User ID: {} clearing cart ID: {}", user.getId(), cart.getId());

            // Yöntem 1: orphanRemoval'a güvenmek (Cart'tan Item listesini temizleyip Cart'ı save etmek)
            // cart.getItems().clear(); // Listeyi temizle
            // cartRepository.save(cart); // Değişikliği kaydet

            // Yöntem 2: Direkt CartItemRepository ile silmek (Daha net olabilir)
            cartItemRepository.deleteByCartId(cart.getId());

            logger.info("Cart ID: {} cleared.", cart.getId());
            // Sepetin kendisi silinmez, içi boşalır.
        } else {
             logger.info("User ID: {} tried to clear cart, but no cart found.", user.getId());
        }
    }
}