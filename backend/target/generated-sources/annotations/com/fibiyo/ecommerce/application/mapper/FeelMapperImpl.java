package com.fibiyo.ecommerce.application.mapper;

import com.fibiyo.ecommerce.application.dto.response.FeelResponseDto;
import com.fibiyo.ecommerce.domain.entity.Feel;
import com.fibiyo.ecommerce.domain.entity.Product;
import com.fibiyo.ecommerce.domain.entity.User;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-05-17T12:02:50+0300",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.42.0.v20250508-0718, environment: Java 21.0.7 (Eclipse Adoptium)"
)
@Component
public class FeelMapperImpl implements FeelMapper {

    @Override
    public FeelResponseDto toDto(Feel feel) {
        if ( feel == null ) {
            return null;
        }

        FeelResponseDto feelResponseDto = new FeelResponseDto();

        feelResponseDto.setProductId( feelProductId( feel ) );
        feelResponseDto.setProductName( feelProductName( feel ) );
        feelResponseDto.setProductSlug( feelProductSlug( feel ) );
        feelResponseDto.setSellerId( feelSellerId( feel ) );
        feelResponseDto.setSellerUsername( feelSellerUsername( feel ) );
        feelResponseDto.setActive( feel.isActive() );
        feelResponseDto.setCreatedAt( feel.getCreatedAt() );
        feelResponseDto.setDescription( feel.getDescription() );
        feelResponseDto.setId( feel.getId() );
        feelResponseDto.setLikes( feel.getLikes() );
        feelResponseDto.setThumbnailUrl( feel.getThumbnailUrl() );
        feelResponseDto.setTitle( feel.getTitle() );
        feelResponseDto.setUpdatedAt( feel.getUpdatedAt() );
        feelResponseDto.setVideoUrl( feel.getVideoUrl() );
        feelResponseDto.setViews( feel.getViews() );

        return feelResponseDto;
    }

    private Long feelProductId(Feel feel) {
        if ( feel == null ) {
            return null;
        }
        Product product = feel.getProduct();
        if ( product == null ) {
            return null;
        }
        Long id = product.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    private String feelProductName(Feel feel) {
        if ( feel == null ) {
            return null;
        }
        Product product = feel.getProduct();
        if ( product == null ) {
            return null;
        }
        String name = product.getName();
        if ( name == null ) {
            return null;
        }
        return name;
    }

    private String feelProductSlug(Feel feel) {
        if ( feel == null ) {
            return null;
        }
        Product product = feel.getProduct();
        if ( product == null ) {
            return null;
        }
        String slug = product.getSlug();
        if ( slug == null ) {
            return null;
        }
        return slug;
    }

    private Long feelSellerId(Feel feel) {
        if ( feel == null ) {
            return null;
        }
        User seller = feel.getSeller();
        if ( seller == null ) {
            return null;
        }
        Long id = seller.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    private String feelSellerUsername(Feel feel) {
        if ( feel == null ) {
            return null;
        }
        User seller = feel.getSeller();
        if ( seller == null ) {
            return null;
        }
        String username = seller.getUsername();
        if ( username == null ) {
            return null;
        }
        return username;
    }
}
