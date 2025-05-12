
package com.fibiyo.ecommerce.application.service;

import com.fibiyo.ecommerce.application.dto.AddToCartRequest;
import com.fibiyo.ecommerce.application.dto.CartResponse;
import com.fibiyo.ecommerce.application.dto.UpdateCartItemRequest;
import com.fibiyo.ecommerce.domain.entity.Cart; // Cart entity import edildi
import com.fibiyo.ecommerce.domain.entity.User; // User entity import edildi

/**
 * Alışveriş sepeti işlemleriyle ilgili servis arayüzü.
 */
public interface CartService {

    /**
     * Mevcut giriş yapmış kullanıcının sepet bilgilerini DTO olarak getirir.
     * Eğer sepet yoksa, oluşturur ve boş sepet döndürür.
     *
     * @return Kullanıcının sepetinin DTO temsili.
     */
    CartResponse getCartForCurrentUser();

    /**
     * Mevcut giriş yapmış kullanıcının Cart entity'sini getirir veya oluşturur.
     * Bu metod genellikle diğer servisler (örn: OrderService) tarafından kullanılır.
     * Sepet ve ürün bilgilerini (items.product) EAGER fetch ile yükler.
     *
     * @param user Cart'ı alınacak/oluşturulacak kullanıcı.
     * @return Kullanıcının Cart entity'si.
     */

    //  Cart getOrCreateCartForUserInternal(); // internal kullanım
    //  Cart getOrCreateCartForUser(User user);


    Cart getOrCreateCartEntityForUser(User user); // <- YENİ EKLENDİ (OrderService için)

    /**
     * Kullanıcının sepetine yeni bir ürün ekler veya mevcut ürünün miktarını artırır.
     *
     * @param request Eklenecek ürün ID'sini ve miktarını içeren istek.
     * @return Güncellenmiş sepetin DTO temsili.
     * @throws ResourceNotFoundException Ürün bulunamazsa.
     * @throws BadRequestException Stok yetersizse veya ürün satın alınamaz durumdaysa.
     */
    CartResponse addItemToCart(AddToCartRequest request);

    /**
     * Sepetteki bir ürünün miktarını günceller.
     *
     * @param productId Güncellenecek ürünün ID'si.
     * @param request Yeni miktarı içeren istek.
     * @return Güncellenmiş sepetin DTO temsili.
     * @throws ResourceNotFoundException Ürün sepette bulunamazsa.
     * @throws BadRequestException Stok yetersizse.
     */
    CartResponse updateCartItemQuantity(Long productId, UpdateCartItemRequest request);

    /**
     * Belirli bir ürünü kullanıcının sepetinden tamamen kaldırır.
     *
     * @param productId Sepetten kaldırılacak ürünün ID'si.
     * @return Güncellenmiş sepetin DTO temsili.
     * @throws ResourceNotFoundException Ürün sepette bulunamazsa.
     */
    CartResponse removeItemFromCart(Long productId);

    /**
     * Mevcut giriş yapmış kullanıcının sepetindeki tüm ürünleri kaldırır.
     */
    void clearCart();
}