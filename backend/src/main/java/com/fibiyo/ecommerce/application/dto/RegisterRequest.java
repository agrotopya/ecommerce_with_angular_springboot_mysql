package com.fibiyo.ecommerce.application.dto;

import com.fibiyo.ecommerce.domain.enums.Role;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "İsim boş olamaz")
    @Size(max = 100)
    private String firstName;

    @NotBlank(message = "Soyisim boş olamaz")
    @Size(max = 100)
    private String lastName;

    @NotBlank(message = "Kullanıcı adı boş olamaz")
    @Size(min = 3, max = 100, message = "Kullanıcı adı 3 ile 100 karakter arasında olmalıdır")
    private String username;

    @NotBlank(message = "E-posta boş olamaz")
    @Email(message = "Geçerli bir e-posta adresi giriniz")
    @Size(max = 255)
    private String email;

    @NotBlank(message = "Şifre boş olamaz")
    @Size(min = 6, max = 100, message = "Şifre en az 6 karakter olmalıdır") // Min 6 karakter önerilir
    private String password;

    @NotNull(message = "Rol boş olamaz")
    private Role role; // Kullanıcı kayıt olurken rolünü seçebilmeli (CUSTOMER veya SELLER)
}