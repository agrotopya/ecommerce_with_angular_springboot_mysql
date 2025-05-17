package com.fibiyo.ecommerce.application.service;

public interface AiReviewAnalysisService {

    /**
     * Generates (or updates) an AI-powered summary of reviews for a given product.
     * The summary is then typically stored in the Product entity.
     *
     * @param productId The ID of the product for which to generate the review summary.
     * @return The generated (or updated) review summary string.
     * @throws com.fibiyo.ecommerce.application.exception.ResourceNotFoundException if the product is not found.
     * @throws com.fibiyo.ecommerce.application.exception.GeminiApiException if there's an error communicating with Gemini API.
     */
    String generateAndSaveReviewSummary(Long productId);

    /**
     * Potentially, a method to trigger summary generation for all products or products needing update.
     * void triggerBulkSummaryGeneration();
     */

    // Belki doğrudan Product entity'sinden alınacağı için bu metod gerekmeyebilir,
    // ama servis üzerinden bir erişim noktası sunmak istenirse eklenebilir.
    // String getSavedReviewSummary(Long productId);
}
