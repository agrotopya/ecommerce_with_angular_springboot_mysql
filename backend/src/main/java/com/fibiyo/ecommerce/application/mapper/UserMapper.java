package com.fibiyo.ecommerce.application.mapper;

import com.fibiyo.ecommerce.application.dto.UserResponse;
import com.fibiyo.ecommerce.application.dto.UserProfileUpdateRequest;
// RegisterRequest maplemeye gerek yok, serviste manuel oluşturuluyor.
import com.fibiyo.ecommerce.domain.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.Collections;
import java.util.List;

@Mapper(componentModel = "spring",
        imports = {java.util.Collections.class, java.util.List.class, com.fibiyo.ecommerce.domain.enums.Role.class},
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE) // Update'te null gelenleri atla
public interface UserMapper {

    // User -> UserResponse
    @Mapping(target = "roles", expression = "java(user.getRole() != null ? List.of(\"ROLE_\" + user.getRole().name()) : Collections.emptyList())")
    @Mapping(target = "imageGenQuota", expression = "java(user.getRole() == com.fibiyo.ecommerce.domain.enums.Role.SELLER ? user.getImageGenQuota() : null)") // Sadece satıcı ise kotayı göster
    UserResponse toUserResponse(User user);

    List<UserResponse> toUserResponseList(List<User> users);

    // UserProfileUpdateRequest -> User (Update için)
    // Sadece izin verilen alanları map'le
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "username", ignore = true)
    @Mapping(target = "email", ignore = true) // E-posta update özel işlem gerektirir
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "authProvider", ignore = true)
    @Mapping(target = "providerId", ignore = true)
    @Mapping(target = "subscriptionType", ignore = true)
    @Mapping(target = "subscriptionExpiryDate", ignore = true)
    @Mapping(target = "loyaltyPoints", ignore = true)
    @Mapping(target = "imageGenQuota", ignore = true)
    @Mapping(target = "productsSold", ignore = true)
    @Mapping(target = "orders", ignore = true)
    @Mapping(target = "reviews", ignore = true)
    @Mapping(target = "notifications", ignore = true)
    @Mapping(target = "wishlistItems", ignore = true)
    void updateUserFromProfileRequest(UserProfileUpdateRequest request, @MappingTarget User user);

}
