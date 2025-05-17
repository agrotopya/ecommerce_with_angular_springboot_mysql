// src/app/shared/models/ai.model.ts

export interface AiImageGenerationRequest {
  prompt: string;
  model?: string; // e.g., 'gpt-image-1', 'dall-e-2', 'dall-e-3'
  n?: number; // Number of images to generate (1-10)
  size?: string; // e.g., '1024x1024', '1792x1024', '1024x1792' (model specific)
  quality?: 'standard' | 'hd' | 'auto' | 'high' | 'medium' | 'low'; // Model specific
  style?: 'vivid' | 'natural'; // For dall-e-3
  response_format?: 'url' | 'b64_json'; // For dall-e-2/3. gpt-image-1 always b64_json.

  // gpt-image-1 specific (opsiyonel)
  background?: 'transparent' | 'opaque' | 'auto';
  moderation?: 'low' | 'auto';
  output_compression?: number; // 0-100
  output_format?: 'png' | 'jpeg' | 'webp';

  // For image editing (opsiyonel)
  referenceImageIdentifier?: string; // Storage'daki referans görselin ID'si/yolu
  maskImageIdentifier?: string; // Storage'daki maske görselinin ID'si/yolu (opsiyonel)
}

export interface AiImageGenerationResponse {
  created: number; // Unix timestamp
  imageUrls: string[]; // Saklanan görsellerin URL'leri
  // Backend'den dönen ham OpenAI yanıtındaki b64_json verileri de eklenebilir,
  // veya direkt URL'ler yeterli olabilir.
  // openAiData?: { b64_json?: string; url?: string; revised_prompt?: string; }[];
  remainingQuota?: number; // Kullanıcının kalan görsel oluşturma kotası
  message?: string; // Başarı veya bilgi mesajı
}

export interface SetAiImageAsProductRequest {
  productId: number;
  aiImageUrl: string; // Backend DTO ile eşleşmesi için 'generatedImageUrl' -> 'aiImageUrl' olarak değiştirildi
  // Hangi görselin yerine geçeceğini belirtmek için:
  targetImageId?: number; // Mevcut bir ProductImage ID'si (bunu güncellemek için)
  setAsPrimary?: boolean; // Eğer true ise, bu görsel ana görsel yapılır
  // Eğer targetImageId verilmezse ve setAsPrimary true ise mainImageUrl güncellenir
  // veya yeni bir ProductImage olarak eklenip primary yapılabilir.
  // Bu mantık backend'de netleştirilmeli.
}
