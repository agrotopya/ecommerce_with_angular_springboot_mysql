package com.fibiyo.ecommerce.application.dto;


import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class ReviewRequest {

    // productId URL'den alınacak

    @NotNull(message = "Puan boş olamaz")
    @Min(value = 1, message = "Puan en az 1 olmalıdır")
    @Max(value = 5, message = "Puan en fazla 5 olabilir")
    private Byte rating;

    @Size(max = 5000, message = "Yorum en fazla 5000 karakter olabilir") // İstediğiniz bir limiti belirleyebilirsiniz
    private String comment; // Opsiyonel olabilir
}