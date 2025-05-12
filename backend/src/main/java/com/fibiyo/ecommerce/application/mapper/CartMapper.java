package com.fibiyo.ecommerce.application.mapper;

import com.fibiyo.ecommerce.application.dto.CartItemResponse;
import com.fibiyo.ecommerce.application.dto.CartResponse;
import com.fibiyo.ecommerce.domain.entity.Cart;
import org.mapstruct.AfterMapping; // Mapping sonrası işlem için
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget; // @AfterMapping için

import java.math.BigDecimal;
import java.util.List;

@Mapper(componentModel = "spring", uses = {CartItemMapper.class}) // CartItemMapper'ı kullanır
public interface CartMapper {

    @Mapping(source = "id", target = "cartId")
    @Mapping(source = "user.id", target = "userId")
    @Mapping(target = "items", source = "items") // CartItemMapper halleder
    @Mapping(target = "cartSubtotal", ignore = true) // Mapping sonrası hesaplanacak
    @Mapping(target = "totalItems", ignore = true) // Mapping sonrası hesaplanacak
    CartResponse toCartResponse(Cart cart);

    // Mapping tamamlandıktan sonra ara toplamı ve toplam ürün sayısını hesapla
    @AfterMapping
    default void calculateTotals(@MappingTarget CartResponse cartResponse, Cart cart) {
        if (cartResponse == null || cartResponse.getItems() == null) {
             cartResponse.setCartSubtotal(BigDecimal.ZERO);
             cartResponse.setTotalItems(0);
             return;
        }
        BigDecimal subtotal = BigDecimal.ZERO;
        int totalItemsCount = 0;
        for (CartItemResponse item : cartResponse.getItems()) {
            subtotal = subtotal.add(item.getItemTotal()); // itemTotal DTO'da hesaplanmıştı
            totalItemsCount += item.getQuantity();
        }
        cartResponse.setCartSubtotal(subtotal);
        cartResponse.setTotalItems(totalItemsCount);
    }
}