package com.fibiyo.ecommerce.application.mapper;

import com.fibiyo.ecommerce.application.dto.ProductRequest;
import com.fibiyo.ecommerce.application.dto.ProductResponse;
import com.fibiyo.ecommerce.domain.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;





// spring ve mapstruct-processor bağımlılıkları pom.xml'de olmalı
@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE, // Update için null geleni atla
        uses = {CategoryMapper.class}) // Category bilgilerini maplemek için CategoryMapper'ı kullanabilir (isteğe bağlı)
public interface ProductMapper {

    // Product -> ProductResponse
    // Kaynak (source) entity'deki alanları, hedef (target) DTO'daki alanlara map'ler.
    @Mapping(source = "category.id", target = "categoryId")
    @Mapping(source = "category.name", target = "categoryName")
    @Mapping(source = "category.slug", target = "categorySlug")
    @Mapping(source = "seller.id", target = "sellerId")
    @Mapping(source = "seller.username", target = "sellerUsername")
    @Mapping(source = "mainImageUrl", target = "mainImageUrl") // Eklendi
    @Mapping(source = "productImages", target = "productImages") // Eklendi
    ProductResponse toProductResponse(Product product);

    List<ProductResponse> toProductResponseList(List<Product> products);

    // ProductRequest -> Product (Create için)
    // ID, slug, createdAt, updatedAt, ilişkiler (category, seller), AI alanları, onay durumu vb. request'te yok, manuel set edilecek.
    // @Mapping(target = "...", ignore = true) şeklinde belirtilebilir.
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "slug", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "approved", ignore = true)
    @Mapping(target = "active", ignore = true) // Serviste default set edilecek
    @Mapping(target = "averageRating", ignore = true)
    @Mapping(target = "reviewCount", ignore = true)
    @Mapping(target = "reviewSummaryAi", ignore = true)
    @Mapping(target = "aiGeneratedImageUrl", ignore = true)
    @Mapping(target = "mainImageUrl", ignore = true) // Eklendi
    @Mapping(target = "productImages", ignore = true) // Eklendi
    @Mapping(target = "category", ignore = true) // Serviste atanacak
    @Mapping(target = "seller", ignore = true) // Serviste atanacak
    @Mapping(target = "orderItems", ignore = true) // İlişki listeleri
    @Mapping(target = "reviews", ignore = true)
    @Mapping(target = "wishlistItems", ignore = true)
    Product toProduct(ProductRequest productRequest);


    // ProductRequest'ten mevcut Product'ı güncelleme
    // ID, slug (genelde değişmez veya dikkatli değişir), createdAt, seller vb. güncellenmez.
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "slug", ignore = true) // Slug update mantığı serviste ele alınabilir
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true) // Otomatik güncellenecek
    @Mapping(target = "approved", ignore = true) // Admin işlevi
    @Mapping(target = "active", ignore = true) // Ayrı admin/seller işlevi
    @Mapping(target = "averageRating", ignore = true)
    @Mapping(target = "reviewCount", ignore = true)
    @Mapping(target = "reviewSummaryAi", ignore = true) // Ayrı AI işlevi
    @Mapping(target = "aiGeneratedImageUrl", ignore = true) // Ayrı AI işlevi
    @Mapping(target = "mainImageUrl", ignore = true) // Eklendi
    @Mapping(target = "productImages", ignore = true) // Eklendi
    @Mapping(target = "category", ignore = true) // Kategori ID'si değişirse serviste atanacak
    @Mapping(target = "seller", ignore = true) // Satıcı değişmemeli
    @Mapping(target = "orderItems", ignore = true) // İlişki listeleri
    @Mapping(target = "reviews", ignore = true)
    @Mapping(target = "wishlistItems", ignore = true)
    void updateProductFromRequest(ProductRequest request, @MappingTarget Product product);

}
