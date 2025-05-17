package com.fibiyo.ecommerce.application.service.impl;

import com.fibiyo.ecommerce.application.dto.AiImageGenerationRequest;
import com.fibiyo.ecommerce.application.dto.OpenAiImage;
import com.fibiyo.ecommerce.application.exception.BadRequestException;
import com.fibiyo.ecommerce.application.exception.ResourceNotFoundException;
import com.fibiyo.ecommerce.application.service.AiService;
import com.fibiyo.ecommerce.application.service.StorageService;
import com.fibiyo.ecommerce.domain.entity.User;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class OpenAiServiceImpl implements AiService {

    private static final Logger logger = LoggerFactory.getLogger(OpenAiServiceImpl.class);

    private final StorageService storageService;
    @Value("${openai.api.key}")
    private String openAiApiKey;

    @Autowired
    public OpenAiServiceImpl(StorageService storageService) {
        this.storageService = storageService;
    }

    private WebClient createWebClient() { // baseUrl parametresi kaldırıldı, her istekte tam URL kullanılacak
        return WebClient.builder()
                .defaultHeader("Authorization", "Bearer " + openAiApiKey)
                .exchangeStrategies(ExchangeStrategies.builder()
                    .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)) // 10MB
                    .build())
                .build();
    }

    @Override
    public List<String> generateProductImage(@NonNull AiImageGenerationRequest request, @NonNull User requestingUser) {
        String requestedModel = StringUtils.hasText(request.getModel()) ? request.getModel().toLowerCase() : "gpt-image-1";
        logger.info("AI Image Request: User ID: {}, Model: {}, Prompt: '{}...', RefImgId: {}",
                    requestingUser.getId(), requestedModel,
                    request.getPrompt() != null ? request.getPrompt().substring(0, Math.min(request.getPrompt().length(), 30)) : "N/A",
                    request.getReferenceImageIdentifier());

        try {
            WebClient webClient = createWebClient();
            String endpointUrl;
            WebClient.ResponseSpec responseSpec; // retrieve() sonrası ResponseSpec alırız

            ObjectMapper objectMapper = new ObjectMapper();
            ObjectNode baseRequestNode = objectMapper.createObjectNode();
            baseRequestNode.put("model", requestedModel);
            baseRequestNode.put("n", request.getN() == null ? 1 : Math.max(1, Math.min(request.getN(), 10)));
            baseRequestNode.put("size", StringUtils.hasText(request.getSize()) ? request.getSize() : "1024x1024");

            if ("dall-e-2".equals(requestedModel) || "dall-e-3".equals(requestedModel)) {
                baseRequestNode.put("response_format", "b64_json");
            }

            if (StringUtils.hasText(request.getReferenceImageIdentifier())) {
                logger.info("Executing OpenAI Image EDIT/VARIATION for User ID: {} with model {}...", requestingUser.getId(), requestedModel);
                if (!"dall-e-2".equals(requestedModel)) {
                    logger.warn("Image editing/variation is typically used with 'dall-e-2'. The provided model '{}' may not support it or behave as expected.", requestedModel);
                }
                endpointUrl = "https://api.openai.com/v1/images/edits";

                Path imagePath = storageService.load(request.getReferenceImageIdentifier());
                File imageFile = imagePath.toFile();
                if (!imageFile.exists()) {
                    throw new ResourceNotFoundException("Reference image not found at: " + request.getReferenceImageIdentifier());
                }

                MultipartBodyBuilder builder = new MultipartBodyBuilder();
                builder.part("image", new FileSystemResource(imageFile));
                if (StringUtils.hasText(request.getPrompt())) {
                    builder.part("prompt", request.getPrompt());
                }
                builder.part("model", requestedModel);
                builder.part("n", String.valueOf(baseRequestNode.get("n").asInt()));
                builder.part("size", baseRequestNode.get("size").asText());
                if (baseRequestNode.has("response_format")) {
                    builder.part("response_format", baseRequestNode.get("response_format").asText());
                }
                
                MultiValueMap<String, HttpEntity<?>> multipartBody = builder.build();
                logger.debug("OpenAI API Edit/Variation Request for {}: FormData", requestedModel);
                
                responseSpec = webClient.post()
                                     .uri(endpointUrl)
                                     .contentType(MediaType.MULTIPART_FORM_DATA)
                                     .body(BodyInserters.fromMultipartData(multipartBody))
                                     .retrieve();
            } else {
                logger.info("Executing OpenAI Image GENERATION (text-to-image) for User ID: {} with model {}...", requestingUser.getId(), requestedModel);
                endpointUrl = "https://api.openai.com/v1/images/generations";
                
                baseRequestNode.put("prompt", request.getPrompt());
                String requestBodyJson = objectMapper.writeValueAsString(baseRequestNode);
                logger.debug("OpenAI API Generation Request Body for {}: {}", requestedModel, requestBodyJson);

                responseSpec = webClient.post()
                                     .uri(endpointUrl)
                                     .contentType(MediaType.APPLICATION_JSON)
                                     .bodyValue(requestBodyJson)
                                     .retrieve();
            }

            JsonNode response = responseSpec
                    .bodyToMono(JsonNode.class)
                    .doOnSuccess(res -> logger.info("Raw OpenAI API response for model {}: {}", requestedModel, res != null ? res.toString() : "null"))
                    .doOnError(err -> logger.error("Error from OpenAI API (model {}): ", requestedModel, err))
                    .block();

            if (response == null || !response.has("data")) {
                logger.error("OpenAI API response for model {} does not contain 'data' field or response is null. Response: {}",
                             requestedModel, response != null ? response.toString() : "null");
                throw new RuntimeException("AI görsel servisinden (" + requestedModel + ") beklenen formatta yanıt alınamadı.");
            }

            List<JsonNode> dataList = response.get("data").findValues("b64_json");
            if (dataList.isEmpty() && response.get("data").isArray()) {
                logger.warn("OpenAI API response 'data' field for {} did not yield 'b64_json'. Checking for 'url'. Full data: {}", requestedModel, response.get("data").toString());
                dataList = response.get("data").findValues("url");
            }
             if (dataList.isEmpty() && response.get("data").isArray() && response.get("data").get(0)!=null && response.get("data").get(0).has("b64_json")) {
                 List<JsonNode> directDataList = new ArrayList<>();
                 for(JsonNode item : response.get("data")){
                     if(item.has("b64_json")) directDataList.add(item.get("b64_json"));
                 }
                 dataList = directDataList;
                 if(!dataList.isEmpty()) logger.info("Successfully extracted b64_json data using direct path access.");
             }

            List<OpenAiImage> generatedApiImages = dataList.stream().map(node -> {
                OpenAiImage img = new OpenAiImage();
                if(node.isTextual()){
                    String textValue = node.asText();
                    if (textValue.startsWith("http://") || textValue.startsWith("https://")) {
                        img.setUrl(textValue);
                    } else {
                        img.setB64Json(textValue);
                    }
                }
                return img;
            }).collect(Collectors.toList());

            logger.info("Received {} image data object(s) from OpenAI API (model {}).", generatedApiImages.size(), requestedModel);
            return processAndStoreImages(generatedApiImages);

        } catch (ResourceNotFoundException | BadRequestException e) {
            logger.error("AI Image Generation failed due to known exception: {}", e.getMessage());
            throw e;
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
            throw new RuntimeException(userMessage, wcre);
        } catch (Exception e) {
            logger.error("Unexpected error during AI Image Generation for User ID: {}. Model: {}. Error Type: {}, Message: {}",
                    requestingUser.getId(), requestedModel, e.getClass().getSimpleName(), e.getMessage(), e);
            throw new RuntimeException("AI görseli üretilirken beklenmedik bir sunucu hatası oluştu.", e);
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
                } catch (IllegalArgumentException e) {
                    logger.error("Failed to decode base64 image data: {}", e.getMessage());
                } catch (Exception e) {
                    logger.error("Failed to store or generate URL for base64 image: {}", e.getMessage(), e);
                }
            } else if (StringUtils.hasText(img.getUrl())) {
                try {
                    String imageUrl = img.getUrl();
                    logger.info("Attempting to download image from OpenAI URL: {}", imageUrl);
                    byte[] imageBytes = WebClient.create()
                            .get()
                            .uri(imageUrl)
                            .retrieve()
                            .bodyToMono(byte[].class)
                            .block();

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
            } else {
                logger.warn("Received an Image object from OpenAI with neither b64_json nor url.");
            }
        }

        if (storedUrls.isEmpty() && !images.isEmpty()) {
            logger.error("Failed to process or store ANY image received from OpenAI (Received {} image objects initially).", images.size());
        }

        return storedUrls;
    }
}
