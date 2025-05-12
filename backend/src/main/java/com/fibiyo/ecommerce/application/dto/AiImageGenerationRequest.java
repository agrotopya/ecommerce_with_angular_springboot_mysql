package com.fibiyo.ecommerce.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min; // 'n' için eklendi
import lombok.Data;

@Data
public class AiImageGenerationRequest {

    @NotBlank(message = "Prompt boş olamaz")
    @Size(min = 10, max = 1000, message = "Prompt 10 ile 1000 karakter arasında olmalıdır.")
    private String prompt;

    // Referans görselin storage'daki unique adı VEYA tam URL'i olabilir.
    // Bu alanı şimdilik opsiyonel yapalım, sadece prompt ile üretim de isteyebiliriz.
    private String referenceImageIdentifier;

    // Opsiyonel parametreler
    @Min(value = 1, message = "En az 1 görsel üretilmelidir.")
    private Integer n = 1; // Kaç adet görsel istenecek

    // Desteklenen boyutları kontrol etmek gerekebilir (örn: OpenAI DALL-E belirli boyutları destekler)
    @NotBlank(message = "Görsel boyutu boş olamaz (örn: 1024x1024)")
    @Size(max=20) // 1024x1024, 512x512, 256x256 gibi
    private String size = "1024x1024";
    private String model = " gpt-image-1"; 
     // Opsiyonel: style, quality vb. eklenebilir
     // private String style;
     // private String quality = "standard"; // veya "hd"
}