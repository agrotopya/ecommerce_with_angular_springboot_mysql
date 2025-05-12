package com.fibiyo.ecommerce.application.service;

import com.fibiyo.ecommerce.application.dto.ProductRequest;
import com.fibiyo.ecommerce.application.dto.ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface ProductService {

    /* ----- Public ----- */
    Page<ProductResponse> findActiveAndApprovedProducts(Pageable pageable, Long categoryId, String searchTerm);
    ProductResponse findActiveAndApprovedProductById(Long id);
    ProductResponse findActiveAndApprovedProductBySlug(String slug);

    ProductResponse updateProductImage(Long productId, MultipartFile file);


    /* ----- Seller ----- */
    ProductResponse createProductBySeller(ProductRequest productRequest);
    ProductResponse updateProductBySeller(Long productId, ProductRequest productRequest);
    void            deleteProductBySeller(Long productId);
    Page<ProductResponse> findProductsByCurrentSeller(Pageable pageable);
    ProductResponse setSellerProductActiveStatus(Long productId, boolean isActive);

    /* ----- Admin ----- */
    Page<ProductResponse> findAllProductsAdmin(Pageable pageable, Long sellerId, Long categoryId,
                                               Boolean isApproved, Boolean isActive, String searchTerm);
    ProductResponse findProductByIdAdmin(Long id);
    ProductResponse approveProduct(Long productId);
    ProductResponse rejectProduct(Long productId);
    void            deleteProductByAdmin(Long productId);
    ProductResponse setAdminProductActiveStatus(Long productId, boolean isActive);



}
