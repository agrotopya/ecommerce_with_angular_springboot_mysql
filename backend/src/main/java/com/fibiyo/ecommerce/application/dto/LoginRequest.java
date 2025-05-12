package com.fibiyo.ecommerce.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "Kullanıcı adı veya e-posta boş olamaz")
    @Size(max = 255)
    private String usernameOrEmail;

    @NotBlank(message = "Şifre boş olamaz")
    private String password;
}