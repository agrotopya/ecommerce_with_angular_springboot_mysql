package com.fibiyo.ecommerce.application.dto;

import com.fibiyo.ecommerce.domain.enums.DiscountType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CouponRequest {

    @NotBlank(message = "Kupon kodu boş olamaz")
    @Size(min = 3, max = 50, message = "Kupon kodu 3 ile 50 karakter arasında olmalıdır")
    // Kodun unique olması DB'de kontrol ediliyor ama servis'te de kontrol edilebilir.
    private String code;

    private String description; // Opsiyonel

    @NotNull(message = "İndirim tipi boş olamaz")
    private DiscountType discountType;

    @NotNull(message = "İndirim değeri boş olamaz")
    @DecimalMin(value = "0.01", message = "İndirim değeri pozitif olmalıdır")
    private BigDecimal discountValue; // Yüzde veya Tutar

    @NotNull(message = "Geçerlilik bitiş tarihi boş olamaz")
    @Future(message = "Geçerlilik bitiş tarihi geçmişte olamaz")
    private LocalDateTime expiryDate;

    @NotNull(message = "Minimum alışveriş tutarı boş olamaz")
    @DecimalMin(value = "0.00", message = "Minimum alışveriş tutarı negatif olamaz")
    private BigDecimal minPurchaseAmount = BigDecimal.ZERO;

    @NotNull(message = "Aktiflik durumu belirtilmelidir")
    private Boolean isActive = true; // Varsayılan

    @Min(value = 0, message = "Kullanım limiti negatif olamaz")
    private Integer usageLimit; // Null ise sınırsız
}