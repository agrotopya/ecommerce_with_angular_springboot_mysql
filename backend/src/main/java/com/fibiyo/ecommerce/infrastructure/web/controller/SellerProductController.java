package com.fibiyo.ecommerce.infrastructure.web.controller;

import com.fibiyo.ecommerce.application.dto.*;
import com.fibiyo.ecommerce.application.exception.BadRequestException;
import com.fibiyo.ecommerce.application.service.SellerProductService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/seller/products") // Seller'a özel endpoint base path
@PreAuthorize("hasRole('SELLER')")    // Tüm metodlar Seller yetkisi gerektirir
public class SellerProductController {

    private static final Logger logger = LoggerFactory.getLogger(SellerProductController.class);

    private final SellerProductService sellerProductService;

    @Autowired
    public SellerProductController(SellerProductService sellerProductService) {
        this.sellerProductService = sellerProductService;
    }

    // Yeni ürün oluşturma
    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductRequest productRequest) {
        logger.info("POST /api/seller/products requested");
        ProductResponse createdProduct = sellerProductService.createProductBySeller(productRequest);
        return new ResponseEntity<>(createdProduct, HttpStatus.CREATED);
    }

    // Satıcının kendi ürünlerini listeleme
    @GetMapping("/my")
    public ResponseEntity<Page<ProductResponse>> getMyProducts(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        logger.info("GET /api/seller/products/my requested");
        Page<ProductResponse> products = sellerProductService.findProductsByCurrentSeller(pageable);
        return ResponseEntity.ok(products);
    }

    // Satıcının kendi ürününün detayını getirme
    @GetMapping("/{productId}")
    public ResponseEntity<ProductResponse> getMyProductDetails(@PathVariable Long productId) {
        logger.info("GET /api/seller/products/{} requested", productId);
        ProductResponse product = sellerProductService.findSellerProductById(productId);
        return ResponseEntity.ok(product);
    }

    // Satıcının kendi ürününü güncelleme
    @PutMapping("/{productId}")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Long productId,
            @Valid @RequestBody ProductRequest productRequest) {
        logger.info("PUT /api/seller/products/{} requested", productId);
        ProductResponse updatedProduct = sellerProductService.updateProductBySeller(productId, productRequest);
        return ResponseEntity.ok(updatedProduct);
    }

    // Satıcının kendi ürününü silme
    @DeleteMapping("/{productId}")
    public ResponseEntity<ApiResponse> deleteProduct(@PathVariable Long productId) {
        logger.warn("DELETE /api/seller/products/{} requested", productId);
        sellerProductService.deleteProductBySeller(productId);
        return ResponseEntity.ok(new ApiResponse(true, "Ürün başarıyla silindi."));
    }

    // Satıcının kendi ürününün aktiflik durumunu ayarlama
    @PatchMapping("/{productId}/activate")
    public ResponseEntity<ProductResponse> setProductActiveStatus(
            @PathVariable Long productId,
            @RequestParam boolean isActive) {
        logger.info("PATCH /api/seller/products/{}/activate requested with status: {}", productId, isActive);
        ProductResponse updatedProduct = sellerProductService.setSellerProductActiveStatus(productId, isActive);
        return ResponseEntity.ok(updatedProduct);
    }

    // Satıcının ürününe ana görsel yükleme/güncelleme
    @PostMapping("/{productId}/image") // Bu endpoint ana görseli yükler/günceller
    public ResponseEntity<ProductResponse> uploadOrUpdateMainProductImage(
            @PathVariable Long productId,
            @RequestParam("file") MultipartFile file) {
        logger.info("POST /api/seller/products/{}/image (main image) requested", productId);
        if (file == null || file.isEmpty()) {
           throw new BadRequestException("Yüklenecek dosya seçilmedi.");
       }
        // Servis metodu, bu resmi ana resim olarak işleyecek şekilde güncellenmeli
        ProductResponse updatedProduct = sellerProductService.updateProductImageBySeller(productId, file); // Bu metodun içi ProductImage ve mainImageUrl'i yönetecek
        return ResponseEntity.ok(updatedProduct);
    }

    // Ürüne yeni bir görsel ekleme
    @PostMapping("/{productId}/images")
    public ResponseEntity<ProductImageDto> addProductImage(
            @PathVariable Long productId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String altText,
            @RequestParam(defaultValue = "false") boolean isPrimary) {
        logger.info("POST /api/seller/products/{}/images requested. isPrimary: {}", productId, isPrimary);
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Yüklenecek dosya seçilmedi.");
        }
        ProductImageDto newImageDto = sellerProductService.addProductImage(productId, file, altText, isPrimary);
        return new ResponseEntity<>(newImageDto, HttpStatus.CREATED);
    }

    // TODO: Üründen belirli bir resmi silmek için yeni endpoint (örn: DELETE /{productId}/images/{imageId})
    // TODO: Ürünün resimleri arasından ana resmi belirlemek için yeni endpoint (örn: PATCH /{productId}/images/{imageId}/set-primary)


    // Satıcı için AI görsel üretme/düzenleme
    @PostMapping("/generate-image")
    public ResponseEntity<AiImageGenerationResponse> generateAiImage(@Valid @RequestBody AiImageGenerationRequest request) {
        logger.info("POST /api/seller/products/generate-image requested for ref: {}, prompt: '{}...'",
                  request.getReferenceImageIdentifier(), request.getPrompt().substring(0, Math.min(50, request.getPrompt().length())));
        AiImageGenerationResponse response = sellerProductService.generateAiImageForSeller(request);
        HttpStatus status = response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
        if (!response.isSuccess() && response.getMessage().toLowerCase().contains("kota") || response.getMessage().toLowerCase().contains("hakkınız bitti")) {
            status = HttpStatus.FORBIDDEN; // 403 Kota bitti
        } else if (!response.isSuccess() && (response.getMessage().contains("API Hatası") || response.getMessage().contains("servisinde") || response.getMessage().contains("beklenmedik") )) {
            status = HttpStatus.INTERNAL_SERVER_ERROR; // 500 İç sunucu/API hatası
        }
         // Diğer durumlar (örn: içerik politikası) BadRequest kalabilir (400).
        return new ResponseEntity<>(response, status);
    }

    // Üretilen AI görselini ürünün ana görseli yapma
    @PostMapping("/set-ai-image")
    public ResponseEntity<ProductResponse> setAiImageAsProductImage(@Valid @RequestBody SetAiImageAsProductRequest request) {
        logger.info("POST /api/seller/products/set-ai-image requested for Product ID: {}", request.getProductId());
        ProductResponse updatedProduct = sellerProductService.setAiImageAsProductImage(request);
        return ResponseEntity.ok(updatedProduct);
    }

}
