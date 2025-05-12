package com.fibiyo.ecommerce.application.mapper;

import com.fibiyo.ecommerce.application.dto.CouponRequest;
import com.fibiyo.ecommerce.application.dto.CouponResponse;
import com.fibiyo.ecommerce.domain.entity.Coupon;
import org.mapstruct.*; // Importlar

import java.util.List;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface CouponMapper {

    // Coupon -> CouponResponse
    // Ekstra alanları (isValid, isExpired etc.) map'lemeden sonra serviste set edebiliriz
    @Mapping(target = "valid", ignore = true)
    @Mapping(target = "expired", ignore = true)
    @Mapping(target = "usageLimitReached", ignore = true)
    
    CouponResponse toCouponResponse(Coupon coupon);

    List<CouponResponse> toCouponResponseList(List<Coupon> coupons);

    // CouponRequest -> Coupon (Create için)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "timesUsed", ignore = true) // Başlangıçta 0
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "orders", ignore = true) // İlişkiyi map'lemiyoruz
    Coupon toCoupon(CouponRequest couponRequest);

    // CouponRequest'ten mevcut Coupon'u güncelleme
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "timesUsed", ignore = true) // Bu sayaç sadece siparişle artar
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "orders", ignore = true)
     @Mapping(target = "code", ignore = true) // Genellikle kupon kodu değiştirilmez, değiştirilecekse serviste kontrol edilmeli.
    void updateCouponFromRequest(CouponRequest request, @MappingTarget Coupon coupon);

}