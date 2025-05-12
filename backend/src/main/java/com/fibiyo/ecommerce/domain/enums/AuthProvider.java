package com.fibiyo.ecommerce.domain.enums;

public enum AuthProvider {
    GOOGLE,
    FACEBOOK,
    GITHUB
    // Eklenebilir...
    , LOCAL // Klasik e-posta/şifre ile giriş yapanlar için (opsiyonel)
}