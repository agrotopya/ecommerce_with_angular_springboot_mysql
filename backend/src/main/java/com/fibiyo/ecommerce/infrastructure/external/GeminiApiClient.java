package com.fibiyo.ecommerce.infrastructure.external;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

// import java.io.IOException; // Artık doğrudan IOException fırlatmıyoruz
import com.fasterxml.jackson.databind.ObjectMapper; 
import com.fasterxml.jackson.databind.JsonNode; 

@Component
public class GeminiApiClient {

    private static final Logger logger = LoggerFactory.getLogger(GeminiApiClient.class);

    private final WebClient webClient;
    private final String apiKey;
    private final String modelName; 
    private final ObjectMapper objectMapper; 

    public GeminiApiClient(WebClient.Builder webClientBuilder,
                           @Value("${gemini.api.key}") String apiKey,
                           @Value("${gemini.model.name:gemini-1.5-flash-latest}") String modelName) { 
        this.apiKey = apiKey;
        this.modelName = modelName;
        this.webClient = webClientBuilder.baseUrl("https://generativelanguage.googleapis.com/v1beta/models/" + this.modelName).build();
        this.objectMapper = new ObjectMapper();
        logger.info("GeminiApiClient (REST) initialized for model: {}", this.modelName);
        if (apiKey == null || apiKey.trim().isEmpty() || apiKey.equals("YOUR_GEMINI_API_KEY_REPLACE_ME") || apiKey.startsWith("AIzaSy")) { // AIzaSy ile başlayanlar genellikle placeholder veya test anahtarlarıdır
            logger.warn("Gemini API key is not configured, is a placeholder, or looks like a test key. API calls will likely fail or be restricted.");
        }
    }

    public String generateSummary(String reviewsText) {
        logger.debug("Attempting to generate summary via REST API for text: {}...", reviewsText.substring(0, Math.min(reviewsText.length(), 100)));

        String promptText = "Aşağıdaki e-ticaret ürünü yorumlarını analiz et. " +
                            "Bu yorumlara dayanarak, ürünün en çok beğenilen 2-3 özelliğini, " +
                            "en çok eleştirilen 2-3 yönünü ve genel bir kullanıcı duygu özetini (örn: çoğunlukla olumlu, karışık, olumsuz) kısa ve net bir şekilde belirt. " +
                            "Özet, doğrudan alıntı yapmak yerine genel çıkarımları yansıtmalıdır. En fazla 3 cümlede hepsini açıklayın. Net olun. Yorum yapıyormuş gibi. " +
                            "YORUMLAR:\n" + reviewsText;

        String requestBody = String.format("{\"contents\":[{\"parts\":[{\"text\": \"%s\"}]}]}", escapeJson(promptText));

        try {
            logger.debug("Sending request to Gemini API. Endpoint: :generateContent?key=...");
            String responseJson = webClient.post()
                    .uri(uriBuilder -> uriBuilder.path(":generateContent").queryParam("key", this.apiKey).build())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(); 

            if (responseJson == null) {
                logger.error("Received null response from Gemini API.");
                throw new RuntimeException("Null response from Gemini API");
            }
            
            logger.debug("Raw Gemini API response: {}", responseJson);

            JsonNode rootNode = objectMapper.readTree(responseJson);
            JsonNode candidates = rootNode.path("candidates");
            if (candidates.isArray() && !candidates.isEmpty()) {
                JsonNode firstCandidate = candidates.get(0);
                JsonNode content = firstCandidate.path("content");
                JsonNode parts = content.path("parts");
                if (parts.isArray() && !parts.isEmpty()) {
                    String summary = parts.get(0).path("text").asText();
                    logger.info("Successfully received and parsed summary from Gemini API.");
                    logger.debug("Parsed Gemini API summary: {}", summary);
                    return summary;
                }
            }
            // Check for error response from Gemini API
            JsonNode errorNode = rootNode.path("error");
            if (!errorNode.isMissingNode()) {
                String errorMessage = errorNode.path("message").asText("Unknown Gemini API error");
                logger.error("Gemini API returned an error: {}", errorMessage);
                throw new RuntimeException("Gemini API error: " + errorMessage);
            }
            
            logger.warn("Could not extract summary text from Gemini API response: {}", responseJson);
            return "AI özeti alınamadı (yanıt formatı beklenmedik).";

        } catch (Exception e) {
            logger.error("Error during Gemini API REST call: {}", e.getMessage(), e);
            throw new RuntimeException("Error communicating with Gemini API via REST: " + e.getMessage(), e);
        }
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\b", "\\b")
                   .replace("\f", "\\f")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
}
