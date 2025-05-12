package com.fibiyo.ecommerce.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data // Adres DTO'su
public class AddressDto {
    @NotBlank(message = "Sokak/Cadde boş olamaz")
    @Size(max = 255)
    private String street;

    @NotBlank(message = "Şehir boş olamaz")
    @Size(max = 100)
    private String city;

    @NotBlank(message = "İl/Eyalet boş olamaz") // Veya İlçe? Duruma göre
    @Size(max = 100)
    private String state; // veya district/county

    @NotBlank(message = "Posta kodu boş olamaz")
    @Size(max = 20)
    private String zipCode;

    @NotBlank(message = "Ülke boş olamaz")
    @Size(max = 100)
    private String country;

    // Opsiyonel: Adres başlığı, telefon vb. eklenebilir
    private String addressTitle;
    private String phoneNumber;
}