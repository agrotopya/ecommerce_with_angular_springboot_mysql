package com.fibiyo.ecommerce.application.mapper;

import com.fibiyo.ecommerce.application.dto.response.FeelResponseDto;
import com.fibiyo.ecommerce.domain.entity.Feel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FeelMapper {

    @Mapping(source = "product.id", target = "productId")
    @Mapping(source = "product.name", target = "productName")
    @Mapping(source = "product.slug", target = "productSlug")
    @Mapping(source = "seller.id", target = "sellerId")
    @Mapping(source = "seller.username", target = "sellerUsername")
    // isActive alanı için özel mapping kaldırıldı, MapStruct'ın otomatik eşleştirmesi bekleniyor.
    // Lombok'un Feel entity'sindeki 'boolean active' alanı için 'isActive()' getter'ı oluşturması gerekir.
    // FeelResponseDto'daki alan adı 'isActive' olduğu için eşleşme sağlanmalı.
    FeelResponseDto toDto(Feel feel);

    // If you need to map from DTO to Entity, you can add methods here
    // For example:
    // Feel toEntity(FeelResponseDto feelResponseDto);
    // Feel toEntity(CreateFeelRequestDto createFeelRequestDto);
}
