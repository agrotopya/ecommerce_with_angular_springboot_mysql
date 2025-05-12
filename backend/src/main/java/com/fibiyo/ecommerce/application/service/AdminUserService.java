package com.fibiyo.ecommerce.application.service;

import com.fibiyo.ecommerce.application.dto.UserResponse;
import com.fibiyo.ecommerce.domain.enums.Role; // Rol filtresi için
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminUserService {

    Page<UserResponse> findAllUsers(Pageable pageable, String searchTerm, Role role, Boolean isActive);

    UserResponse findUserById(Long userId);

    UserResponse updateUserActiveStatus(Long userId, boolean isActive);

    UserResponse updateUserRole(Long userId, Role newRole);

    // Opsiyonel: Kullanıcı silme (Dikkatli kullanılmalı! İlişkili veriler ne olacak?)
    // void deleteUser(Long userId); // Silme işlemi riskli olduğu için şimdilik implemente etmiyoruz

}