package com.fibiyo.ecommerce.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct; // import et
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StripeConfig {

    @Value("${stripe.secret.key}")
    private String secretKey;

    @PostConstruct // Bean oluştuktan sonra çalışır
    public void initStripe() {
        Stripe.apiKey = secretKey; // Stripe kütüphanesine API anahtarını set et
    }
}