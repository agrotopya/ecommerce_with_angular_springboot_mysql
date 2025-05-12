package com.fibiyo.ecommerce.application.dto;

import com.fibiyo.ecommerce.domain.enums.SubscriptionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionStatusResponse {
    private SubscriptionType currentSubscription;
    private LocalDateTime expiryDate;
    private boolean isActive; // Genel aktiflik durumu
}