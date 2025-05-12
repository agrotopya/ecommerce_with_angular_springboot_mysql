package com.fibiyo.ecommerce.application.service.impl;

import com.fibiyo.ecommerce.application.dto.ReviewRequest;
import com.fibiyo.ecommerce.application.dto.ReviewResponse;
import com.fibiyo.ecommerce.application.exception.BadRequestException;
import com.fibiyo.ecommerce.application.exception.ForbiddenException;
import com.fibiyo.ecommerce.application.exception.ResourceNotFoundException;
import com.fibiyo.ecommerce.application.mapper.ReviewMapper;
import com.fibiyo.ecommerce.application.service.ReviewService;
import com.fibiyo.ecommerce.domain.entity.Product;
import com.fibiyo.ecommerce.domain.entity.Review;
import com.fibiyo.ecommerce.domain.entity.User;
import com.fibiyo.ecommerce.domain.enums.OrderStatus;
import com.fibiyo.ecommerce.domain.enums.Role;
import com.fibiyo.ecommerce.infrastructure.persistence.repository.OrderRepository;
import com.fibiyo.ecommerce.infrastructure.persistence.repository.ProductRepository;
import com.fibiyo.ecommerce.infrastructure.persistence.repository.ReviewRepository;
import com.fibiyo.ecommerce.infrastructure.persistence.repository.UserRepository;
import com.fibiyo.ecommerce.infrastructure.persistence.specification.ReviewSpecifications;

// OrderRepository (satın alma kontrolü için) gerekebilir
import static com.fibiyo.ecommerce.infrastructure.persistence.specification.ReviewSpecifications.*; // Specification'ı import et
import org.springframework.data.jpa.domain.Specification; // Specification import

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

// Review Specifications
import static com.fibiyo.ecommerce.infrastructure.persistence.specification.ReviewSpecifications.*;


@Service
public class ReviewServiceImpl implements ReviewService {

    private static final Logger logger = LoggerFactory.getLogger(ReviewServiceImpl.class);

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ReviewMapper reviewMapper;
    private final OrderRepository orderRepository;

     // Opsiyonel: private final OrderRepository orderRepository;

     // Helper Methods (getCurrentUser etc. önceki servislerden alınabilir)
      private User getCurrentUser() {
          Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
         if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
             throw new ForbiddenException("Erişim için kimlik doğrulaması gerekli.");
          }
         String username = authentication.getName();
         return userRepository.findByUsername(username)
                  .orElseThrow(() -> new ResourceNotFoundException("Current user not found in database: " + username));
      }
    //    private void checkAdminRole() {
    //       User currentUser = getCurrentUser();
    //       if (currentUser.getRole() != Role.ADMIN) {
    //          throw new ForbiddenException("Bu işlem için Admin yetkisi gerekli.");
    //      }
    //    } methodlarda kullanıyorduk fakat reviewcontrollerda zaten preauth ile kontrol ediliyor


    @Autowired
    public ReviewServiceImpl(ReviewRepository reviewRepository, ProductRepository productRepository, UserRepository userRepository, ReviewMapper reviewMapper, OrderRepository orderRepository) {
        this.reviewRepository = reviewRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.reviewMapper = reviewMapper;
        this.orderRepository = orderRepository; // OrderRepository'yi ekle
    }

    @Override
    @Transactional
    public ReviewResponse addReview(Long productId, ReviewRequest reviewRequest) {
        User customer = getCurrentUser(); // Aktif kullanıcıyı al
        logger.info("Customer ID: {} attempting to add review for product ID: {}", customer.getId(), productId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        // Kullanıcı bu ürüne daha önce yorum yapmış mı? (Unique constraint ile DB seviyesinde de kontrol ediliyor)
        if (reviewRepository.existsByProductIdAndCustomerId(productId, customer.getId())) {
            logger.warn("Customer ID: {} already reviewed product ID: {}", customer.getId(), productId);
            throw new BadRequestException("Bu ürüne zaten yorum yaptınız.");
        }

     

        Review review = reviewMapper.toReview(reviewRequest);
        review.setProduct(product);
        review.setCustomer(customer);
        review.setApproved(true); // Varsayılan olarak onaylı (İş kuralına göre false olabilir)

      // Opsiyonel: Kullanıcının bu ürünü sipariş edip etmediğini kontrol et
orderRepository.findFirstByCustomerIdAndOrderItems_Product_IdAndStatusOrderByOrderDateDesc(
    customer.getId(), productId, OrderStatus.DELIVERED
).ifPresent(review::setOrder);

        Review savedReview = reviewRepository.save(review);
        logger.info("Review added successfully with ID: {} for product ID: {} by customer ID: {}", savedReview.getId(), productId, customer.getId());

         // Ürünün ortalama puanını ve yorum sayısını güncelle
        updateProductRatingAndCount(productId);


        return reviewMapper.toReviewResponse(savedReview);
    }

    @Override
    @Transactional
    public void deleteMyReview(Long reviewId) {
        User customer = getCurrentUser();
        logger.warn("Customer ID: {} attempting to delete review ID: {}", customer.getId(), reviewId);

        Review review = reviewRepository.findById(reviewId)
                 .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + reviewId));

        // Yorumun sahibi mi kontrol et
        if (!review.getCustomer().getId().equals(customer.getId())) {
            throw new ForbiddenException("Bu yorumu silme yetkiniz yok.");
        }

        Long productId = review.getProduct().getId(); // Puanı güncellemek için Product ID'yi al
        reviewRepository.delete(review);
         logger.info("Review ID: {} deleted successfully by customer ID: {}", reviewId, customer.getId());

        // Ürünün ortalama puanını ve yorum sayısını güncelle
        updateProductRatingAndCount(productId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponse> findReviewsByProduct(Long productId, Pageable pageable, boolean approvedOnly) {
         logger.debug("Finding reviews for product ID: {}. ApprovedOnly: {}", productId, approvedOnly);
        // Ürün var mı kontrolü (opsiyonel ama iyi pratik)
        if (!productRepository.existsById(productId)) {
             throw new ResourceNotFoundException("Product not found with id: " + productId);
        }

        Page<Review> reviewPage;
        if (approvedOnly) {
            // Sadece onaylı yorumları getir
             // Specification kullanmak daha esnek olabilir ama şimdilik derived query yeterli.
             reviewPage = reviewRepository.findByProductIdAndIsApprovedTrue(productId, pageable);
         } else {
            // Tüm yorumları getir (Admin vs. için)
            reviewPage = reviewRepository.findByProductId(productId, pageable);
         }

        return reviewPage.map(reviewMapper::toReviewResponse);
    }

    // Ürünün rating ve count alanlarını güncelleyen private metot
    // Ürün puan/sayısını güncelleyen private metot (Önceki adımda eklenmişti, tekrar kontrol et)
    private void updateProductRatingAndCount(Long productId) {
        try {
            BigDecimal avgRating = reviewRepository.calculateAverageRatingByProductId(productId);
            long reviewCount = reviewRepository.countByProductIdAndIsApprovedTrue(productId);
            productRepository.findById(productId).ifPresent(product -> { // ifPresent ile null check daha temiz
                product.setAverageRating(avgRating != null ? avgRating : BigDecimal.ZERO);
                product.setReviewCount((int) reviewCount);
                productRepository.save(product); // Transaction içinde olduğumuz için save yeterli
                logger.info("Updated product ID: {} rating to {} and review count to {}", productId, product.getAverageRating(), product.getReviewCount());
            });
        } catch (Exception e) {
            logger.error("Error updating product rating/count for product ID {}: {}", productId, e.getMessage(), e);
        }
    }

    // --- Admin Operations Implementation ---

  
    @Override
    @Transactional(readOnly = true) // Sadece okuma işlemi
    public Page<ReviewResponse> findAllReviews(Pageable pageable, Boolean isApproved) {
         // 1. Yetki Kontrolü
          logger.debug("Admin fetching all reviews. IsApproved filter: {}", isApproved);

         // 2. Specification ile Filtreleme
         Specification<Review> spec = Specification.where(null); // Başlangıç filtresi (tümünü getir)
         if (isApproved != null) {
              // ReviewSpecifications içindeki isApproved metodunu kullan
              spec = spec.and(ReviewSpecifications.isApproved(isApproved));
         }
         // İleride product ID, customer ID veya tarih aralığı filtreleri de eklenebilir.
         // if(productId != null) { spec = spec.and(ReviewSpecifications.hasProduct(productId)); }

         // 3. Repository Çağrısı
          Page<Review> reviewPage = reviewRepository.findAll(spec, pageable); // JpaSpecificationExecutor sayesinde çalışır

         // 4. DTO Dönüşümü
          return reviewPage.map(reviewMapper::toReviewResponse); // Sayfayı DTO sayfasına dönüştür
    }

    @Override
    @Transactional // Veri güncelleme
    public ReviewResponse approveReview(Long reviewId) {
        // 1. Yetki Kontrolü
        logger.info("Admin approving review ID: {}", reviewId);

         // 2. Review'ı Bul
         Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + reviewId));

         // 3. İşlemi Yap ve Kaydet
         boolean needsRatingUpdate = !review.isApproved(); // Önceden onaylı değilse puanı güncellemek gerekecek
         if(needsRatingUpdate){
              review.setApproved(true);
             review = reviewRepository.save(review); // Güncellenmiş review'ı alalım
             logger.info("Review ID: {} approved by admin.", reviewId);
             // 4. Ürün Puanını Güncelle (Eğer durum değiştiyse)
             if(review.getProduct() != null) {
                 updateProductRatingAndCount(review.getProduct().getId());
             }
          } else {
              logger.warn("Review ID: {} was already approved.", reviewId); // Zaten onaylıysa tekrar işlem yapma
          }

        // 5. Yanıtı Dön
        return reviewMapper.toReviewResponse(review);
    }
    @Override
    @Transactional // Veri güncelleme
    public ReviewResponse rejectReview(Long reviewId) {
         // 1. Yetki Kontrolü
         logger.warn("Admin rejecting (disapproving) review ID: {}", reviewId);

        // 2. Review'ı Bul
        Review review = reviewRepository.findById(reviewId)
               .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + reviewId));

         // 3. İşlemi Yap ve Kaydet
        boolean needsRatingUpdate = review.isApproved(); // Önceden onaylı ise puanı güncellemek gerekecek
        if (needsRatingUpdate) {
            review.setApproved(false); // Onayı kaldır
             review = reviewRepository.save(review);
            logger.info("Review ID: {} disapproved by admin.", reviewId);
             // 4. Ürün Puanını Güncelle (Eğer durum değiştiyse)
            if(review.getProduct() != null){
                 updateProductRatingAndCount(review.getProduct().getId());
            }
        } else {
             logger.warn("Review ID: {} was already disapproved.", reviewId); // Zaten onaylı değilse tekrar işlem yapma
        }


         // 5. Yanıtı Dön
        return reviewMapper.toReviewResponse(review);
    }



    @Override
    @Transactional // Veri silme
    public void deleteReviewByAdmin(Long reviewId) {
        // 1. Yetki Kontrolü
         logger.warn("Admin deleting review ID: {}", reviewId);

        // 2. Review'ı Bul (Silmeden önce varlığını kontrol etmek iyi pratiktir)
        Review review = reviewRepository.findById(reviewId)
               .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + reviewId));

        Long productId = review.getProduct() != null ? review.getProduct().getId() : null; // Ürün ID'sini al (varsa)

         // 3. Silme İşlemi
        reviewRepository.delete(review);
         logger.info("Review ID: {} deleted by admin.", reviewId);

         // 4. Ürün Puanını Güncelle (Eğer silinen yorum onaylı idiyse ve ürün varsa)
        if(productId != null && review.isApproved()){
             updateProductRatingAndCount(productId);
         }
    }

}

