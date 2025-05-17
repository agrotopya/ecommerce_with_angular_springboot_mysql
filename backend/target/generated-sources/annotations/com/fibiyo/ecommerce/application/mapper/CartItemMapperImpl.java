package com.fibiyo.ecommerce.application.mapper;

import com.fibiyo.ecommerce.application.dto.CartItemResponse;
import com.fibiyo.ecommerce.domain.entity.CartItem;
import com.fibiyo.ecommerce.domain.entity.Product;
import java.math.BigDecimal;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-05-17T12:02:50+0300",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.42.0.v20250508-0718, environment: Java 21.0.7 (Eclipse Adoptium)"
)
@Component
public class CartItemMapperImpl implements CartItemMapper {

    @Override
    public CartItemResponse toCartItemResponse(CartItem cartItem) {
        if ( cartItem == null ) {
            return null;
        }

        CartItemResponse cartItemResponse = new CartItemResponse();

        cartItemResponse.setCartItemId( cartItem.getId() );
        cartItemResponse.setProductId( cartItemProductId( cartItem ) );
        cartItemResponse.setProductName( cartItemProductName( cartItem ) );
        cartItemResponse.setProductSlug( cartItemProductSlug( cartItem ) );
        cartItemResponse.setProductPrice( cartItemProductPrice( cartItem ) );
        cartItemResponse.setProductStock( cartItemProductStock( cartItem ) );
        cartItemResponse.setProductImageUrl( cartItemProductMainImageUrl( cartItem ) );
        cartItemResponse.setAddedAt( cartItem.getAddedAt() );
        cartItemResponse.setQuantity( cartItem.getQuantity() );

        cartItemResponse.setItemTotal( calculateItemTotal(cartItem) );

        return cartItemResponse;
    }

    private Long cartItemProductId(CartItem cartItem) {
        if ( cartItem == null ) {
            return null;
        }
        Product product = cartItem.getProduct();
        if ( product == null ) {
            return null;
        }
        Long id = product.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    private String cartItemProductName(CartItem cartItem) {
        if ( cartItem == null ) {
            return null;
        }
        Product product = cartItem.getProduct();
        if ( product == null ) {
            return null;
        }
        String name = product.getName();
        if ( name == null ) {
            return null;
        }
        return name;
    }

    private String cartItemProductSlug(CartItem cartItem) {
        if ( cartItem == null ) {
            return null;
        }
        Product product = cartItem.getProduct();
        if ( product == null ) {
            return null;
        }
        String slug = product.getSlug();
        if ( slug == null ) {
            return null;
        }
        return slug;
    }

    private BigDecimal cartItemProductPrice(CartItem cartItem) {
        if ( cartItem == null ) {
            return null;
        }
        Product product = cartItem.getProduct();
        if ( product == null ) {
            return null;
        }
        BigDecimal price = product.getPrice();
        if ( price == null ) {
            return null;
        }
        return price;
    }

    private Integer cartItemProductStock(CartItem cartItem) {
        if ( cartItem == null ) {
            return null;
        }
        Product product = cartItem.getProduct();
        if ( product == null ) {
            return null;
        }
        Integer stock = product.getStock();
        if ( stock == null ) {
            return null;
        }
        return stock;
    }

    private String cartItemProductMainImageUrl(CartItem cartItem) {
        if ( cartItem == null ) {
            return null;
        }
        Product product = cartItem.getProduct();
        if ( product == null ) {
            return null;
        }
        String mainImageUrl = product.getMainImageUrl();
        if ( mainImageUrl == null ) {
            return null;
        }
        return mainImageUrl;
    }
}
