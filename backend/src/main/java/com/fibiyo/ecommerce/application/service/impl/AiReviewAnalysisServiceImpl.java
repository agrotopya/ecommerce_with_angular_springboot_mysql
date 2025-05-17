package com.fibiyo.ecommerce.application.service.impl;

import com.fibiyo.ecommerce.application.exception.ResourceNotFoundException;
import com.fibiyo.ecommerce.application.service.AiReviewAnalysisService;
import com.fibiyo.ecommerce.domain.entity.Product;
import com.fibiyo.ecommerce.domain.entity.Review;
import com.fibiyo.ecommerce.infrastructure.external.GeminiApiClient; // Eklendi
import com.fibiyo.ecommerce.infrastructure.persistence.repository.ProductRepository;
import com.fibiyo.ecommerce.infrastructure.persistence.repository.ReviewRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AiReviewAnalysisServiceImpl implements AiReviewAnalysisService {

    private static final Logger logger = LoggerFactory.getLogger(AiReviewAnalysisServiceImpl.class);

    private final ProductRepository productRepository;
    private final ReviewRepository reviewRepository;
    private final GeminiApiClient geminiApiClient; // Eklendi

    @Autowired
    public AiReviewAnalysisServiceImpl(ProductRepository productRepository,
                                       ReviewRepository reviewRepository,
                                       GeminiApiClient geminiApiClient) { // Eklendi
        this.productRepository = productRepository;
        this.reviewRepository = reviewRepository;
        this.geminiApiClient = geminiApiClient; // Eklendi
    }

    @Override
    @Transactional
    public String generateAndSaveReviewSummary(Long productId) {
        logger.info("Attempting to generate AI review summary for product ID: {}", productId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> {
                    logger.error("Product not found with ID: {}", productId);
                    return new ResourceNotFoundException("Product not found with id: " + productId);
                });

        // Sadece onaylanmış yorumları alalım, örneğin son 100 yorum veya hepsi
        // Pageable kullanarak tüm onaylanmış yorumları çekebiliriz, ancak çok fazla yorum varsa dikkatli olunmalı.
        // Şimdilik basitlik adına son N yorumu alabiliriz veya tümünü.
        // Burada tüm onaylanmış yorumları alıyoruz.
        List<Review> approvedReviews = reviewRepository.findByProductIdAndIsApprovedTrue(productId, 
            PageRequest.of(0, 200, Sort.by(Sort.Direction.DESC, "createdAt"))).getContent(); // Örnek: Son 200 yorum

        if (approvedReviews.isEmpty()) {
            logger.info("No approved reviews found for product ID: {}. No summary will be generated.", productId);
            product.setAiReviewSummary("Bu ürün için henüz yeterli yorum bulunmamaktadır."); // Veya null/boş bırakılabilir
            productRepository.save(product);
            return product.getAiReviewSummary();
        }

        // Yorumları birleştirerek Gemini'ye gönderilecek metni oluştur
        String reviewsText = approvedReviews.stream()
                .map(review -> "Puan: " + review.getRating() + "/5 - Yorum: " + (review.getComment() != null ? review.getComment() : "Yorum yok"))
                .collect(Collectors.joining("\n---\n"));

        logger.debug("Combined reviews text for product ID {}:\n{}", productId, reviewsText);

        String summary;
        try {
            logger.info("Calling Gemini API to generate summary for product ID: {}", productId);
            summary = geminiApiClient.generateSummary(reviewsText);
            if (summary == null || summary.trim().isEmpty()) {
                logger.warn("Gemini API returned an empty or null summary for product ID: {}. Using a default message.", productId);
                summary = "Bu ürün için AI tarafından oluşturulmuş bir özet şu anda mevcut değil.";
            }
        } catch (Exception e) {
            logger.error("Error calling Gemini API for product ID {}: {}", productId, e.getMessage(), e);
            summary = "Yorum özeti oluşturulurken bir hata oluştu. Lütfen daha sonra tekrar deneyin.";
        }

        product.setAiReviewSummary(summary);
        productRepository.save(product);
        logger.info("Successfully generated and saved AI review summary for product ID: {}", productId);

        return summary;
    }
}
