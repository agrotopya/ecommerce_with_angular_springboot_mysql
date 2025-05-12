package com.fibiyo.ecommerce.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserProfileUpdateRequest {
    // Genellikle sadece isim/soyisim güncellenir kolayca.
    @NotBlank(message = "İsim boş olamaz")
    @Size(max = 100)
    private String firstName;

    @NotBlank(message = "Soyisim boş olamaz")
    @Size(max = 100)
    private String lastName;

    // Opsiyonel: Email değişikliği isteği (Doğrulama gerektirir)
    // @Email @Size(max=255)
    // private String email;

     // Opsiyonel: Adres Güncelleme
     // private List<AddressDto> addresses;
}