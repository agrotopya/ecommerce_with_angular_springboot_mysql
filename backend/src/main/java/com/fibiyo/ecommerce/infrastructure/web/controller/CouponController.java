package com.fibiyo.ecommerce.infrastructure.web.controller;

import com.fibiyo.ecommerce.application.dto.ApiResponse;
import com.fibiyo.ecommerce.application.dto.CouponRequest;
import com.fibiyo.ecommerce.application.dto.CouponResponse;
import com.fibiyo.ecommerce.application.dto.CouponValidationResponse;
import com.fibiyo.ecommerce.application.service.CouponService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull; // @RequestParam validasyonu için
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.NumberFormat; // BigDecimal validasyonu
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated; // @RequestParam validasyonu için
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/coupons")
@Validated // @RequestParam validasyonlarını etkinleştirmek için
public class CouponController {

    private static final Logger logger = LoggerFactory.getLogger(CouponController.class);

    private final CouponService couponService;

    @Autowired
    public CouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    // --- Public/Customer Endpoint ---

    // Kupon kodunu doğrulamak için endpoint (Sepetteki toplam ile birlikte)
    @GetMapping("/validate/{code}")
    public ResponseEntity<CouponValidationResponse> validateCoupon(
            @PathVariable String code,
            // BigDecimal validasyonu ekleyelim (pozitif olmalı)
            @RequestParam @NotNull @NumberFormat(style = NumberFormat.Style.NUMBER) @Min(value = 0, message = "Sepet tutarı negatif olamaz") BigDecimal cartTotal) {
         logger.info("GET /api/coupons/validate/{} requested with cartTotal: {}", code, cartTotal);
         // Servis içindeki büyük harf kontrolü yeterli, burada tekrar yapmaya gerek yok.
         CouponValidationResponse validationResponse = couponService.validateCoupon(code, cartTotal);
         // Kupon geçersiz olsa bile 200 OK döneriz, yanıtın içeriği durumu belirtir.
         return ResponseEntity.ok(validationResponse);
    }

    // --- Admin Endpoints ---

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CouponResponse> createCoupon(@Valid @RequestBody CouponRequest couponRequest) {
        logger.info("POST /api/coupons requested (Admin)");
        CouponResponse createdCoupon = couponService.createCoupon(couponRequest);
        return new ResponseEntity<>(createdCoupon, HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<CouponResponse>> getAllCoupons(
             @PageableDefault(size = 15, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
             @RequestParam(required = false) Boolean isActive) { // isActive filtresi
         logger.info("GET /api/coupons requested (Admin). Filter isActive: {}", isActive);
        Page<CouponResponse> coupons = couponService.findAllCoupons(pageable, isActive);
        return ResponseEntity.ok(coupons);
    }

    @GetMapping("/{couponId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CouponResponse> getCouponById(@PathVariable Long couponId) {
        logger.info("GET /api/coupons/{} requested (Admin)", couponId);
        CouponResponse coupon = couponService.findCouponById(couponId);
        return ResponseEntity.ok(coupon);
    }

     @GetMapping("/code/{code}") // Path variable olarak kodu alalım
     @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CouponResponse> getCouponByCode(@PathVariable String code) {
          logger.info("GET /api/coupons/code/{} requested (Admin)", code);
         CouponResponse coupon = couponService.findCouponByCode(code);
          return ResponseEntity.ok(coupon);
     }


    @PutMapping("/{couponId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CouponResponse> updateCoupon(
            @PathVariable Long couponId,
            @Valid @RequestBody CouponRequest couponRequest) {
         logger.info("PUT /api/coupons/{} requested (Admin)", couponId);
        CouponResponse updatedCoupon = couponService.updateCoupon(couponId, couponRequest);
        return ResponseEntity.ok(updatedCoupon);
    }

    @DeleteMapping("/{couponId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> deleteCoupon(@PathVariable Long couponId) {
         logger.warn("DELETE /api/coupons/{} requested (Admin)", couponId);
        couponService.deleteCoupon(couponId);
        return ResponseEntity.ok(new ApiResponse(true, "Kupon başarıyla silindi."));
    }
}