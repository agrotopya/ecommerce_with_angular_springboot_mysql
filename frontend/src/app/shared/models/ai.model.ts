// Corresponds to backend ProductEnhancementRequestDto.java
export interface ProductEnhancementRequestDto {
  currentName: string;
  currentDescription?: string;
  categoryName?: string;
}

// Corresponds to backend ProductEnhancementResponseDto.java
export interface ProductEnhancementResponseDto {
  suggestedName?: string;
  suggestedDescription?: string;
  suggestedKeywords?: string[];
  idealImagePrompt?: string;
  generalTips?: string;
  success: boolean;
  errorMessage?: string;
}

// Corresponds to backend AiImageGenerationRequest.java
export interface AiImageGenerationRequest { // Dto suffix kaldırıldı, backend ile aynı olsun
  prompt: string;
  referenceImageIdentifier?: string;
  n?: number;
  size?: '256x256' | '512x512' | '1024x1024' | '1792x1024' | '1024x1792';
  model?: string;
}

// Corresponds to backend AiImageGenerationResponse.java
export interface AiImageGenerationResponse { // Dto suffix kaldırıldı
  success: boolean;
  message: string;
  imageUrls?: string[];
  remainingQuota?: number | null;
}

// Corresponds to backend SetAiImageAsProductRequest.java
export interface SetAiImageAsProductRequest { // Dto suffix kaldırıldı
  productId: number;
  aiImageUrl: string;
  setAsPrimary?: boolean; // Backend DTO'sunda bu alan var mı kontrol edilmeli, apidocs'ta yoktu.
                          // Eğer backend'de yoksa, frontend'de bu mantık yönetilmeli veya backend'e eklenmeli.
                          // Şimdilik frontend'de ProductFormComponent içinde yönetiliyor.
}
