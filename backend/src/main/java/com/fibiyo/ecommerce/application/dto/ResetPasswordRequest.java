package com.fibiyo.ecommerce.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordRequest {
    @NotBlank(message = "Token boş olamaz")
    private String token;

    @NotBlank(message = "Yeni şifre boş olamaz")
    @Size(min = 6, max = 100, message = "Yeni şifre en az 6 karakter olmalıdır")
    private String newPassword;
}