package com.fibiyo.ecommerce.application.mapper;

import com.fibiyo.ecommerce.application.dto.ProductImageDto;
import com.fibiyo.ecommerce.application.dto.ProductRequest;
import com.fibiyo.ecommerce.application.dto.ProductResponse;
import com.fibiyo.ecommerce.domain.entity.Category;
import com.fibiyo.ecommerce.domain.entity.Product;
import com.fibiyo.ecommerce.domain.entity.ProductImage;
import com.fibiyo.ecommerce.domain.entity.User;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-05-17T11:59:19+0300",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.42.0.v20250508-0718, environment: Java 21.0.7 (Eclipse Adoptium)"
)
@Component
public class ProductMapperImpl implements ProductMapper {

    @Override
    public ProductResponse toProductResponse(Product product) {
        if ( product == null ) {
            return null;
        }

        ProductResponse productResponse = new ProductResponse();

        productResponse.setCategoryId( productCategoryId( product ) );
        productResponse.setCategoryName( productCategoryName( product ) );
        productResponse.setCategorySlug( productCategorySlug( product ) );
        productResponse.setSellerId( productSellerId( product ) );
        productResponse.setSellerUsername( productSellerUsername( product ) );
        productResponse.setMainImageUrl( product.getMainImageUrl() );
        productResponse.setProductImages( productImageListToProductImageDtoList( product.getProductImages() ) );
        productResponse.setReviewSummaryAi( product.getReviewSummaryAi() );
        productResponse.setActive( product.isActive() );
        productResponse.setAiGeneratedImageUrl( product.getAiGeneratedImageUrl() );
        productResponse.setApproved( product.isApproved() );
        productResponse.setAverageRating( product.getAverageRating() );
        productResponse.setCreatedAt( product.getCreatedAt() );
        productResponse.setDescription( product.getDescription() );
        productResponse.setId( product.getId() );
        productResponse.setName( product.getName() );
        productResponse.setPrice( product.getPrice() );
        productResponse.setReviewCount( product.getReviewCount() );
        productResponse.setSku( product.getSku() );
        productResponse.setSlug( product.getSlug() );
        productResponse.setStock( product.getStock() );
        productResponse.setUpdatedAt( product.getUpdatedAt() );

        return productResponse;
    }

    @Override
    public List<ProductResponse> toProductResponseList(List<Product> products) {
        if ( products == null ) {
            return null;
        }

        List<ProductResponse> list = new ArrayList<ProductResponse>( products.size() );
        for ( Product product : products ) {
            list.add( toProductResponse( product ) );
        }

        return list;
    }

    @Override
    public Product toProduct(ProductRequest productRequest) {
        if ( productRequest == null ) {
            return null;
        }

        Product product = new Product();

        product.setDescription( productRequest.getDescription() );
        product.setName( productRequest.getName() );
        product.setPrice( productRequest.getPrice() );
        product.setSku( productRequest.getSku() );
        product.setStock( productRequest.getStock() );

        return product;
    }

    @Override
    public void updateProductFromRequest(ProductRequest request, Product product) {
        if ( request == null ) {
            return;
        }

        if ( request.getDescription() != null ) {
            product.setDescription( request.getDescription() );
        }
        if ( request.getName() != null ) {
            product.setName( request.getName() );
        }
        if ( request.getPrice() != null ) {
            product.setPrice( request.getPrice() );
        }
        if ( request.getSku() != null ) {
            product.setSku( request.getSku() );
        }
        if ( request.getStock() != null ) {
            product.setStock( request.getStock() );
        }
    }

    private Long productCategoryId(Product product) {
        if ( product == null ) {
            return null;
        }
        Category category = product.getCategory();
        if ( category == null ) {
            return null;
        }
        Long id = category.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    private String productCategoryName(Product product) {
        if ( product == null ) {
            return null;
        }
        Category category = product.getCategory();
        if ( category == null ) {
            return null;
        }
        String name = category.getName();
        if ( name == null ) {
            return null;
        }
        return name;
    }

    private String productCategorySlug(Product product) {
        if ( product == null ) {
            return null;
        }
        Category category = product.getCategory();
        if ( category == null ) {
            return null;
        }
        String slug = category.getSlug();
        if ( slug == null ) {
            return null;
        }
        return slug;
    }

    private Long productSellerId(Product product) {
        if ( product == null ) {
            return null;
        }
        User seller = product.getSeller();
        if ( seller == null ) {
            return null;
        }
        Long id = seller.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    private String productSellerUsername(Product product) {
        if ( product == null ) {
            return null;
        }
        User seller = product.getSeller();
        if ( seller == null ) {
            return null;
        }
        String username = seller.getUsername();
        if ( username == null ) {
            return null;
        }
        return username;
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
}
