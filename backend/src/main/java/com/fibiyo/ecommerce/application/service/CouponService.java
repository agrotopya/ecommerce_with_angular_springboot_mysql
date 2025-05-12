package com.fibiyo.ecommerce.application.service;

import com.fibiyo.ecommerce.application.dto.CouponRequest;
import com.fibiyo.ecommerce.application.dto.CouponResponse;
import com.fibiyo.ecommerce.application.dto.CouponValidationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

/**
 * Kupon işlemleriyle ilgili servis arayüzü.
 */
public interface CouponService {

    // --- Admin Operations ---

    /**
     * Yeni bir kupon oluşturur. (Admin yetkisi gerektirir)
     *
     * @param couponRequest Oluşturulacak kupon bilgileri.
     * @return Oluşturulan kuponun detayları (geçerlilik durumuyla).
     * @throws BadRequestException Kupon kodu zaten varsa veya validasyon hatası oluşursa.
     * @throws ForbiddenException İşlemi yapan kullanıcı Admin değilse.
     */
    CouponResponse createCoupon(CouponRequest couponRequest);

    /**
     * Mevcut bir kuponu günceller. (Admin yetkisi gerektirir)
     * Kupon kodu genellikle değiştirilemez.
     *
     * @param couponId      Güncellenecek kuponun ID'si.
     * @param couponRequest Yeni kupon bilgileri.
     * @return Güncellenen kuponun detayları (geçerlilik durumuyla).
     * @throws ResourceNotFoundException Kupon bulunamazsa.
     * @throws ForbiddenException İşlemi yapan kullanıcı Admin değilse.
     * @throws BadRequestException Validasyon hatası oluşursa.
     */
    CouponResponse updateCoupon(Long couponId, CouponRequest couponRequest);

    /**
     * Bir kuponu siler (veya pasife çeker). (Admin yetkisi gerektirir)
     *
     * @param couponId Silinecek kuponun ID'si.
     * @throws ResourceNotFoundException Kupon bulunamazsa.
     * @throws ForbiddenException İşlemi yapan kullanıcı Admin değilse.
     */
    void deleteCoupon(Long couponId);

    /**
     * Tüm kuponları sayfalı ve filtrelenmiş olarak listeler. (Admin yetkisi gerektirir)
     *
     * @param pageable Sayfalama ve sıralama bilgileri.
     * @param isActive Aktiflik durumuna göre filtreleme (null ise filtre uygulanmaz).
     * @return Kuponların sayfalanmış listesi.
     * @throws ForbiddenException İşlemi yapan kullanıcı Admin değilse.
     */
    Page<CouponResponse> findAllCoupons(Pageable pageable, Boolean isActive);

    /**
     * Belirli bir kuponu ID'sine göre bulur. (Admin yetkisi gerektirir)
     *
     * @param couponId Bulunacak kuponun ID'si.
     * @return Kuponun detayları (geçerlilik durumuyla).
     * @throws ResourceNotFoundException Kupon bulunamazsa.
     * @throws ForbiddenException İşlemi yapan kullanıcı Admin değilse.
     */
    CouponResponse findCouponById(Long couponId);

    /**
     * Belirli bir kuponu koduna göre bulur. (Admin yetkisi gerektirir)
     *
     * @param code Bulunacak kuponun kodu (büyük/küçük harf duyarsız).
     * @return Kuponun detayları (geçerlilik durumuyla).
     * @throws ResourceNotFoundException Kupon bulunamazsa.
     * @throws ForbiddenException İşlemi yapan kullanıcı Admin değilse.
     */
    CouponResponse findCouponByCode(String code);


    // --- Customer/System Operations ---

    /**
     * Verilen kupon kodunun, belirtilen sepet toplamı için geçerli olup olmadığını kontrol eder.
     * Herkese açık olabilir veya login gerektirebilir.
     *
     * @param code      Doğrulanacak kupon kodu.
     * @param cartTotal Uygulanacak sepetin ara toplamı (indirimler hariç).
     * @return Doğrulama sonucunu ve olası indirim tutarını içeren DTO.
     */
    CouponValidationResponse validateCoupon(String code, BigDecimal cartTotal);

    // --- Internal Operations (OrderService tarafından kullanılabilir) ---
    // Bu metodlar public olmak zorunda değil, sadece aynı pakette veya
    // component scan ile bulunabilir olmalı. Şimdilik interface'de tutmuyoruz.

    /*
     * Verilen kodla ilişkili geçerli Coupon entity'sini getirir.
     * public Coupon getValidCouponEntityByCode(String code) throws BadRequestException;
     */

    /*
     * Bir kuponun kullanım sayısını güvenli bir şekilde artırır.
     * public void incrementCouponUsageCount(Coupon coupon);
     */

}