package com.fibiyo.ecommerce.application.service.impl;

import com.fibiyo.ecommerce.application.dto.AiImageGenerationRequest;
import com.fibiyo.ecommerce.application.dto.AiImageGenerationResponse; // Eklendi
import com.fibiyo.ecommerce.application.dto.OpenAiImage;
import com.fibiyo.ecommerce.application.dto.request.ProductEnhancementRequestDto;
import com.fibiyo.ecommerce.application.dto.response.ProductEnhancementResponseDto;
import com.fibiyo.ecommerce.application.exception.BadRequestException;
import com.fibiyo.ecommerce.application.exception.ResourceNotFoundException;
import com.fibiyo.ecommerce.application.service.AiService;
import com.fibiyo.ecommerce.application.service.StorageService;
import com.fibiyo.ecommerce.domain.entity.User;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Arrays;
import java.util.stream.Collectors;

@Service
public class OpenAiServiceImpl implements AiService {

    private static final Logger logger = LoggerFactory.getLogger(OpenAiServiceImpl.class);

    private final StorageService storageService;
    @Value("${openai.api.key}")
    private String openAiApiKey;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    private final WebClient openAiWebClient;
    private final WebClient geminiWebClient;
    private final ObjectMapper objectMapper;


    @Autowired
    public OpenAiServiceImpl(StorageService storageService, ObjectMapper objectMapper) {
        this.storageService = storageService;
        this.objectMapper = objectMapper;

        this.openAiWebClient = WebClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .defaultHeader("Authorization", "Bearer " + openAiApiKey)
                .exchangeStrategies(ExchangeStrategies.builder()
                    .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)) // 10MB
                    .build())
                .build();
        
        this.geminiWebClient = WebClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com") // Gemini base URL
                .exchangeStrategies(ExchangeStrategies.builder()
                    .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                    .build())
                .build();
    }


    @Override
    public AiImageGenerationResponse generateProductImage(@NonNull AiImageGenerationRequest request, @NonNull User requestingUser) { // Dönüş tipi güncellendi
        String requestedModel = StringUtils.hasText(request.getModel()) ? request.getModel().toLowerCase() : "dall-e-3";
        logger.info("AI Image Request: User ID: {}, Model: {}, Prompt: '{}...', RefImgId: {}",
                    requestingUser.getId(), requestedModel,
                    request.getPrompt() != null ? request.getPrompt().substring(0, Math.min(request.getPrompt().length(), 30)) : "N/A",
                    request.getReferenceImageIdentifier());
        try {
            String endpointUrl;
            WebClient.ResponseSpec responseSpec;

            ObjectNode baseRequestNode = objectMapper.createObjectNode();
            baseRequestNode.put("model", requestedModel);
            baseRequestNode.put("n", request.getN() == null ? 1 : Math.max(1, Math.min(request.getN(), 10)));
            baseRequestNode.put("size", StringUtils.hasText(request.getSize()) ? request.getSize() : "1024x1024");
            baseRequestNode.put("response_format", "b64_json");

            if (StringUtils.hasText(request.getReferenceImageIdentifier())) {
                logger.info("Executing OpenAI Image EDIT/VARIATION for User ID: {} with model {}...", requestingUser.getId(), requestedModel);
                 if (!"dall-e-2".equals(requestedModel)) {
                    logger.warn("Image editing/variation is typically used with 'dall-e-2'. The provided model '{}' may not support it or behave as expected.", requestedModel);
                     throw new BadRequestException("Image editing/variation is only supported with dall-e-2 model.");
                }
                endpointUrl = "/images/edits"; 

                Path imagePath = storageService.load(request.getReferenceImageIdentifier());
                File imageFile = imagePath.toFile();
                if (!imageFile.exists()) {
                    throw new ResourceNotFoundException("Reference image not found at: " + request.getReferenceImageIdentifier());
                }

                MultipartBodyBuilder builder = new MultipartBodyBuilder();
                builder.part("image", new FileSystemResource(imageFile));
                if (StringUtils.hasText(request.getPrompt())) { 
                    builder.part("prompt", request.getPrompt());
                } else {
                    throw new BadRequestException("Prompt is required for image editing.");
                }
                builder.part("n", String.valueOf(baseRequestNode.get("n").asInt()));
                builder.part("size", baseRequestNode.get("size").asText());
                builder.part("response_format", baseRequestNode.get("response_format").asText());
                
                MultiValueMap<String, HttpEntity<?>> multipartBody = builder.build();
                
                responseSpec = this.openAiWebClient.post()
                                     .uri(endpointUrl)
                                     .contentType(MediaType.MULTIPART_FORM_DATA)
                                     .body(BodyInserters.fromMultipartData(multipartBody))
                                     .retrieve();
            } else {
                logger.info("Executing OpenAI Image GENERATION (text-to-image) for User ID: {} with model {}...", requestingUser.getId(), requestedModel);
                endpointUrl = "/images/generations"; 
                
                baseRequestNode.put("prompt", request.getPrompt());
                String requestBodyJson = objectMapper.writeValueAsString(baseRequestNode);
                logger.debug("OpenAI API Generation Request Body for {}: {}", requestedModel, requestBodyJson);

                responseSpec = this.openAiWebClient.post()
                                     .uri(endpointUrl)
                                     .contentType(MediaType.APPLICATION_JSON)
                                     .bodyValue(requestBodyJson)
                                     .retrieve();
            }

            JsonNode response = responseSpec
                    .bodyToMono(JsonNode.class)
                    .doOnSuccess(res -> logger.info("Raw OpenAI API response for model {}: {}", requestedModel, res != null ? res.toString().substring(0, Math.min(res.toString().length(), 500)) : "null"))
                    .doOnError(err -> logger.error("Error from OpenAI API (model {}): ", requestedModel, err))
                    .block(); 

            if (response == null || !response.has("data")) {
                logger.error("OpenAI API response for model {} does not contain 'data' field or response is null. Response: {}",
                             requestedModel, response != null ? response.toString() : "null");
                return new AiImageGenerationResponse(false, "AI görsel servisinden (" + requestedModel + ") beklenen formatta yanıt alınamadı.", null, requestingUser.getImageGenQuota());
            }
            
            List<OpenAiImage> generatedApiImages = new ArrayList<>();
            if (response.get("data").isArray()) {
                for (JsonNode imgNode : response.get("data")) {
                    OpenAiImage img = new OpenAiImage();
                    if (imgNode.has("b64_json")) {
                        img.setB64Json(imgNode.get("b64_json").asText());
                    } else if (imgNode.has("url")) {
                        img.setUrl(imgNode.get("url").asText());
                    }
                    generatedApiImages.add(img);
                }
            }

            logger.info("Received {} image data object(s) from OpenAI API (model {}).", generatedApiImages.size(), requestedModel);
            List<String> storedImageUrls = processAndStoreImages(generatedApiImages);
            // TODO: Kota düşürme mantığı UserServis'e taşınabilir veya burada kalabilir.
            // Şimdilik User entity'sindeki imageGenQuota'nın güncellendiğini varsayıyoruz.
            return new AiImageGenerationResponse(true, storedImageUrls.size() + " adet görsel başarıyla oluşturuldu.", storedImageUrls, requestingUser.getImageGenQuota());

        } catch (ResourceNotFoundException | BadRequestException e) {
            logger.error("AI Image Generation failed due to known exception: {}", e.getMessage());
            return new AiImageGenerationResponse(false, e.getMessage(), null, requestingUser.getImageGenQuota());
        } catch (WebClientResponseException wcre) {
            logger.error("OpenAI API call failed for User ID: {}. Model: {}. Status: {}. Response Body: {}",
                    requestingUser.getId(), requestedModel, wcre.getStatusCode(), wcre.getResponseBodyAsString(), wcre);
            String userMessage = "AI görsel servisiyle iletişimde bir hata oluştu: " + wcre.getStatusCode();
            try {
                JsonNode errorNode = new ObjectMapper().readTree(wcre.getResponseBodyAsString());
                if (errorNode.has("error") && errorNode.get("error").has("message")) {
                    userMessage = "AI Servisi Hatası: " + errorNode.get("error").get("message").asText();
                }
            } catch (Exception jsonEx) {
                logger.error("Could not parse OpenAI error response body: {}", jsonEx.getMessage());
            }
            return new AiImageGenerationResponse(false, userMessage, null, requestingUser.getImageGenQuota());
        } catch (Exception e) {
            logger.error("Unexpected error during AI Image Generation for User ID: {}. Model: {}. Error Type: {}, Message: {}",
                    requestingUser.getId(), requestedModel, e.getClass().getSimpleName(), e.getMessage(), e);
            return new AiImageGenerationResponse(false, "AI görseli üretilirken beklenmedik bir sunucu hatası oluştu.", null, requestingUser.getImageGenQuota());
        }
    }

    private List<String> processAndStoreImages(@NonNull List<OpenAiImage> images) {
        Objects.requireNonNull(images, "Image list cannot be null");
        List<String> storedUrls = new ArrayList<>();
        for (OpenAiImage img : images) {
            if (StringUtils.hasText(img.getB64Json())) {
                try {
                    byte[] imageBytes = Base64.getDecoder().decode(img.getB64Json());
                    String filename = storageService.store(imageBytes, "ai_generated_images", "png");
                    String url = storageService.generateUrl(filename);
                    storedUrls.add(url);
                    logger.info("Stored generated AI image (from b64_json) as {} at URL: {}", filename, url);
                } catch (Exception e) {
                    logger.error("Failed to store or generate URL for base64 image: {}", e.getMessage(), e);
                }
            } else if (StringUtils.hasText(img.getUrl())) {
                try {
                    String imageUrl = img.getUrl();
                    logger.info("Attempting to download image from OpenAI URL: {}", imageUrl);
                    byte[] imageBytes = WebClient.create().get().uri(imageUrl).retrieve().bodyToMono(byte[].class).block();
                    if (imageBytes != null && imageBytes.length > 0) {
                        String filename = storageService.store(imageBytes, "ai_generated_images", "png");
                        String storedUrl = storageService.generateUrl(filename);
                        storedUrls.add(storedUrl);
                        logger.info("Downloaded and stored image from OpenAI URL {} as {}", imageUrl, filename);
                    } else {
                        logger.error("Failed to download image from OpenAI URL: {} (empty response)", imageUrl);
                    }
                } catch (Exception e) {
                    logger.error("Failed to download or store image from OpenAI URL: {}", e.getMessage(), e);
                }
            }
        }
        if (storedUrls.isEmpty() && !images.isEmpty()) {
            logger.error("Failed to process or store ANY image received from OpenAI (Received {} image objects initially).", images.size());
        }
        return storedUrls;
    }

    @Override
    public ProductEnhancementResponseDto getEnhancedProductDetails(@NonNull ProductEnhancementRequestDto requestDto, @NonNull User requestingUser) {
        logger.info("Product enhancement request for user ID: {}, Product Name: '{}'", requestingUser.getId(), requestDto.getCurrentName());

        String promptText = String.format(
            "Bir e-ticaret platformu için ürün listeleme detaylarını iyileştir.\n" +
            "Mevcut Ürün Adı: \"%s\"\n" +
            "Mevcut Açıklama: \"%s\"\n" +
            "Kategori: \"%s\"\n\n" +
            "Lütfen aşağıdaki formatta ve her bir başlığı tam olarak kullanarak öneriler sun (başlıkları ** ile işaretle):\n" +
            "**Önerilen Ürün Adı:** [Maksimum 70 karakter, dikkat çekici ve SEO dostu bir ürün adı önerisi]\n" +
            "**Önerilen Ürün Açıklaması:** [Maksimum 3 paragraf, ürünün faydalarını ve özelliklerini vurgulayan, akıcı bir dille yazılmış açıklama önerisi]\n" +
            "**Önerilen Anahtar Kelimeler:** [Virgülle ayrılmış 5-7 adet alakalı anahtar kelime]\n" +
            "**İdeal Görsel İçin Prompt:** [Ürünü en iyi şekilde sergileyecek bir görsel için DALL-E veya Midjourney gibi bir AI modeline verilebilecek detaylı prompt önerisi]\n" +
            "**Genel İpuçları:** [Satıcının listelemeyi daha da iyileştirmesi için 2-3 kısa ve etkili ipucu]",
            requestDto.getCurrentName(),
            requestDto.getCurrentDescription() == null ? "Belirtilmemiş" : requestDto.getCurrentDescription(),
            requestDto.getCategoryName() == null ? "Belirtilmemiş" : requestDto.getCategoryName()
        );

        ObjectNode requestPayload = objectMapper.createObjectNode();
        ArrayNode contentsArray = requestPayload.putArray("contents");
        ObjectNode content = contentsArray.addObject();
        ArrayNode partsArray = content.putArray("parts");
        partsArray.addObject().put("text", promptText);
        
        try {
            logger.debug("Sending request to Gemini API. Prompt: {}", promptText.substring(0, Math.min(promptText.length(), 200)) + "...");
            JsonNode geminiResponse = geminiWebClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1beta/models/gemini-1.5-flash-latest:generateContent")
                            .queryParam("key", geminiApiKey)
                            .build())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestPayload.toString())
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .doOnError(e -> logger.error("Error response from Gemini API: {}", e.getMessage(), e))
                    .block(); 

            if (geminiResponse == null) {
                logger.error("Gemini API returned null response for product: {}", requestDto.getCurrentName());
                return new ProductEnhancementResponseDto(null, null, null, null, null, false, "AI servisinden yanıt alınamadı.");
            }
            
            logger.debug("Raw Gemini API response: {}", geminiResponse.toString().substring(0, Math.min(geminiResponse.toString().length(), 500)) + "...");

            if (geminiResponse.has("candidates") && geminiResponse.get("candidates").isArray() && geminiResponse.get("candidates").size() > 0) {
                JsonNode candidate = geminiResponse.get("candidates").get(0);
                if (candidate.has("content") && candidate.get("content").has("parts") && candidate.get("content").get("parts").isArray() && candidate.get("content").get("parts").size() > 0) {
                    String rawText = candidate.get("content").get("parts").get(0).get("text").asText("");
                    return parseGeminiResponse(rawText, requestDto.getCurrentName());
                }
            }
            
            logger.warn("Gemini API response format was not as expected for product: {}. Response: {}", requestDto.getCurrentName(), geminiResponse.toString());
            return new ProductEnhancementResponseDto(null, null, null, null, null, false, "AI servisinden beklenen formatta yanıt alınamadı.");

        } catch (WebClientResponseException wcre) {
            logger.error("Gemini API call failed for product: {}. Status: {}. Response: {}", 
                requestDto.getCurrentName(), wcre.getStatusCode(), wcre.getResponseBodyAsString(), wcre);
            return new ProductEnhancementResponseDto(null, null, null, null, null, false, "AI servisiyle iletişimde hata: " + wcre.getStatusCode());
        } catch (Exception e) {
            logger.error("Unexpected error during Gemini API call for product: {}: {}", requestDto.getCurrentName(), e.getMessage(), e);
            return new ProductEnhancementResponseDto(null, null, null, null, null, false, "AI önerileri alınırken beklenmedik bir hata oluştu.");
        }
    }

    private ProductEnhancementResponseDto parseGeminiResponse(String rawText, String originalProductName) {
        String suggestedName = extractSection(rawText, "Önerilen Ürün Adı:");
        String suggestedDescription = extractSection(rawText, "Önerilen Ürün Açıklaması:");
        String keywordsRaw = extractSection(rawText, "Önerilen Anahtar Kelimeler:");
        List<String> keywords = StringUtils.hasText(keywordsRaw) ? 
                                Arrays.stream(keywordsRaw.split(","))
                                      .map(String::trim)
                                      .filter(StringUtils::hasText)
                                      .collect(Collectors.toList()) :
                                List.of();
        String idealImagePrompt = extractSection(rawText, "İdeal Görsel İçin Prompt:");
        String generalTips = extractSection(rawText, "Genel İpuçları:");

        if (!StringUtils.hasText(suggestedName) && !StringUtils.hasText(suggestedDescription)) {
             logger.warn("Could not parse significant suggestions from Gemini response for product '{}'. Raw text: {}", originalProductName, rawText.substring(0, Math.min(rawText.length(), 300)));
             return new ProductEnhancementResponseDto(null, null, null, null, null, false, "AI servisinden anlamlı öneriler ayrıştırılamadı.");
        }

        return new ProductEnhancementResponseDto(
                suggestedName,
                suggestedDescription,
                keywords,
                idealImagePrompt,
                generalTips,
                true,
                null
        );
    }

    private String extractSection(String text, String header) {
        try {
            Pattern pattern = Pattern.compile("\\*\\*" + Pattern.quote(header) + "\\*\\*\\s*([\\s\\S]*?)(?=\\n\\*\\*|$)");
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                return matcher.group(1).trim();
            }
        } catch (Exception e) {
            logger.error("Error extracting section '{}' from text: {}", header, e.getMessage());
        }
        return "";
    }
}
