package com.fibiyo.ecommerce.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal; // İndirim tutarını göstermek için

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CouponValidationResponse {
   private boolean valid;
   private String message; // Geçersizse nedenini belirtir
   private String code; // Kupon kodu
   private BigDecimal discountAmountCalculated; // Opsiyonel: Sepete göre hesaplanan indirim
}