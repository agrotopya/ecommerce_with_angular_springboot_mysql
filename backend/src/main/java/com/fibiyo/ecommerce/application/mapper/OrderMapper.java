package com.fibiyo.ecommerce.application.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fibiyo.ecommerce.application.dto.*;
import com.fibiyo.ecommerce.domain.entity.*;
import org.mapstruct.*; // Mapping, Context vs.

import java.util.List;

// MapStruct ayarları
@Mapper(componentModel = "spring", uses = {OrderItemMapper.class}) // OrderItemMapper'ı kullanacak
public interface OrderMapper {

    // --- Entity to Response ---

    @Mapping(source = "customer.id", target = "customerId")
    @Mapping(source = "customer.username", target = "customerUsername")
    @Mapping(source = "coupon.code", target = "couponCode")
     // JSON string'leri AddressDto'ya maplemek için özel metot kullan
    @Mapping(target = "shippingAddress", qualifiedByName = "jsonToAddressDto")
    @Mapping(target = "billingAddress", qualifiedByName = "jsonToAddressDto")
    @Mapping(target = "orderItems", source = "orderItems") //Uses ile OrderItemMapper kullanılır
    OrderResponse toOrderResponse(Order order);

    List<OrderResponse> toOrderResponseList(List<Order> orders);

    // --- Request to Entity (createOrder için direkt kullanılmayacak, manuel oluşturulacak) ---
    // OrderRequest direkt Order'a maplenmez çünkü totalAmount, status, customer vb.
    // request'te yok, servis katmanında hesaplanıp set edilmeli.


     // --- Özel Dönüşüm Metotları ---

    // Bu metotlar JSON string'i AddressDto'ya dönüştürür.
     @Named("jsonToAddressDto")
     default AddressDto jsonToAddressDto(String jsonString) {
         if (jsonString == null || jsonString.isBlank()) {
             return null;
         }
         // ObjectMapper DI ile alınabilir veya burada new instance oluşturulabilir.
         // DI ile almak daha best practice. Şimdilik new instance kullanalım.
         ObjectMapper objectMapper = new ObjectMapper();
         try {
             return objectMapper.readValue(jsonString, AddressDto.class);
         } catch (JsonProcessingException e) {
             // Hata loglanabilir veya null dönülebilir.
             System.err.println("Error parsing address JSON: " + e.getMessage());
             return null; // veya özel bir hata fırlatılabilir
         }
     }
     // Bu metotlar AddressDto'yu JSON string'e dönüştürür (Entity'e set etmek için).
     @Named("addressDtoToJson")
     default String addressDtoToJson(AddressDto addressDto) {
          if (addressDto == null) {
             return null;
          }
         ObjectMapper objectMapper = new ObjectMapper();
          try {
             return objectMapper.writeValueAsString(addressDto);
          } catch (JsonProcessingException e) {
              System.err.println("Error serializing address DTO: " + e.getMessage());
              return null; // veya özel bir hata fırlatılabilir
          }
      }

}

// Ayrı bir OrderItemMapper interface'i oluşturalım
@Mapper(componentModel = "spring")
interface OrderItemMapper {
    @Mapping(source = "product.id", target = "productId")
    @Mapping(source = "product.name", target = "productName")
    @Mapping(source = "product.slug", target = "productSlug")
    @Mapping(source = "product.mainImageUrl", target = "productImageUrl") // imageUrl -> mainImageUrl
    OrderItemResponse toOrderItemResponse(OrderItem orderItem);

    List<OrderItemResponse> toOrderItemResponseList(List<OrderItem> orderItems);

    // OrderItemRequest -> OrderItem (Service içinde product set edilecek)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "priceAtPurchase", ignore = true) // Serviste atanacak
    @Mapping(target = "itemTotal", ignore = true)
    @Mapping(target = "order", ignore = true)
    @Mapping(target = "product", ignore = true) // Serviste atanacak
    OrderItem toOrderItem(OrderItemRequest request);
}
