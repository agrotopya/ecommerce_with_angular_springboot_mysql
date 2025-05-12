package com.fibiyo.ecommerce.infrastructure.web.controller;

import com.fibiyo.ecommerce.application.dto.ApiResponse;
import com.fibiyo.ecommerce.application.dto.UserResponse;
import com.fibiyo.ecommerce.application.dto.UserRoleUpdateRequest; // Rol DTO
import com.fibiyo.ecommerce.application.dto.UserStatusUpdateRequest; // Durum DTO
import com.fibiyo.ecommerce.application.service.AdminUserService;
import com.fibiyo.ecommerce.domain.enums.Role;
import jakarta.validation.Valid; // DTO validasyonu için
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/admin/users") // Admin'e özel path
@PreAuthorize("hasRole('ADMIN')") // Tüm endpointler ADMIN yetkisi gerektirir
public class AdminUserController {

    private static final Logger logger = LoggerFactory.getLogger(AdminUserController.class);

    private final AdminUserService adminUserService;

    @Autowired
    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    // Tüm kullanıcıları filtreli ve sayfalı getir
    @GetMapping
    public ResponseEntity<Page<UserResponse>> getAllUsers(
            @PageableDefault(size = 15, sort = "id", direction = Sort.Direction.ASC) Pageable pageable,
            @RequestParam(required = false) String search, // Arama terimi
            @RequestParam(required = false) Role role,     // Role göre filtre
            @RequestParam(required = false) Boolean active // Aktiflik durumuna göre filtre 
            // Parametre adını 'active' yaptık Service'e uygun olsun diye
    ) {
         logger.info("GET /api/admin/users requested with filters - Search: '{}', Role: {}, Active: {}", search, role, active);
         Page<UserResponse> users = adminUserService.findAllUsers(pageable, search, role, active);
         return ResponseEntity.ok(users);
    }

    // ID ile tek kullanıcı getir
    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long userId) {
         logger.info("GET /api/admin/users/{} requested", userId);
         UserResponse user = adminUserService.findUserById(userId);
         return ResponseEntity.ok(user);
    }

    // Kullanıcının aktiflik durumunu güncelle
     @PatchMapping("/{userId}/status")
     public ResponseEntity<UserResponse> updateUserStatus(
             @PathVariable Long userId,
              // @Valid @RequestBody UserStatusUpdateRequest request // Ayrı DTO kullanılabilir
              @RequestParam boolean isActive // Veya direkt parametre olarak al
      ) {
           logger.warn("PATCH /api/admin/users/{}/status requested with isActive={}", userId, isActive); // Durum değişikliği Warn olabilir
          UserResponse updatedUser = adminUserService.updateUserActiveStatus(userId, isActive);
          return ResponseEntity.ok(updatedUser);
      }


    // Kullanıcının rolünü güncelle
    @PatchMapping("/{userId}/role")
     public ResponseEntity<UserResponse> updateUserRole(
        
             @PathVariable Long userId,
              // @Valid @RequestBody UserRoleUpdateRequest request // Ayrı DTO kullanılabilir
             @RequestParam Role role // Direkt parametre
     ) {
         logger.warn("PATCH /api/admin/users/{}/role requested with newRole={}", userId, role); // Rol değişikliği Warn olabilir
         UserResponse updatedUser = adminUserService.updateUserRole(userId, role);
         return ResponseEntity.ok(updatedUser);
    }

     // Opsiyonel: Kullanıcı silme endpoint'i
     /*
     @DeleteMapping("/{userId}")
     public ResponseEntity<ApiResponse> deleteUser(@PathVariable Long userId) {
         logger.error("DELETE /api/admin/users/{} requested by ADMIN", userId); // Silme kritik, ERROR loglayalım
         adminUserService.deleteUser(userId);
          return ResponseEntity.ok(new ApiResponse(true, "Kullanıcı başarıyla silindi."));
      }
     */
}