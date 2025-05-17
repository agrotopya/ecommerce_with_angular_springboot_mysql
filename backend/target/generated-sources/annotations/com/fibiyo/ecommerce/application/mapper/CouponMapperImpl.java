package com.fibiyo.ecommerce.application.mapper;

import com.fibiyo.ecommerce.application.dto.CouponRequest;
import com.fibiyo.ecommerce.application.dto.CouponResponse;
import com.fibiyo.ecommerce.domain.entity.Coupon;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-05-17T12:02:50+0300",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.42.0.v20250508-0718, environment: Java 21.0.7 (Eclipse Adoptium)"
)
@Component
public class CouponMapperImpl implements CouponMapper {

    @Override
    public CouponResponse toCouponResponse(Coupon coupon) {
        if ( coupon == null ) {
            return null;
        }

        CouponResponse couponResponse = new CouponResponse();

        couponResponse.setActive( coupon.isActive() );
        couponResponse.setCode( coupon.getCode() );
        couponResponse.setCreatedAt( coupon.getCreatedAt() );
        couponResponse.setDescription( coupon.getDescription() );
        couponResponse.setDiscountType( coupon.getDiscountType() );
        couponResponse.setDiscountValue( coupon.getDiscountValue() );
        couponResponse.setExpiryDate( coupon.getExpiryDate() );
        couponResponse.setId( coupon.getId() );
        couponResponse.setMinPurchaseAmount( coupon.getMinPurchaseAmount() );
        couponResponse.setTimesUsed( coupon.getTimesUsed() );
        couponResponse.setUpdatedAt( coupon.getUpdatedAt() );
        couponResponse.setUsageLimit( coupon.getUsageLimit() );

        return couponResponse;
    }

    @Override
    public List<CouponResponse> toCouponResponseList(List<Coupon> coupons) {
        if ( coupons == null ) {
            return null;
        }

        List<CouponResponse> list = new ArrayList<CouponResponse>( coupons.size() );
        for ( Coupon coupon : coupons ) {
            list.add( toCouponResponse( coupon ) );
        }

        return list;
    }

    @Override
    public Coupon toCoupon(CouponRequest couponRequest) {
        if ( couponRequest == null ) {
            return null;
        }

        Coupon coupon = new Coupon();

        coupon.setCode( couponRequest.getCode() );
        coupon.setDescription( couponRequest.getDescription() );
        coupon.setDiscountType( couponRequest.getDiscountType() );
        coupon.setDiscountValue( couponRequest.getDiscountValue() );
        coupon.setExpiryDate( couponRequest.getExpiryDate() );
        coupon.setMinPurchaseAmount( couponRequest.getMinPurchaseAmount() );
        coupon.setUsageLimit( couponRequest.getUsageLimit() );

        return coupon;
    }

    @Override
    public void updateCouponFromRequest(CouponRequest request, Coupon coupon) {
        if ( request == null ) {
            return;
        }

        if ( request.getDescription() != null ) {
            coupon.setDescription( request.getDescription() );
        }
        if ( request.getDiscountType() != null ) {
            coupon.setDiscountType( request.getDiscountType() );
        }
        if ( request.getDiscountValue() != null ) {
            coupon.setDiscountValue( request.getDiscountValue() );
        }
        if ( request.getExpiryDate() != null ) {
            coupon.setExpiryDate( request.getExpiryDate() );
        }
        if ( request.getMinPurchaseAmount() != null ) {
            coupon.setMinPurchaseAmount( request.getMinPurchaseAmount() );
        }
        if ( request.getUsageLimit() != null ) {
            coupon.setUsageLimit( request.getUsageLimit() );
        }
    }
}
