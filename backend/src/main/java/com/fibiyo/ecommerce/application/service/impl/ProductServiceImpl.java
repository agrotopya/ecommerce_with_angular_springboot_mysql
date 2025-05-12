package com.fibiyo.ecommerce.application.service.impl;
import com.fibiyo.ecommerce.application.service.NotificationService; // Eğer zaten inject edildiyse tekrar eklemeye gerek yok
import com.fibiyo.ecommerce.domain.enums.NotificationType; // bu eklenecek

import com.fibiyo.ecommerce.application.dto.ProductRequest;
import com.fibiyo.ecommerce.application.dto.ProductResponse;
import com.fibiyo.ecommerce.application.exception.BadRequestException;
import com.fibiyo.ecommerce.application.exception.ForbiddenException;
import com.fibiyo.ecommerce.application.exception.ResourceNotFoundException;
import com.fibiyo.ecommerce.application.mapper.ProductMapper;
import com.fibiyo.ecommerce.application.service.ProductService;
import com.fibiyo.ecommerce.application.service.StorageService;
import com.fibiyo.ecommerce.application.util.SlugUtils;
import com.fibiyo.ecommerce.domain.entity.Category;
import com.fibiyo.ecommerce.domain.entity.Product;
import com.fibiyo.ecommerce.domain.entity.User;
import com.fibiyo.ecommerce.domain.enums.Role;
import com.fibiyo.ecommerce.infrastructure.persistence.repository.CategoryRepository;
import com.fibiyo.ecommerce.infrastructure.persistence.repository.ProductRepository;
import com.fibiyo.ecommerce.infrastructure.persistence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Paths; // Paths importu eklendi
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static com.fibiyo.ecommerce.infrastructure.persistence.specification.ProductSpecifications.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {
    private final Logger logger = LoggerFactory.getLogger(ProductServiceImpl.class);
    private final ProductRepository  productRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository     userRepository;
    private final ProductMapper      productMapper;
    private final StorageService storageService; // StorageService'i inject et
private final NotificationService notificationService; // bu eklenecek

    /* ---------- Helper ---------- */

         // URL'den dosya adını çıkaran basit bir helper
     private String extractFilenameFromUrl(String url){
            if (url == null || !url.contains("/")) {
                return url;
            }
            return url.substring(url.lastIndexOf('/') + 1);
        }
  

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal()))
            throw new ForbiddenException("Erişim için kimlik doğrulaması gerekli.");
        String username = auth.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Kullanıcı veritabanında bulunamadı: " + username));
    }

    private void assertAdmin() {
        if (getCurrentUser().getRole() != Role.ADMIN)
            throw new ForbiddenException("Bu işlem için Admin yetkisi gerekli.");
    }

    private void assertOwnerOrAdmin(Product product) {
        User current = getCurrentUser();
        if (current.getRole() != Role.ADMIN && !product.getSeller().getId().equals(current.getId()))
            throw new ForbiddenException("Bu işlem için yetkiniz yok.");
    }



    @Override
    @Transactional
    public ProductResponse updateProductImage(Long productId, MultipartFile file) {
        User currentUser = getCurrentUser(); // Yetki kontrolü için veya loglama için
        logger.info("User '{}' attempting to upload image for product ID: {}", currentUser.getUsername(), productId);

        Product product = productRepository.findById(productId)
               .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        // Spring Security @PreAuthorize veya metod içi kontrol ile sahiplik/admin yetkisi zaten kontrol ediliyor olmalı.
        // Örneğin: @PreAuthorize("@productSecurity.hasPermission(#productId, authentication)")

        String oldImageFileName = null;
        if (product.getMainImageUrl() != null && !product.getMainImageUrl().isBlank()) { // getImageUrl -> getMainImageUrl
            try {
                // Sadece dosya adını alıyoruz, alt klasör bilgisiyle birleştirilecek.
                oldImageFileName = extractFilenameFromUrl(product.getMainImageUrl()); // getImageUrl -> getMainImageUrl
            } catch (Exception e) {
                logger.warn("Could not determine old image filename from URL: {}", product.getMainImageUrl()); // getImageUrl -> getMainImageUrl
            }
        }

        // Resmi sıkıştırarak kaydet
        // Parametreler: file, subfolder, targetWidth, targetHeight, quality
        // storeImage metodu "subfolder/uniqueFilename.ext" formatında göreli yolu döner.
        String newRelativeImagePath = storageService.storeImage(file, "products/images", 1200, 1200, 0.80f);
        String newImageUrl = storageService.generateUrl(newRelativeImagePath); // Bu tam URL'yi oluşturur.

        product.setMainImageUrl(newImageUrl); // setImageUrl -> setMainImageUrl
        Product updatedProduct = productRepository.save(product);
        logger.info("Image updated for product ID: {}. New image URL: {}", productId, newImageUrl);

        // Eski resim varsa ve yeni resimden farklıysa sil
        if (oldImageFileName != null) {
            String newImageFileName = extractFilenameFromUrl(newImageUrl); // Yeni URL'den de dosya adını al
            if (!oldImageFileName.equals(newImageFileName)) {
                try {
                    // Eski resmi silerken alt klasör bilgisini ekliyoruz.
                    String oldRelativePathToDelete = Paths.get("products/images", oldImageFileName).toString().replace("\\", "/");
                    storageService.deleteFile(oldRelativePathToDelete);
                    logger.info("Attempted to delete old image file: {}", oldRelativePathToDelete);
                } catch (Exception e) {
                    logger.warn("Could not delete old image file '{}': {}", oldImageFileName, e.getMessage());
                }
            }
        }
        return productMapper.toProductResponse(updatedProduct);
    }

 
    /* ---------- Public Ops ---------- */

    @Override @Transactional(readOnly = true)
    public Page<ProductResponse> findActiveAndApprovedProducts(Pageable pageable, Long categoryId, String searchTerm) {
        Specification<Product> spec = Specification.where(isActive(true)).and(isApproved(true));
        if (categoryId != null) spec = spec.and(hasCategory(categoryId));
        if (searchTerm != null && !searchTerm.isBlank()) spec = spec.and(nameOrDescriptionContains(searchTerm));
        return productRepository.findAll(spec, pageable).map(productMapper::toProductResponse);
    }

    @Override @Transactional(readOnly = true)
    public ProductResponse findActiveAndApprovedProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ürün bulunamadı: " + id));
        if (!product.isActive() || !product.isApproved())
            throw new ResourceNotFoundException("Ürün bulunamadı: " + id);
        return productMapper.toProductResponse(product);
    }

    @Override @Transactional(readOnly = true)
    public ProductResponse findActiveAndApprovedProductBySlug(String slug) {
        Product product = productRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Ürün bulunamadı: " + slug));
        if (!product.isActive() || !product.isApproved())
            throw new ResourceNotFoundException("Ürün bulunamadı: " + slug);
        return productMapper.toProductResponse(product);
    }

    /* ---------- Seller Ops ---------- */

    @Override @Transactional
    public ProductResponse createProductBySeller(ProductRequest req) {
        User seller = getCurrentUser();
        if (seller.getRole() != Role.SELLER)
            throw new ForbiddenException("Sadece satıcılar ürün ekleyebilir.");

        Category category = categoryRepository.findById(req.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Kategori bulunamadı: " + req.getCategoryId()));

        Product product = productMapper.toProduct(req);
        product.setSlug(SlugUtils.toSlug(req.getName()));
        product.setSeller(seller);
        product.setCategory(category);
        product.setApproved(false);
        product.setActive(true);

        return productMapper.toProductResponse(productRepository.save(product));
    }

    @Override @Transactional
    public ProductResponse updateProductBySeller(Long id, ProductRequest req) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ürün bulunamadı: " + id));
        assertOwnerOrAdmin(product);

        if (!product.getCategory().getId().equals(req.getCategoryId())) {
            Category cat = categoryRepository.findById(req.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Kategori bulunamadı: " + req.getCategoryId()));
            product.setCategory(cat);
        }

        productMapper.updateProductFromRequest(req, product);

        // bu eklenecek
if (!product.getName().equals(req.getName())) {
    String newSlug = SlugUtils.toSlug(req.getName());
    if (productRepository.existsBySlug(newSlug)) {
        throw new BadRequestException("Aynı isimle başka bir ürün zaten var. Lütfen farklı bir ad girin.");
    }
    product.setSlug(newSlug);
}


        return productMapper.toProductResponse(productRepository.save(product));
    }

    @Override @Transactional
    public void deleteProductBySeller(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ürün bulunamadı: " + id));
        assertOwnerOrAdmin(product);
        productRepository.delete(product);
    }

    @Override @Transactional(readOnly = true)
    public Page<ProductResponse> findProductsByCurrentSeller(Pageable pageable) {
        User seller = getCurrentUser();
        if (seller.getRole() != Role.SELLER)
            throw new ForbiddenException("Sadece satıcılar işlem yapabilir.");
        return productRepository.findAll(hasSeller(seller.getId()), pageable)
                .map(productMapper::toProductResponse);
    }

    @Override @Transactional
    public ProductResponse setSellerProductActiveStatus(Long id, boolean isActive) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ürün bulunamadı: " + id));
        assertOwnerOrAdmin(product);
        product.setActive(isActive);
        return productMapper.toProductResponse(productRepository.save(product));
    }

    /* ---------- Admin Ops ---------- */

    @Override @Transactional(readOnly = true)
    public Page<ProductResponse> findAllProductsAdmin(Pageable pageable, Long sellerId, Long categoryId,
                                                      Boolean approved, Boolean active, String searchTerm) {
        assertAdmin();
        Specification<Product> spec = Specification.where(null);
        if (sellerId  != null) spec = spec.and(hasSeller(sellerId));
        if (categoryId!= null) spec = spec.and(hasCategory(categoryId));
        if (approved  != null) spec = spec.and(isApproved(approved));
        if (active    != null) spec = spec.and(isActive(active));
        if (searchTerm!= null && !searchTerm.isBlank())
            spec = spec.and(nameOrDescriptionContains(searchTerm));

        return productRepository.findAll(spec, pageable).map(productMapper::toProductResponse);
    }

    @Override @Transactional(readOnly = true)
    public ProductResponse findProductByIdAdmin(Long id) {
        assertAdmin();
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ürün bulunamadı: " + id));
        return productMapper.toProductResponse(product);
    }

    @Override @Transactional
    public ProductResponse approveProduct(Long id) {
        assertAdmin();
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ürün bulunamadı: " + id));

                
        product.setApproved(true);

        Product saved = productRepository.save(product);

// bu eklenecek
notificationService.createNotification(
    saved.getSeller(),
    "Ürününüz onaylandı: " + saved.getName(),
    "/seller/products",
    NotificationType.PRODUCT_UPDATE
);
        return productMapper.toProductResponse(productRepository.save(product));



    }

    @Override @Transactional
    public ProductResponse rejectProduct(Long id) {
        assertAdmin();
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ürün bulunamadı: " + id));
        product.setApproved(false);
        return productMapper.toProductResponse(productRepository.save(product));
    }

    @Override @Transactional
    public void deleteProductByAdmin(Long id) {
        assertAdmin();
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ürün bulunamadı: " + id));
        productRepository.delete(product);
    }

    @Override @Transactional
    public ProductResponse setAdminProductActiveStatus(Long id, boolean isActive) {
        assertAdmin();
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ürün bulunamadı: " + id));
        product.setActive(isActive);
        return productMapper.toProductResponse(productRepository.save(product));
    }
}
