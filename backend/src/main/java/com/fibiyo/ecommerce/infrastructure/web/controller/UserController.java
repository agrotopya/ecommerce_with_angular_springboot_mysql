package com.fibiyo.ecommerce.infrastructure.web.controller;

import com.fibiyo.ecommerce.application.dto.ApiResponse;
import com.fibiyo.ecommerce.application.dto.ChangePasswordRequest;
import com.fibiyo.ecommerce.application.dto.UserProfileUpdateRequest;
import com.fibiyo.ecommerce.application.dto.UserResponse;
import com.fibiyo.ecommerce.application.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;



@RestController
@RequestMapping("/api/users") // Genel user endpoint'leri (profile, password vb.)
@PreAuthorize("isAuthenticated()") // Bu endpoint'ler login gerektirir
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    // Mevcut kullanıcının profilini getir
    @GetMapping("/me") // /api/users/me
    public ResponseEntity<UserResponse> getCurrentUserProfile() {
         logger.info("GET /api/users/me requested");
        UserResponse userProfile = userService.getCurrentUserProfile();
        return ResponseEntity.ok(userProfile);
    }

    // Mevcut kullanıcının profilini güncelle
    @PutMapping("/me") // /api/users/me
    public ResponseEntity<UserResponse> updateUserProfile(@Valid @RequestBody UserProfileUpdateRequest request) {
         logger.info("PUT /api/users/me requested");
        UserResponse updatedProfile = userService.updateUserProfile(request);
        return ResponseEntity.ok(updatedProfile);
    }

    // Mevcut kullanıcının şifresini değiştir
    @PatchMapping("/me/password") // /api/users/me/password
    public ResponseEntity<ApiResponse> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        logger.info("PATCH /api/users/me/password requested");
        userService.changePassword(request);
        return ResponseEntity.ok(new ApiResponse(true, "Şifre başarıyla değiştirildi."));
    }

     // TODO: Kullanıcının adreslerini yönetmek için endpoint'ler eklenebilir
     // @GetMapping("/me/addresses")

     


     // @PostMapping("/me/addresses")
     // @PutMapping("/me/addresses/{addressId}")
     // @DeleteMapping("/me/addresses/{addressId}")



     
}