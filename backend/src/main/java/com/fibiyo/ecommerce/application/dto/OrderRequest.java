package com.fibiyo.ecommerce.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal; // Keep if shippingFee or discountAmount are used, otherwise remove

@Data
public class OrderRequest {

    @NotNull(message = "Teslimat adresi boş olamaz")
    @Valid
    private AddressDto shippingAddress;

    @Valid
    private AddressDto billingAddress; // Opsiyonel

    @NotEmpty(message = "Ödeme yöntemi belirtilmelidir (örn: STRIPE, PAYPAL)")
    @Size(max = 50)
    private String paymentMethod;

    @Size(max = 50)
    private String couponCode; // Opsiyonel

    // Removed items list as per previous discussions, it's usually derived from cart
    // Removed shippingFee and discountAmount as these are usually calculated backend or part of payment flow
}

