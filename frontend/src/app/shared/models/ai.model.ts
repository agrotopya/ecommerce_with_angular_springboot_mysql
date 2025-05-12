// src/app/shared/models/ai.model.ts
import { Product } from "./product.model";

export interface AiImageGenerationRequest {
  prompt: string;
  referenceImageIdentifier?: string; // Depolamadaki referans görselin adı/yolu
  n?: number; // Üretilecek görsel sayısı (default 1)
  size?: '256x256' | '512x512' | '1024x1024'; // (default 1024x1024)
  model?: string; // Opsiyonel, kullanılacak model (default: "gpt-image-1" veya backend default'u)
}

export interface AiImageGenerationResponse {
  success: boolean;
  message: string;
  imageUrls?: string[]; // Üretilen görsellerin URL'leri
  remainingQuota?: number | null; // SELLER_PLUS değilse kalan kota, PLUS ise null
}

export interface SetAiImageAsProductRequest {
  productId: number;
  aiImageUrl: string; // AI ile üretilen ve saklanan görselin tam URL'si
}

// OpenAiImage DTO'su apidocs.md'de belirtilmiş, ancak doğrudan API yanıtında kullanılmıyor gibi.
// Eğer gerekirse eklenebilir.
// export interface OpenAiImage {
//   b64Json?: string; // Base64 kodlanmış resim verisi
//   url?: string; // Resmin URL'i
//   revised_prompt?: string; // Eğer prompt OpenAI tarafından revize edildiyse
// }
