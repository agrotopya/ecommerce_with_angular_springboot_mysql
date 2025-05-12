package com.fibiyo.ecommerce.application.service;

import com.fibiyo.ecommerce.application.dto.ChangePasswordRequest;
import com.fibiyo.ecommerce.application.dto.UserProfileUpdateRequest;
import com.fibiyo.ecommerce.application.dto.UserResponse;

public interface UserService {

    UserResponse getCurrentUserProfile(); // Mevcut giriş yapmış kullanıcının profili

    UserResponse updateUserProfile(UserProfileUpdateRequest request); // Profil güncelleme

    void changePassword(ChangePasswordRequest request); // Şifre değiştirme

    // Opsiyonel: Admin için kullanıcı bulma metodu da buraya eklenebilir
    // UserResponse findUserById(Long userId);
}