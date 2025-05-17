package com.fibiyo.ecommerce.application.mapper;

import com.fibiyo.ecommerce.application.dto.ProductImageDto;
import com.fibiyo.ecommerce.application.dto.ProductResponse;
import com.fibiyo.ecommerce.domain.entity.Product;
import com.fibiyo.ecommerce.domain.entity.ProductImage;
import com.fibiyo.ecommerce.domain.entity.WishlistItem;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-05-17T12:02:50+0300",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.42.0.v20250508-0718, environment: Java 21.0.7 (Eclipse Adoptium)"
)
@Component
public class WishlistMapperImpl implements WishlistMapper {

    @Override
    public ProductResponse wishlistItemToProductResponse(WishlistItem wishlistItem) {
        if ( wishlistItem == null ) {
            return null;
        }

        ProductResponse productResponse = new ProductResponse();

        productResponse.setAiGeneratedImageUrl( wishlistItemProductAiGeneratedImageUrl( wishlistItem ) );
        productResponse.setAverageRating( wishlistItemProductAverageRating( wishlistItem ) );
        productResponse.setCreatedAt( wishlistItemProductCreatedAt( wishlistItem ) );
        productResponse.setDescription( wishlistItemProductDescription( wishlistItem ) );
        productResponse.setId( wishlistItemProductId( wishlistItem ) );
        productResponse.setMainImageUrl( wishlistItemProductMainImageUrl( wishlistItem ) );
        productResponse.setName( wishlistItemProductName( wishlistItem ) );
        productResponse.setPrice( wishlistItemProductPrice( wishlistItem ) );
        List<ProductImage> productImages = wishlistItemProductProductImages( wishlistItem );
        productResponse.setProductImages( productImageListToProductImageDtoList( productImages ) );
        productResponse.setReviewCount( wishlistItemProductReviewCount( wishlistItem ) );
        productResponse.setReviewSummaryAi( wishlistItemProductReviewSummaryAi( wishlistItem ) );
        productResponse.setSku( wishlistItemProductSku( wishlistItem ) );
        productResponse.setSlug( wishlistItemProductSlug( wishlistItem ) );
        productResponse.setStock( wishlistItemProductStock( wishlistItem ) );
        productResponse.setUpdatedAt( wishlistItemProductUpdatedAt( wishlistItem ) );
        productResponse.setActive( wishlistItemProductActive( wishlistItem ) );
        productResponse.setApproved( wishlistItemProductApproved( wishlistItem ) );

        return productResponse;
    }

    @Override
    public List<ProductResponse> wishlistItemsToProductResponseList(List<WishlistItem> wishlistItems) {
        if ( wishlistItems == null ) {
            return null;
        }

        List<ProductResponse> list = new ArrayList<ProductResponse>( wishlistItems.size() );
        for ( WishlistItem wishlistItem : wishlistItems ) {
            list.add( wishlistItemToProductResponse( wishlistItem ) );
        }

        return list;
    }

    private String wishlistItemProductAiGeneratedImageUrl(WishlistItem wishlistItem) {
        if ( wishlistItem == null ) {
            return null;
        }
        Product product = wishlistItem.getProduct();
        if ( product == null ) {
            return null;
        }
        String aiGeneratedImageUrl = product.getAiGeneratedImageUrl();
        if ( aiGeneratedImageUrl == null ) {
            return null;
        }
        return aiGeneratedImageUrl;
    }

    private BigDecimal wishlistItemProductAverageRating(WishlistItem wishlistItem) {
        if ( wishlistItem == null ) {
            return null;
        }
        Product product = wishlistItem.getProduct();
        if ( product == null ) {
            return null;
        }
        BigDecimal averageRating = product.getAverageRating();
        if ( averageRating == null ) {
            return null;
        }
        return averageRating;
    }

    private LocalDateTime wishlistItemProductCreatedAt(WishlistItem wishlistItem) {
        if ( wishlistItem == null ) {
            return null;
        }
        Product product = wishlistItem.getProduct();
        if ( product == null ) {
            return null;
        }
        LocalDateTime createdAt = product.getCreatedAt();
        if ( createdAt == null ) {
            return null;
        }
        return createdAt;
    }

    private String wishlistItemProductDescription(WishlistItem wishlistItem) {
        if ( wishlistItem == null ) {
            return null;
        }
        Product product = wishlistItem.getProduct();
        if ( product == null ) {
            return null;
        }
        String description = product.getDescription();
        if ( description == null ) {
            return null;
        }
        return description;
    }

    private Long wishlistItemProductId(WishlistItem wishlistItem) {
        if ( wishlistItem == null ) {
            return null;
        }
        Product product = wishlistItem.getProduct();
        if ( product == null ) {
            return null;
        }
        Long id = product.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    private String wishlistItemProductMainImageUrl(WishlistItem wishlistItem) {
        if ( wishlistItem == null ) {
            return null;
        }
        Product product = wishlistItem.getProduct();
        if ( product == null ) {
            return null;
        }
        String mainImageUrl = product.getMainImageUrl();
        if ( mainImageUrl == null ) {
            return null;
        }
        return mainImageUrl;
    }

    private String wishlistItemProductName(WishlistItem wishlistItem) {
        if ( wishlistItem == null ) {
            return null;
        }
        Product product = wishlistItem.getProduct();
        if ( product == null ) {
            return null;
        }
        String name = product.getName();
        if ( name == null ) {
            return null;
        }
        return name;
    }

    private BigDecimal wishlistItemProductPrice(WishlistItem wishlistItem) {
        if ( wishlistItem == null ) {
            return null;
        }
        Product product = wishlistItem.getProduct();
        if ( product == null ) {
            return null;
        }
        BigDecimal price = product.getPrice();
        if ( price == null ) {
            return null;
        }
        return price;
    }

    private List<ProductImage> wishlistItemProductProductImages(WishlistItem wishlistItem) {
        if ( wishlistItem == null ) {
            return null;
        }
        Product product = wishlistItem.getProduct();
        if ( product == null ) {
            return null;
        }
        List<ProductImage> productImages = product.getProductImages();
        if ( productImages == null ) {
            return null;
        }
        return productImages;
    }

    protected ProductImageDto productImageToProductImageDto(ProductImage productImage) {
        if ( productImage == null ) {
            return null;
        }

        ProductImageDto.ProductImageDtoBuilder productImageDto = ProductImageDto.builder();

        productImageDto.altText( productImage.getAltText() );
        productImageDto.displayOrder( productImage.getDisplayOrder() );
        productImageDto.id( productImage.getId() );
        productImageDto.imageUrl( productImage.getImageUrl() );
        productImageDto.uploadedAt( productImage.getUploadedAt() );

        return productImageDto.build();
    }

    protected List<ProductImageDto> productImageListToProductImageDtoList(List<ProductImage> list) {
        if ( list == null ) {
            return null;
        }

        List<ProductImageDto> list1 = new ArrayList<ProductImageDto>( list.size() );
        for ( ProductImage productImage : list ) {
            list1.add( productImageToProductImageDto( productImage ) );
        }

        return list1;
    }

    private int wishlistItemProductReviewCount(WishlistItem wishlistItem) {
        if ( wishlistItem == null ) {
            return 0;
        }
        Product product = wishlistItem.getProduct();
        if ( product == null ) {
            return 0;
        }
        int reviewCount = product.getReviewCount();
        return reviewCount;
    }

    private String wishlistItemProductReviewSummaryAi(WishlistItem wishlistItem) {
        if ( wishlistItem == null ) {
            return null;
        }
        Product product = wishlistItem.getProduct();
        if ( product == null ) {
            return null;
        }
        String reviewSummaryAi = product.getReviewSummaryAi();
        if ( reviewSummaryAi == null ) {
            return null;
        }
        return reviewSummaryAi;
    }

    private String wishlistItemProductSku(WishlistItem wishlistItem) {
        if ( wishlistItem == null ) {
            return null;
        }
        Product product = wishlistItem.getProduct();
        if ( product == null ) {
            return null;
        }
        String sku = product.getSku();
        if ( sku == null ) {
            return null;
        }
        return sku;
    }

    private String wishlistItemProductSlug(WishlistItem wishlistItem) {
        if ( wishlistItem == null ) {
            return null;
        }
        Product product = wishlistItem.getProduct();
        if ( product == null ) {
            return null;
        }
        String slug = product.getSlug();
        if ( slug == null ) {
            return null;
        }
        return slug;
    }

    private Integer wishlistItemProductStock(WishlistItem wishlistItem) {
        if ( wishlistItem == null ) {
            return null;
        }
        Product product = wishlistItem.getProduct();
        if ( product == null ) {
            return null;
        }
        Integer stock = product.getStock();
        if ( stock == null ) {
            return null;
        }
        return stock;
    }

    private LocalDateTime wishlistItemProductUpdatedAt(WishlistItem wishlistItem) {
        if ( wishlistItem == null ) {
            return null;
        }
        Product product = wishlistItem.getProduct();
        if ( product == null ) {
            return null;
        }
        LocalDateTime updatedAt = product.getUpdatedAt();
        if ( updatedAt == null ) {
            return null;
        }
        return updatedAt;
    }

    private boolean wishlistItemProductActive(WishlistItem wishlistItem) {
        if ( wishlistItem == null ) {
            return false;
        }
        Product product = wishlistItem.getProduct();
        if ( product == null ) {
            return false;
        }
        boolean active = product.isActive();
        return active;
    }

    private boolean wishlistItemProductApproved(WishlistItem wishlistItem) {
        if ( wishlistItem == null ) {
            return false;
        }
        Product product = wishlistItem.getProduct();
        if ( product == null ) {
            return false;
        }
        boolean approved = product.isApproved();
        return approved;
    }
}
