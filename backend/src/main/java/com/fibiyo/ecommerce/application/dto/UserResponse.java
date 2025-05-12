package com.fibiyo.ecommerce.application.dto;

import com.fibiyo.ecommerce.domain.enums.AuthProvider;
import com.fibiyo.ecommerce.domain.enums.Role;
import com.fibiyo.ecommerce.domain.enums.SubscriptionType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private List<String> roles; // Changed from Role to List<String>
    private boolean isActive;
    private String avatar; // Added avatar field
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Profilde gösterilebilecek ek bilgiler
    private AuthProvider authProvider; // Nasıl giriş yaptığı
    private SubscriptionType subscriptionType;
    private LocalDateTime subscriptionExpiryDate;
    private int loyaltyPoints;
    // Sadece satıcı ise gösterilebilir:
    private Integer imageGenQuota; // Nullable yapalım

     // private List<AddressDto> addresses; // Adresleri de döndürebiliriz
     // Not: Şifre HASH'ini ASLA DTO'ya koyma!
}
