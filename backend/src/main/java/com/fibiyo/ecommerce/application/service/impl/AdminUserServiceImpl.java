package com.fibiyo.ecommerce.application.service.impl;

import com.fibiyo.ecommerce.application.dto.UserResponse;
import com.fibiyo.ecommerce.application.exception.BadRequestException;
import com.fibiyo.ecommerce.application.exception.ForbiddenException;
import com.fibiyo.ecommerce.application.exception.ResourceNotFoundException;
import com.fibiyo.ecommerce.application.mapper.UserMapper;
import com.fibiyo.ecommerce.application.service.AdminUserService;
import com.fibiyo.ecommerce.domain.entity.User;
import com.fibiyo.ecommerce.domain.enums.Role;
import com.fibiyo.ecommerce.infrastructure.persistence.repository.UserRepository;
import com.fibiyo.ecommerce.infrastructure.persistence.specification.UserSpecifications; // User için Specification
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class AdminUserServiceImpl implements AdminUserService {

    private static final Logger logger = LoggerFactory.getLogger(AdminUserServiceImpl.class);

    private final UserRepository userRepository;
    private final UserMapper userMapper;


    // Helper
    private User getCurrentUser() {
         String username = SecurityContextHolder.getContext().getAuthentication().getName();
          // Bu metod sadece admin işlemleri için, giriş yapılmış ve user bulunmalı
         return userRepository.findByUsername(username)
                 .orElseThrow(() -> new ResourceNotFoundException("Current Admin user not found: " + username));
    }

    // private void checkAdminRole() {
    //      // Aslında bu servis sadece Admin tarafından çağrılacağı için @PreAuthorize yeterli olabilir,
    //      // ama çift kontrol güvenlik açısından iyidir.
    //      User currentUser = getCurrentUser();
    //     if (currentUser.getRole() != Role.ADMIN) {
    //         logger.warn("Non-admin user '{}' attempted admin user operation.", currentUser.getUsername());
    //         throw new ForbiddenException("Bu işlemi gerçekleştirmek için Admin yetkisine sahip olmalısınız.");
    //     }
    //  }


    @Autowired
    public AdminUserServiceImpl(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }




    @Override
    @Transactional
    public Page<UserResponse> findAllUsers(Pageable pageable, String searchTerm, Role role, Boolean isActive) {
        // 1. Yetki kontrolüne gerek yok, controller'da @PreAuthorize yeterli.
        // Veya burada checkAdminRole() çağrılabilir.
            // checkAdminRole(); // Controller'da PreAuthorize varsa burada şart değil

        logger.debug("Admin fetching all users. Filters - Search: '{}', Role: {}, IsActive: {}, Pageable: {}",
                searchTerm, role, isActive, pageable);

         // 2. Specification ile filtreleme
         Specification<User> spec = Specification.where(UserSpecifications.searchByTerm(searchTerm)) // User Specifications kullanılacak
                .and(UserSpecifications.hasRole(role))
                .and(UserSpecifications.isActive(isActive));

         // 3. Repository Çağrısı
        Page<User> userPage = userRepository.findAll(spec, pageable); // Specification'ı kullan
            
        logger.debug("Found {} users matching criteria.", userPage.getTotalElements());

         // 4. DTO Dönüşümü
        return userPage.map(userMapper::toUserResponse);
    }

    @Override
    @Transactional
    public UserResponse findUserById(Long userId) {
        // checkAdminRole(); // Controller'da @PreAuthorize yeterli olabilir.
         logger.debug("Admin fetching user by ID: {}", userId);
         User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
         return userMapper.toUserResponse(user);
    }

    @Override
    @Transactional
    public UserResponse updateUserActiveStatus(Long userId, boolean isActive) {
         // checkAdminRole();
         logger.info("Admin setting active status to {} for user ID: {}", isActive, userId);
         User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

         // Admin kendi hesabını pasifleştirmemeli? (Opsiyonel kontrol)
         User currentUser = getCurrentUser();
         if (currentUser.getId().equals(userId) && !isActive) {
             logger.warn("Admin attempted to deactivate their own account. Action blocked.");
             throw new BadRequestException("Admin kendi hesabını pasifleştiremez.");
         }
 


         if (user.getRole() == Role.ADMIN && !isActive) {
             logger.warn("Admin attempted to deactivate another admin (ID: {}). This might be risky.", userId);

             // Başka adminleri pasifleştirmek engellenebilir veya loglanabilir.
         }

         user.setActive(isActive);
         User updatedUser = userRepository.save(user);
        logger.info("User ID: {} active status updated to {} by admin.", userId, isActive);
         return userMapper.toUserResponse(updatedUser);
    }

    @Override
    @Transactional
    public UserResponse updateUserRole(Long userId, Role newRole) {
        // checkAdminRole();
        logger.info("Admin changing role for user ID: {} to {}", userId, newRole);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Admin kendi rolünü değiştirmemeli? Veya en az bir admin kalmalı kontrolü?
         // User currentUser = getCurrentUser(); if (currentUser.getId().equals(userId)) {...}

         if (user.getRole() == newRole) {
            logger.warn("User ID: {} already has the role {}. No change needed.", userId, newRole);
             return userMapper.toUserResponse(user); // Değişiklik yoksa direkt dön
         }

              if (user.getRole() == Role.ADMIN && newRole != Role.ADMIN && userRepository.countByRole(Role.ADMIN) <= 1) {
            logger.error("Attempt to demote the last remaining admin. Action blocked.");
            throw new BadRequestException("Sistemde en az bir admin kalmalı. Son admin'in rolü değiştirilemez.");
        }


         logger.warn("Changing role of user ID: {} from {} to {}", userId, user.getRole(), newRole);
        user.setRole(newRole);
         User updatedUser = userRepository.save(user);
         logger.info("Role changed successfully for user ID: {}.", userId);
         return userMapper.toUserResponse(updatedUser);
    }

     // Opsiyonel: Silme işlemi
     /*
     @Override
     @Transactional
     public void deleteUser(Long userId) {
          checkAdminRole();
          logger.warn("ADMIN: Attempting to DELETE user ID: {}", userId);
           User userToDelete = userRepository.findById(userId)
                  .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

         // Admin kendini silemez
         User currentUser = getCurrentUser();
          if(currentUser.getId().equals(userToDelete.getId())){
               throw new BadRequestException("Admin cannot delete their own account.");
          }

         // TODO: İlişkili verilerin (sipariş, ürün, yorum vb.) ne olacağı düşünülmeli.
         // Silmek yerine isActive=false yapmak çoğu zaman daha güvenlidir.
         // Veya ilişkili verileri anonimleştirmek gerekebilir.
          userRepository.delete(userToDelete);
         logger.info("ADMIN: User ID: {} deleted successfully.", userId);
     }
     */

}