package com.fibiyo.ecommerce.application.dto;

import com.fibiyo.ecommerce.domain.enums.SubscriptionType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SubscriptionUpgradeRequest {
    @NotNull(message = "Abonelik türü boş olamaz")
    private SubscriptionType targetSubscription; // CUSTOMER_PLUS veya SELLER_PLUS
     // Opsiyonel: Ödeme periyodu (aylık/yıllık) vs. eklenebilir
}