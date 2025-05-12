package com.fibiyo.ecommerce.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List; // URL listesi için

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiImageGenerationResponse {
    private boolean success;
    private String message; // Başarı veya hata mesajı
    private List<String> imageUrls; // Başarılı ise üretilen URL'lerin listesi
    private Integer remainingQuota; // İstek sonrası kalan hak sayısı
}