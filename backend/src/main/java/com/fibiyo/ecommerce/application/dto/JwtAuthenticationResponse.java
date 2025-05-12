package com.fibiyo.ecommerce.application.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List; // Roller listesi için

@Data
@NoArgsConstructor
public class JwtAuthenticationResponse {
    private String accessToken;
    private String tokenType = "Bearer";
    private Long userId;
    private String username;
    private String email;
    private List<String> roles; // Roller listesi

    public JwtAuthenticationResponse(String accessToken, Long userId, String username, String email, List<String> roles) {
        this.accessToken = accessToken;
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.roles = roles;
    }
}