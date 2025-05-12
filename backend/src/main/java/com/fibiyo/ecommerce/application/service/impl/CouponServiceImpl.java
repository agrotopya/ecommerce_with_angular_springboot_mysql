package com.fibiyo.ecommerce.application.service.impl;

import com.fibiyo.ecommerce.application.dto.CouponRequest;
import com.fibiyo.ecommerce.application.dto.CouponResponse;
import com.fibiyo.ecommerce.application.dto.CouponValidationResponse;
import com.fibiyo.ecommerce.application.exception.BadRequestException;
import com.fibiyo.ecommerce.application.exception.ForbiddenException;
import com.fibiyo.ecommerce.application.exception.ResourceNotFoundException;
import com.fibiyo.ecommerce.application.mapper.CouponMapper;
import com.fibiyo.ecommerce.application.service.CouponService;
import com.fibiyo.ecommerce.domain.entity.Coupon;
import com.fibiyo.ecommerce.domain.enums.DiscountType;
import com.fibiyo.ecommerce.domain.enums.Role;
import com.fibiyo.ecommerce.domain.entity.User;
import com.fibiyo.ecommerce.infrastructure.persistence.repository.CouponRepository;
import com.fibiyo.ecommerce.infrastructure.persistence.repository.UserRepository;
import com.fibiyo.ecommerce.infrastructure.persistence.specification.CouponSpecifications; // Doğru import
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class CouponServiceImpl implements CouponService {

    private static final Logger logger = LoggerFactory.getLogger(CouponServiceImpl.class);

    private final CouponRepository couponRepository;
    private final CouponMapper couponMapper;
    private final UserRepository userRepository; // Admin kontrolü için

    @Autowired
    public CouponServiceImpl(CouponRepository couponRepository, CouponMapper couponMapper, UserRepository userRepository) {
        this.couponRepository = couponRepository;
        this.couponMapper = couponMapper;
        this.userRepository = userRepository;
    }

    // --- Helper Metotlar ---
    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        if ("anonymousUser".equals(username)) {
             // Bazı servislerde bu durum tolere edilebilir ama Admin işlemleri için edilemez.
            throw new ForbiddenException("Bu işlem için giriş yapmalısınız.");
        }
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Current authenticated user not found: " + username));
    }

    // private void checkAdminRole() {
    //     User currentUser = getCurrentUser(); // Helper metodu çağır
    //     if (currentUser.getRole() != Role.ADMIN) {
    //         logger.warn("User '{}' (Role: {}) attempted an admin-only coupon operation.", currentUser.getUsername(), currentUser.getRole());
    //         throw new ForbiddenException("Bu işlemi gerçekleştirmek için Admin yetkisine sahip olmalısınız.");
    //     }
    // }

    // Kuponu yanıta çevirirken geçerlilik durumunu hesapla
    private CouponResponse mapToResponseWithStatus(Coupon coupon) {
        if (coupon == null) return null;
        CouponResponse response = couponMapper.toCouponResponse(coupon);
        response.setExpired(coupon.isExpired());
        response.setUsageLimitReached(coupon.isUsageLimitReached());
        response.setValid(coupon.isValid());
        return response;
    }

    // --- Admin Operations Implementation ---

    @Override
    @Transactional
    public CouponResponse createCoupon(CouponRequest couponRequest) {
        // checkAdminRole();
        String requestCode = couponRequest.getCode().toUpperCase(); // Standardize et
        logger.info("ADMIN: Creating coupon with code: {}", requestCode);

        if (couponRepository.existsByCode(requestCode)) {
            logger.warn("ADMIN: Coupon code '{}' already exists.", requestCode);
            throw new BadRequestException("Bu kupon kodu zaten mevcut.");
        }

         // Yüzde kontrolü
         if(couponRequest.getDiscountType() == DiscountType.PERCENTAGE
                 && (couponRequest.getDiscountValue().compareTo(BigDecimal.ZERO) <= 0 || couponRequest.getDiscountValue().compareTo(BigDecimal.valueOf(100)) > 0)) {
               throw new BadRequestException("Yüzdesel indirim değeri 0 ile 100 arasında olmalıdır.");
           }

        Coupon coupon = couponMapper.toCoupon(couponRequest);
        coupon.setCode(requestCode); // Büyük harf kaydet
        coupon.setTimesUsed(0); // Yeni kuponun kullanım sayısı 0 olmalı
        Coupon savedCoupon = couponRepository.save(coupon);
        logger.info("ADMIN: Coupon '{}' (ID: {}) created successfully.", savedCoupon.getCode(), savedCoupon.getId());
        return mapToResponseWithStatus(savedCoupon);
    }

    @Override
    @Transactional
    public CouponResponse updateCoupon(Long couponId, CouponRequest couponRequest) {
       // checkAdminRole();
        logger.info("ADMIN: Updating coupon with ID: {}", couponId);

        Coupon existingCoupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found with id: " + couponId));

         // Kodun güncellenmesine izin vermiyoruz (Mapper'da ignore edildi)
         if (!existingCoupon.getCode().equalsIgnoreCase(couponRequest.getCode())){
             logger.warn("ADMIN: Attempted to change coupon code for ID {}, which is not allowed.", couponId);
             // Gerekirse hata fırlat veya sadece kodu güncelleme
              // throw new BadRequestException("Kupon kodu değiştirilemez.");
         }

          // Yüzde kontrolü
         if(couponRequest.getDiscountType() == DiscountType.PERCENTAGE
                  && (couponRequest.getDiscountValue().compareTo(BigDecimal.ZERO) <= 0 || couponRequest.getDiscountValue().compareTo(BigDecimal.valueOf(100)) > 0)) {
              throw new BadRequestException("Yüzdesel indirim değeri 0 ile 100 arasında olmalıdır.");
          }


        couponMapper.updateCouponFromRequest(couponRequest, existingCoupon);

        Coupon updatedCoupon = couponRepository.save(existingCoupon);
        logger.info("ADMIN: Coupon ID: {} updated successfully.", updatedCoupon.getId());
        return mapToResponseWithStatus(updatedCoupon);
    }

    @Override
    @Transactional
    public void deleteCoupon(Long couponId) {
       // checkAdminRole();
        logger.warn("ADMIN: Deleting coupon with ID: {}", couponId);

        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found with id: " + couponId));

        // TODO: Silmek yerine pasife çekme (isActive=false) stratejisi değerlendirilebilir.
        // Siparişlerde kullanılan kupon referansının ne olacağına karar vermek lazım (SET NULL iyiydi).
        couponRepository.delete(coupon); // Direkt silelim şimdilik
        logger.info("ADMIN: Coupon ID: {} deleted successfully.", couponId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CouponResponse> findAllCoupons(Pageable pageable, Boolean isActive) {
      //  checkAdminRole();
        logger.debug("ADMIN: Fetching all coupons. IsActive filter: {}, Pageable: {}", isActive, pageable);

        Specification<Coupon> spec = Specification.where(CouponSpecifications.isActive(isActive));
        // İleride başka filtrelemeler eklenirse: spec = spec.and(CouponSpecifications.codeContains("..."));

        Page<Coupon> couponPage = couponRepository.findAll(spec, pageable);
        logger.debug("ADMIN: Found {} coupons matching criteria.", couponPage.getTotalElements());

        return couponPage.map(this::mapToResponseWithStatus); // mapToResponseWithStatus'u kullan
    }

    @Override
    @Transactional(readOnly = true)
    public CouponResponse findCouponById(Long couponId) {
      //  checkAdminRole();
        logger.debug("ADMIN: Fetching coupon by ID: {}", couponId);
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found with id: " + couponId));
        return mapToResponseWithStatus(coupon);
    }

    @Override
    @Transactional(readOnly = true)
    public CouponResponse findCouponByCode(String code) {
      //  checkAdminRole();
        String upperCaseCode = code.toUpperCase();
        logger.debug("ADMIN: Fetching coupon by code: {}", upperCaseCode);
        Coupon coupon = couponRepository.findByCode(upperCaseCode)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found with code: " + code));
        return mapToResponseWithStatus(coupon);
    }


    // --- Customer/System Operations Implementation ---

    @Override
    @Transactional(readOnly = true)
    public CouponValidationResponse validateCoupon(String code, BigDecimal cartTotal) {
        String upperCaseCode = code.toUpperCase();
        logger.debug("Validating coupon code: {} for cart total: {}", upperCaseCode, cartTotal);

        Optional<Coupon> couponOpt = couponRepository.findByCode(upperCaseCode);

        if (couponOpt.isEmpty()) {
            logger.warn("Validation failed: Coupon code '{}' not found.", upperCaseCode);
            return new CouponValidationResponse(false, "Geçersiz kupon kodu.", code, BigDecimal.ZERO);
        }

        Coupon coupon = couponOpt.get();

        if (!coupon.isValid()) {
            String message = "Kupon geçerli değil.";
            if(!coupon.isActive()) message = "Kupon şu anda aktif değil.";
            else if(coupon.isExpired()) message = "Kuponun kullanım süresi dolmuş.";
            else if(coupon.isUsageLimitReached()) message = "Kupon kullanım limitine ulaşmış.";
            logger.warn("Validation failed for coupon '{}': {}", upperCaseCode, message);
            return new CouponValidationResponse(false, message, code, BigDecimal.ZERO);
        }

        if (cartTotal == null) cartTotal = BigDecimal.ZERO; // Null gelme ihtimaline karşı

        if (cartTotal.compareTo(coupon.getMinPurchaseAmount()) < 0) {
             String message = String.format("Kuponu kullanmak için minimum sepet tutarı %.2f TL olmalıdır.", coupon.getMinPurchaseAmount());
            logger.warn("Validation failed for coupon '{}': Minimum purchase amount not met (Required: {}, Cart: {})",
                       upperCaseCode, coupon.getMinPurchaseAmount(), cartTotal);
            return new CouponValidationResponse(false, message, code, BigDecimal.ZERO);
        }

        // Kupon geçerli, indirim tutarını hesapla
        BigDecimal discountAmount = calculateDiscount(coupon, cartTotal);
        logger.info("Coupon code '{}' is valid. Calculated discount: {}", upperCaseCode, discountAmount);
        return new CouponValidationResponse(true, "Kupon başarıyla uygulandı!", code, discountAmount);
    }

     // İndirim hesaplama yardımcı metodu
     private BigDecimal calculateDiscount(Coupon coupon, BigDecimal amount) {
          BigDecimal discount = BigDecimal.ZERO;
         if (coupon.getDiscountType() == DiscountType.FIXED_AMOUNT) {
             discount = coupon.getDiscountValue();
          } else if (coupon.getDiscountType() == DiscountType.PERCENTAGE) {
             // Yüzdeyi uygula (scale ayarlarına dikkat et)
              discount = amount.multiply(coupon.getDiscountValue().divide(BigDecimal.valueOf(100), 2, BigDecimal.ROUND_HALF_UP)); // Virgülden sonra 2 basamak, yukarı yuvarla
          }
         // İndirim tutarı, esas tutarı geçemez
          return discount.min(amount);
      }

}