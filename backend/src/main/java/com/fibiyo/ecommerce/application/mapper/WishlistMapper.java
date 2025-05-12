package com.fibiyo.ecommerce.application.mapper;

import com.fibiyo.ecommerce.application.dto.ProductResponse; // Product'ın DTO'su
import com.fibiyo.ecommerce.domain.entity.WishlistItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", uses = {ProductMapper.class}) // ProductMapper'ı kullanabiliriz
public interface WishlistMapper {

    // WishlistItem -> ProductResponse
    // Bu biraz dolaylı, doğrudan ProductMapper'ı kullanmak daha mantıklı.
    // Ama örnek olarak:
     @Mapping(source = "product", target = ".") // WishlistItem'ın product alanını ProductResponse'a map'le (ProductMapper halleder)
     ProductResponse wishlistItemToProductResponse(WishlistItem wishlistItem);

     List<ProductResponse> wishlistItemsToProductResponseList(List<WishlistItem> wishlistItems);
}