package com.fibiyo.ecommerce.application.service.impl;

import com.fibiyo.ecommerce.application.dto.AiImageGenerationRequest;
import com.fibiyo.ecommerce.application.dto.OpenAiImage;
import com.fibiyo.ecommerce.application.exception.BadRequestException;
import com.fibiyo.ecommerce.application.exception.ResourceNotFoundException;
import com.fibiyo.ecommerce.application.service.AiService;
import com.fibiyo.ecommerce.application.service.StorageService;
import com.fibiyo.ecommerce.domain.entity.User;
import com.openai.client.OpenAIClient;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class OpenAiServiceImpl implements AiService {

    private static final Logger logger = LoggerFactory.getLogger(OpenAiServiceImpl.class);

    private final StorageService storageService;
    private final Optional<OpenAIClient> openAIClient;
    @Value("${openai.api.key}")
    private String openAiApiKey;

    @Autowired(required = false)
    public OpenAiServiceImpl(OpenAIClient openAIClient, StorageService storageService) {
        this.openAIClient = Optional.ofNullable(openAIClient);
        this.storageService = storageService;
    }

    private WebClient createWebClient(String baseUrl) {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + openAiApiKey)
                .exchangeStrategies(ExchangeStrategies.builder()
                    .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                    .build())
                .build();
    }



    @Override
    public List<String> generateProductImage(@NonNull AiImageGenerationRequest request, @NonNull User requestingUser) {
        logger.info("AI Image Request - User: {}, Ref Image ID: {}, Size: {}, Count: {}, Prompt starts: 	{}...	",
                requestingUser.getId(),
                request.getReferenceImageIdentifier() != null ? request.getReferenceImageIdentifier() : "None",
                request.getSize(),
                request.getN(),
                request.getPrompt().substring(0, Math.min(request.getPrompt().length(), 50)));

        try {
            List<OpenAiImage> generatedApiImages;
            if ("gpt-image-1".equalsIgnoreCase(request.getModel())) {
               // yerine bu gelecek
logger.debug("Generating image using GPT-Image-1 model via /v1/images/generations...");

WebClient webClient = createWebClient("https://api.openai.com/v1/images/generations"); // bu eklenecek

JsonNode response = webClient.post()
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("""
                {
                  "prompt": "%s",
                  "n": %d,
                  "size": "%s",
                  "response_format": "b64_json"
                }
                """.formatted(
                request.getPrompt().replace("\"", "\\\""),
                request.getN(),
                request.getSize() != null ? request.getSize() : "1024x1024"
        ))
        .retrieve()
        .bodyToMono(JsonNode.class)
        .block();

List<JsonNode> dataList = response.get("data").findValues("b64_json");
List<OpenAiImage> singleImageList = dataList.stream().map(node -> {
    OpenAiImage image = new OpenAiImage();
    image.setB64Json(node.asText());
    return image;
}).collect(Collectors.toList());

return processAndStoreImages(singleImageList);

            }
            if (StringUtils.hasText(request.getReferenceImageIdentifier())) {
                logger.debug("Executing OpenAI Image Edit using WebClient...");

                Resource referenceResource = storageService.loadAsResource(request.getReferenceImageIdentifier());
                byte[] imageBytes;
                try (InputStream inputStream = referenceResource.getInputStream()) {
                    imageBytes = inputStream.readAllBytes();
                } catch (IOException e) {
                    logger.error("Failed to read reference image 	{}	 for AI edit.", request.getReferenceImageIdentifier(), e);
                    throw new BadRequestException("Referans görsel okunamadı: " + request.getReferenceImageIdentifier());
                }

                WebClient editClient = createWebClient("https://api.openai.com/v1/images/edits"); // yerine bu gelecek


                MultiValueMap<String, Object> formData = new LinkedMultiValueMap<>();
                formData.add("prompt", request.getPrompt());
                formData.add("n", request.getN());
                formData.add("size", request.getSize() != null ? request.getSize() : "1024x1024");
                formData.add("response_format", "b64_json");
                formData.add("image", new ByteArrayResource(imageBytes) {
                    @Override
                    public String getFilename() {
                        return "reference.png";
                    }
                });

                JsonNode editResponse = editClient.post()
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .bodyValue(formData)
                        .retrieve()
                        .bodyToMono(JsonNode.class)
                        .block();

                List<JsonNode> dataList = editResponse.get("data").findValues("b64_json");
                generatedApiImages = dataList.stream().map(node -> {
                    OpenAiImage img = new OpenAiImage();
                    img.setB64Json(node.asText());
                    return img;
                }).collect(Collectors.toList());

            } else {
                logger.debug("Executing OpenAI Image Generation using WebClient...");

                WebClient webClient = createWebClient("https://api.openai.com/v1/images/generations"); // yerine bu gelecek

                JsonNode response = webClient.post()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue("""
                                {
                                  "prompt": "%s",
                                  "n": %d,
                                  "size": "%s",
                                  "response_format": "b64_json"
                                }
                                """.formatted(
                                request.getPrompt().replace("\"", "\\\""),
                                request.getN(),
                                request.getSize() != null ? request.getSize() : "1024x1024"
                        ))
                        .retrieve()
                        .bodyToMono(JsonNode.class)
                        .block();

                List<JsonNode> dataList = response.get("data").findValues("b64_json");
                generatedApiImages = dataList.stream().map(node -> {
                    OpenAiImage img = new OpenAiImage();
                    img.setB64Json(node.asText());
                    return img;
                }).collect(Collectors.toList());
            }

            logger.info("Received {} image data object(s) from OpenAI API.", generatedApiImages.size());
            return processAndStoreImages(generatedApiImages);

        } catch (ResourceNotFoundException | BadRequestException e) {
            throw e;
        } catch (Exception e) {
            logger.error("OpenAI API call failed for User ID: {}. Error Type: {}, Message: {}",
                    requestingUser.getId(), e.getClass().getSimpleName(), e.getMessage(), e);

            String userMessage = "AI görseli üretilirken beklenmedik bir hata oluştu.";
            if (e.getMessage() != null) {
                if (e.getMessage().contains("content_policy_violation")) {
                    userMessage = "İsteğiniz içerik politikası nedeniyle reddedildi.";
                    throw new BadRequestException(userMessage);
                } else if (e.getMessage().contains("billing") || e.getMessage().contains("quota")) {
                    userMessage = "AI servisinde geçici bir sorun veya limit aşımı var.";
                    throw new RuntimeException(userMessage);
                } else if (e.getMessage().contains("Invalid prompt") || e.getMessage().contains("invalid size")) {
                    userMessage = "AI servisine gönderilen istek geçersiz: " + e.getMessage();
                    throw new BadRequestException(userMessage);
                } else {
                    userMessage = "AI görsel servisiyle iletişimde hata: " + e.getMessage();
                }
            }
            throw new RuntimeException(userMessage, e);
        }
    }
    private List<String> processAndStoreImages(@NonNull List<OpenAiImage> images) {
        Objects.requireNonNull(images, "Image list cannot be null");
        List<String> storedUrls = new ArrayList<>();
    
        for (OpenAiImage img : images) {
            if (StringUtils.hasText(img.getB64Json())) {
                // bu eklenecek
                try {
                    byte[] imageBytes = Base64.getDecoder().decode(img.getB64Json());
                    String filename = storageService.store(imageBytes, "ai_generated_images", "png");
                    String url = storageService.generateUrl(filename);
                    storedUrls.add(url);
                    logger.info("Stored generated AI image as 	{}	 at URL: {}", filename, url);
                } catch (IllegalArgumentException e) {
                    logger.error("Failed to decode base64 image data: {}", e.getMessage());
                } catch (Exception e) {
                    logger.error("Failed to store or generate URL for base64 image: {}", e.getMessage(), e);
                }
            } else if (StringUtils.hasText(img.getUrl())) {
                // bu eklenecek
                try {
                    String imageUrl = img.getUrl();
                    byte[] imageBytes = WebClient.create()
                            .get()
                            .uri(imageUrl)
                            .retrieve()
                            .bodyToMono(byte[].class)
                            .block();
                    String filename = storageService.store(imageBytes, "ai_generated_images", "png");
                    String storedUrl = storageService.generateUrl(filename);
                    storedUrls.add(storedUrl);
                    logger.info("Downloaded and stored image from OpenAI URL 	{}	 as 	{}	", imageUrl, filename);
                } catch (Exception e) {
                    logger.error("Failed to download or store image from OpenAI URL: {}", e.getMessage(), e);
                }
            } else {
                // bu eklenecek
                logger.warn("Received an Image object from OpenAI with neither b64_json nor url.");
            }
        }
    
        if (storedUrls.isEmpty() && !images.isEmpty()) {
            logger.error("Failed to process or store ANY image received from OpenAI (Received {} image objects initially).", images.size());
            throw new RuntimeException("AI tarafından üretilen görseller saklanamadı.");
        }
    
        return storedUrls;
    }
    
}

