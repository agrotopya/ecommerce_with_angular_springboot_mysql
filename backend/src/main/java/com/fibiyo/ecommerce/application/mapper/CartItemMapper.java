package com.fibiyo.ecommerce.application.mapper;

import com.fibiyo.ecommerce.application.dto.CartItemResponse;
import com.fibiyo.ecommerce.domain.entity.CartItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import java.math.BigDecimal; // Manuel hesaplama için

@Mapper(componentModel = "spring")
public interface CartItemMapper {

    @Mapping(source = "id", target = "cartItemId") // Entity id -> DTO cartItemId
    @Mapping(source = "product.id", target = "productId")
    @Mapping(source = "product.name", target = "productName")
    @Mapping(source = "product.slug", target = "productSlug")
    @Mapping(source = "product.price", target = "productPrice")
    @Mapping(source = "product.stock", target = "productStock")
    @Mapping(source = "product.mainImageUrl", target = "productImageUrl") // imageUrl -> mainImageUrl
    @Mapping(target = "itemTotal", expression = "java(calculateItemTotal(cartItem))") // Toplamı hesapla
    CartItemResponse toCartItemResponse(CartItem cartItem);

    // Toplam tutarı hesaplayan helper metot
    default BigDecimal calculateItemTotal(CartItem cartItem) {
        if (cartItem == null || cartItem.getProduct() == null || cartItem.getQuantity() == null) {
            return BigDecimal.ZERO;
        }
        return cartItem.getProduct().getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
    }
}
