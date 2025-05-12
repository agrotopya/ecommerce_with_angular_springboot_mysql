package com.fibiyo.ecommerce.application.service.impl;

import com.fibiyo.ecommerce.application.dto.ChangePasswordRequest;
import com.fibiyo.ecommerce.application.dto.UserProfileUpdateRequest;
import com.fibiyo.ecommerce.application.dto.UserResponse;
import com.fibiyo.ecommerce.application.exception.BadRequestException;
import com.fibiyo.ecommerce.application.exception.ForbiddenException;
import com.fibiyo.ecommerce.application.exception.ResourceNotFoundException;
import com.fibiyo.ecommerce.application.mapper.UserMapper;
import com.fibiyo.ecommerce.application.service.UserService;
import com.fibiyo.ecommerce.domain.entity.User;
import com.fibiyo.ecommerce.infrastructure.persistence.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder; // Şifre kontrolü ve güncelleme için
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder; // Şifre işlemleri için


    // Helper
    private User getCurrentAuthenticatedUser() {
         String username = SecurityContextHolder.getContext().getAuthentication().getName();
         if ("anonymousUser".equals(username)) {
             throw new ForbiddenException("Bu işlem için giriş yapmalısınız.");
          }
          return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found in database: " + username));
     }

    @Autowired
    public UserServiceImpl(UserRepository userRepository, UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public UserResponse getCurrentUserProfile() {
        User currentUser = getCurrentAuthenticatedUser();
         logger.debug("Fetching profile for user: {}", currentUser.getUsername());
         return userMapper.toUserResponse(currentUser);
    }

    @Override
    @Transactional
    public UserResponse updateUserProfile(UserProfileUpdateRequest request) {
        User currentUser = getCurrentAuthenticatedUser();
         logger.info("User '{}' updating their profile.", currentUser.getUsername());

         // Mapper ile sadece izin verilen alanları (firstName, lastName) güncelle
         userMapper.updateUserFromProfileRequest(request, currentUser);

        // TODO: Email değişikliği istenirse burada ele alınmalı.
        // Yeni email unique mi? Yeni email'e doğrulama kodu gönderme vb.
         /*
        if (request.getEmail() != null && !request.getEmail().equalsIgnoreCase(currentUser.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new BadRequestException("Yeni e-posta adresi zaten kullanılıyor.");
            }
            // Doğrulama mekanizması işlet...
            currentUser.setEmail(request.getEmail());
            currentUser.setEmailVerified(false); // Doğrulama gerekli flag'i
        }
         */

         User updatedUser = userRepository.save(currentUser);
         logger.info("Profile updated successfully for user '{}'", updatedUser.getUsername());
        return userMapper.toUserResponse(updatedUser);
    }

    @Override
    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        User currentUser = getCurrentAuthenticatedUser();
        logger.info("User '{}' attempting to change password.", currentUser.getUsername());

        // Mevcut şifre doğru mu kontrol et
        if (!passwordEncoder.matches(request.getCurrentPassword(), currentUser.getPasswordHash())) {
             logger.warn("Incorrect current password provided for user '{}'", currentUser.getUsername());
            throw new BadRequestException("Mevcut şifreniz hatalı.");
        }

        // Yeni şifreyi hash'le ve kaydet
        currentUser.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(currentUser);
        logger.info("Password changed successfully for user '{}'", currentUser.getUsername());

        // Güvenlik Notu: Şifre değişikliği sonrası mevcut tüm oturumları (JWT'leri) geçersiz kılmak iyi bir pratiktir.
        // Bu daha ileri seviye bir konudur (token blacklist, JTI vb.).
    }
}