package com.fibiyo.ecommerce.application.service.impl;

import com.fibiyo.ecommerce.application.dto.*;
import com.fibiyo.ecommerce.application.exception.*;
import com.fibiyo.ecommerce.application.mapper.ProductMapper;
import com.fibiyo.ecommerce.application.service.AiService;
import com.fibiyo.ecommerce.application.service.SellerProductService;
import com.fibiyo.ecommerce.application.service.StorageService;
import com.fibiyo.ecommerce.application.util.SlugUtils;
import com.fibiyo.ecommerce.domain.entity.Category;
import com.fibiyo.ecommerce.domain.entity.Product;
import com.fibiyo.ecommerce.domain.entity.ProductImage;
import com.fibiyo.ecommerce.domain.entity.User;
import com.fibiyo.ecommerce.domain.enums.Role;
import com.fibiyo.ecommerce.domain.enums.SubscriptionType;
import com.fibiyo.ecommerce.infrastructure.persistence.repository.CategoryRepository;
import com.fibiyo.ecommerce.infrastructure.persistence.repository.ProductRepository;
import com.fibiyo.ecommerce.infrastructure.persistence.repository.UserRepository;
import com.fibiyo.ecommerce.infrastructure.persistence.specification.ProductSpecifications;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Objects;
import java.util.ArrayList; // ArrayList importu eklendi (gerçi zaten vardı)

@Service
public class SellerProductServiceImpl implements SellerProductService {

    private static final Logger logger = LoggerFactory.getLogger(SellerProductServiceImpl.class);

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final ProductMapper productMapper;
    private final StorageService storageService;
    private final AiService aiService;

    @Autowired
    public SellerProductServiceImpl(ProductRepository productRepository, CategoryRepository categoryRepository, UserRepository userRepository, ProductMapper productMapper, StorageService storageService, AiService aiService) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.productMapper = productMapper;
        this.storageService = storageService;
        this.aiService = aiService;
    }

    private User getCurrentSeller() {
         Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
         if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
             throw new ForbiddenException("Bu işlem için Seller olarak giriş yapmalısınız.");
          }
          String username = authentication.getName();
          User user = userRepository.findByUsername(username)
                 .orElseThrow(() -> new ResourceNotFoundException("Current user not found: " + username));
         if (user.getRole() != Role.SELLER) {
              logger.warn("User {} (Role: {}) attempted a seller-only operation.", username, user.getRole());
             throw new ForbiddenException("Bu işlem için Seller yetkisi gereklidir.");
          }
         return user;
     }

     private Product findProductByIdAndCheckOwnership(Long productId) {
         User currentSeller = getCurrentSeller();
          Product product = productRepository.findById(productId)
                 .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));
         if (!Objects.equals(product.getSeller().getId(), currentSeller.getId())) {
            logger.warn("Seller ID: {} attempted to access product ID: {} owned by Seller ID: {}",
                       currentSeller.getId(), productId, product.getSeller().getId());
            throw new ForbiddenException("Bu ürüne erişim yetkiniz yok.");
         }
         return product;
     }

     private void validateUniqueSlug(String slug, Long currentProductId) {
          productRepository.findBySlug(slug).ifPresent(existingProduct -> {
                if (!Objects.equals(existingProduct.getId(), currentProductId)) {
                   logger.warn("Slug {} already exists for product ID: {}", slug, existingProduct.getId());
                    throw new BadRequestException("Bu ürün adı veya benzeri başka bir ürün tarafından kullanılıyor. Lütfen adı değiştirin.");
                }
            });
       }

       private String extractFilenameFromUrl(String url){
          if (url == null || !url.contains("/")) return null;
          try {
              return url.substring(url.lastIndexOf('/') + 1);
           } catch (Exception e) {
              logger.warn("Could not extract filename from URL: {}", url);
              return null;
           }
       }

        private void deleteOldFile(String oldFilename){
             if(StringUtils.hasText(oldFilename)){
                 try {
                     storageService.deleteFile(oldFilename);
                      logger.info("Old file deleted: {}", oldFilename);
                  } catch (Exception e){
                     logger.warn("Could not delete old file {}: {}", oldFilename, e.getMessage());
                  }
              }
          }

    @Override
    @Transactional
    public ProductResponse createProductBySeller(ProductRequest productRequest) {
        User currentSeller = getCurrentSeller();
        logger.info("Seller {} (ID: {}) creating product {}", currentSeller.getUsername(), currentSeller.getId(), productRequest.getName());

        Category category = categoryRepository.findById(productRequest.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + productRequest.getCategoryId()));

        Product product = productMapper.toProduct(productRequest);
        String slug = SlugUtils.toSlug(product.getName());
        validateUniqueSlug(slug, null);
        product.setSlug(slug);
        product.setSeller(currentSeller);
        product.setCategory(category);
        product.setApproved(false);
        product.setActive(true);

        Product savedProduct = productRepository.save(product);
        logger.info("Product {} (ID: {}) created by seller ID: {}", savedProduct.getName(), savedProduct.getId(), currentSeller.getId());
        return productMapper.toProductResponse(savedProduct);
    }

    @Override
    @Transactional
    public ProductResponse updateProductBySeller(Long productId, ProductRequest productRequest) {
         Product existingProduct = findProductByIdAndCheckOwnership(productId);
         logger.info("Seller ID: {} updating product ID: {}", existingProduct.getSeller().getId(), productId);

         if (!Objects.equals(existingProduct.getCategory().getId(), productRequest.getCategoryId())) {
            Category newCategory = categoryRepository.findById(productRequest.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("New category not found with id: " + productRequest.getCategoryId()));
            existingProduct.setCategory(newCategory);
         }

         if (!existingProduct.getName().equalsIgnoreCase(productRequest.getName())) {
            String newSlug = SlugUtils.toSlug(productRequest.getName());
            validateUniqueSlug(newSlug, productId);
            existingProduct.setSlug(newSlug);
         }

         productMapper.updateProductFromRequest(productRequest, existingProduct);
          existingProduct.setApproved(false);

        Product updatedProduct = productRepository.save(existingProduct);
        logger.info("Product ID {} updated by seller ID: {}", updatedProduct.getId(), updatedProduct.getSeller().getId());
        return productMapper.toProductResponse(updatedProduct);
    }

    @Override
    @Transactional
    public void deleteProductBySeller(Long productId) {
        Product productToDelete = findProductByIdAndCheckOwnership(productId);
        logger.warn("Seller ID: {} DELETING product ID: {}", productToDelete.getSeller().getId(), productId);

        deleteOldFile(extractFilenameFromUrl(productToDelete.getMainImageUrl()));
        deleteOldFile(extractFilenameFromUrl(productToDelete.getAiGeneratedImageUrl()));

        productRepository.delete(productToDelete);
        logger.info("Product ID {} deleted by seller ID: {}", productId, productToDelete.getSeller().getId());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> findProductsByCurrentSeller(Pageable pageable) {
        User currentSeller = getCurrentSeller();
        logger.debug("Fetching products for seller ID: {}", currentSeller.getId());
        Specification<Product> spec = Specification.where(ProductSpecifications.hasSeller(currentSeller.getId()));
        Page<Product> productPage = productRepository.findAll(spec, pageable);
        return productPage.map(productMapper::toProductResponse);
    }

     @Override
     @Transactional(readOnly = true)
     public ProductResponse findSellerProductById(Long productId) {
         Product product = findProductByIdAndCheckOwnership(productId);
          logger.debug("Fetching owned product ID: {} for seller ID: {}", productId, product.getSeller().getId());
         return productMapper.toProductResponse(product);
     }

    @Override
    @Transactional
    public ProductResponse setSellerProductActiveStatus(Long productId, boolean isActive) {
         Product product = findProductByIdAndCheckOwnership(productId);
         logger.info("Seller ID: {} setting active status to {} for product ID: {}", product.getSeller().getId(), isActive, productId);

         if (product.isActive() == isActive) {
             logger.debug("Product ID: {} already has active status {}.", productId, isActive);
             return productMapper.toProductResponse(product);
         }
         product.setActive(isActive);
        Product updatedProduct = productRepository.save(product);
        logger.info("Product ID {} active status set to {} by seller ID: {}", updatedProduct.getId(), isActive, updatedProduct.getSeller().getId());
        return productMapper.toProductResponse(updatedProduct);
    }

    @Override
    @Transactional
    public ProductResponse updateProductImageBySeller(Long productId, MultipartFile file) {
        Product product = findProductByIdAndCheckOwnership(productId);
        logger.info("Seller ID: {} uploading/updating MAIN image for product ID: {}", product.getSeller().getId(), productId);

        String newRelativeImagePath = storageService.storeImage(file, "products/images", 1200, 1200, 0.80f);
        String newImageUrl = storageService.generateUrl(newRelativeImagePath);

        product.getProductImages().stream()
                .filter(ProductImage::isPrimary)
                .findFirst()
                .ifPresent(oldPrimary -> oldPrimary.setPrimary(false));

        ProductImage newProductImage = ProductImage.builder()
                .product(product)
                .imageUrl(newImageUrl)
                .isPrimary(true)
                .displayOrder(0)
                .altText(product.getName() + " image")
                .build();
        product.getProductImages().add(newProductImage);
        product.setMainImageUrl(newImageUrl);

        Product updatedProduct = productRepository.save(product);
        logger.info("Main image updated for product ID: {}. New image URL: {}", productId, newImageUrl);
        return productMapper.toProductResponse(updatedProduct);
    }

    @Transactional
    public ProductImageDto addProductImage(Long productId, MultipartFile imageFile, String altText, boolean isPrimarySet) {
        Product product = findProductByIdAndCheckOwnership(productId);
        logger.info("Seller ID: {} adding image to product ID: {}", product.getSeller().getId(), productId);

        String relativeImagePath = storageService.storeImage(imageFile, "products/images", 1200, 1200, 0.80f);
        String imageUrl = storageService.generateUrl(relativeImagePath);

        if (isPrimarySet) {
            product.getProductImages().forEach(img -> img.setPrimary(false));
            product.setMainImageUrl(imageUrl);
        }

        ProductImage newProductImage = ProductImage.builder()
                .product(product)
                .imageUrl(imageUrl)
                .altText(StringUtils.hasText(altText) ? altText : product.getName() + " image")
                .isPrimary(isPrimarySet)
                .displayOrder(product.getProductImages().size())
                .build();

        product.getProductImages().add(newProductImage);
        productRepository.save(product);

        return ProductImageDto.builder()
                .id(newProductImage.getId())
                .imageUrl(newProductImage.getImageUrl())
                .altText(newProductImage.getAltText())
                .isPrimary(newProductImage.isPrimary())
                .displayOrder(newProductImage.getDisplayOrder())
                .uploadedAt(newProductImage.getUploadedAt())
                .build();
    }

    @Override
    @Transactional
    public AiImageGenerationResponse generateAiImageForSeller(AiImageGenerationRequest request) {
        User currentSeller = getCurrentSeller();
        int currentQuota = currentSeller.getImageGenQuota();
        SubscriptionType currentSubscription = currentSeller.getSubscriptionType();
        logger.info("Seller ID: {} requesting AI image generation (Quota: {}, Plan: {})", currentSeller.getId(), currentQuota, currentSubscription);

        if (currentQuota <= 0 && currentSubscription != SubscriptionType.SELLER_PLUS) {
            logger.warn("Seller ID: {} has insufficient image generation quota.", currentSeller.getId());
            return new AiImageGenerationResponse(false, "Yeterli görsel oluşturma hakkınız bulunmuyor. Daha fazlası için Plus üyeliğe geçin.", null, 0);
        }

        AiImageGenerationResponse aiResponse; // Değişken tipi güncellendi
        try {
            aiResponse = aiService.generateProductImage(request, currentSeller); // aiService çağrısı doğru
            if (aiResponse.isSuccess() && currentSubscription != SubscriptionType.SELLER_PLUS) {
                int newQuota = Math.max(0, currentQuota - 1); // Kota düşürme
                currentSeller.setImageGenQuota(newQuota);
                userRepository.save(currentSeller);
                logger.info("Decremented image quota for Seller ID: {}. Remaining: {}", currentSeller.getId(), newQuota);
                // Yanıttaki remainingQuota'yı güncelle
                return new AiImageGenerationResponse(aiResponse.isSuccess(), aiResponse.getMessage(), aiResponse.getImageUrls(), newQuota);
            } else if (aiResponse.isSuccess() && currentSubscription == SubscriptionType.SELLER_PLUS){
                logger.info("Seller ID: {} is on PLUS plan, quota not decremented.", currentSeller.getId());
                 return new AiImageGenerationResponse(aiResponse.isSuccess(), aiResponse.getMessage(), aiResponse.getImageUrls(), null); // Plus için kota null
            }
            // Eğer aiResponse.isSuccess() false ise, doğrudan onu döndür
            return aiResponse;
        } catch (BadRequestException e) {
            logger.warn("AI image generation controlled error for Seller ID: {}: {}", currentSeller.getId(), e.getMessage());
            return new AiImageGenerationResponse(false, "Görsel üretilemedi: " + e.getMessage(), null, currentQuota);
        } catch (Exception e) {
            logger.error("AI image generation failed unexpectedly for Seller ID: {}", currentSeller.getId(), e);
            return new AiImageGenerationResponse(false, "Görsel üretilirken beklenmedik bir hata oluştu.", null, currentQuota);
        }
    }

    @Override
    @Transactional
    public ProductResponse setAiImageAsProductImage(SetAiImageAsProductRequest request) {
        Product product = findProductByIdAndCheckOwnership(request.getProductId());
        User currentSeller = product.getSeller();
        String aiImageUrl = request.getAiImageUrl();
        logger.info("Seller ID: {} setting AI image {} as MAIN image for Product ID: {}",
               currentSeller.getId(), aiImageUrl, request.getProductId());
        String oldMainImageFilename = extractFilenameFromUrl(product.getMainImageUrl());
        product.setMainImageUrl(aiImageUrl);
        product.setAiGeneratedImageUrl(aiImageUrl);
        Product savedProduct = productRepository.save(product);
        if(oldMainImageFilename != null && !oldMainImageFilename.equals(extractFilenameFromUrl(aiImageUrl))) {
            deleteOldFile(oldMainImageFilename);
        }
        logger.info("Product ID: {} main image URL updated to AI generated URL: {}", product.getId(), aiImageUrl);
        return productMapper.toProductResponse(savedProduct);
    }
}
