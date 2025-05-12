package com.fibiyo.ecommerce.application.dto;

import com.fibiyo.ecommerce.domain.enums.DiscountType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CouponResponse {
    private Long id;
    private String code;
    private String description;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private LocalDateTime expiryDate;
    private BigDecimal minPurchaseAmount;
    private boolean isActive;
    private Integer usageLimit; // Null ise sınırsız
    private int timesUsed;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    // Ekstra Alanlar
    private boolean valid;
private boolean expired;
private boolean usageLimitReached;

}