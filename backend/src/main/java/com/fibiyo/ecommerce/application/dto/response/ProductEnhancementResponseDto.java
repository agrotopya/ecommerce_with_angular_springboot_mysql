package com.fibiyo.ecommerce.application.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductEnhancementResponseDto {
    private String suggestedName;
    private String suggestedDescription;
    private List<String> suggestedKeywords;
    private String idealImagePrompt; // Görsel için prompt önerisi
    private String generalTips; // Genel iyileştirme ipuçları
    private boolean success; // İşlemin başarılı olup olmadığını belirtir
    private String errorMessage; // Hata durumunda mesaj
}
