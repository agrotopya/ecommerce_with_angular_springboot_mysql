package com.fibiyo.ecommerce.application.mapper;

import com.fibiyo.ecommerce.application.dto.CartItemResponse;
import com.fibiyo.ecommerce.application.dto.CartResponse;
import com.fibiyo.ecommerce.domain.entity.Cart;
import com.fibiyo.ecommerce.domain.entity.CartItem;
import com.fibiyo.ecommerce.domain.entity.User;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-05-17T11:59:19+0300",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.42.0.v20250508-0718, environment: Java 21.0.7 (Eclipse Adoptium)"
)
@Component
public class CartMapperImpl implements CartMapper {

    @Autowired
    private CartItemMapper cartItemMapper;

    @Override
    public CartResponse toCartResponse(Cart cart) {
        if ( cart == null ) {
            return null;
        }

        CartResponse cartResponse = new CartResponse();

        cartResponse.setCartId( cart.getId() );
        cartResponse.setUserId( cartUserId( cart ) );
        cartResponse.setItems( cartItemListToCartItemResponseList( cart.getItems() ) );
        cartResponse.setUpdatedAt( cart.getUpdatedAt() );

        calculateTotals( cartResponse, cart );

        return cartResponse;
    }

    private Long cartUserId(Cart cart) {
        if ( cart == null ) {
            return null;
        }
        User user = cart.getUser();
        if ( user == null ) {
            return null;
        }
        Long id = user.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    protected List<CartItemResponse> cartItemListToCartItemResponseList(List<CartItem> list) {
        if ( list == null ) {
            return null;
        }

        List<CartItemResponse> list1 = new ArrayList<CartItemResponse>( list.size() );
        for ( CartItem cartItem : list ) {
            list1.add( cartItemMapper.toCartItemResponse( cartItem ) );
        }

        return list1;
    }
}
