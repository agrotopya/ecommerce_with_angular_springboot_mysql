package com.fibiyo.ecommerce.application.service.impl;

import com.fibiyo.ecommerce.application.dto.NotificationResponse;
import com.fibiyo.ecommerce.application.dto.UnreadNotificationCountDto;
import com.fibiyo.ecommerce.application.exception.ForbiddenException;
import com.fibiyo.ecommerce.application.exception.ResourceNotFoundException;
import com.fibiyo.ecommerce.application.mapper.NotificationMapper;
import com.fibiyo.ecommerce.application.service.NotificationService;
import com.fibiyo.ecommerce.domain.entity.Notification;
import com.fibiyo.ecommerce.domain.entity.User;
import com.fibiyo.ecommerce.domain.enums.NotificationType;
import com.fibiyo.ecommerce.infrastructure.persistence.repository.NotificationRepository;
import com.fibiyo.ecommerce.infrastructure.persistence.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Transactional gerekli

@Service
public class NotificationServiceImpl implements NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final NotificationMapper notificationMapper;


     // Helper
     private User getCurrentUser() {
          String username = SecurityContextHolder.getContext().getAuthentication().getName();
         // Kullanıcı "anonymousUser" ise hata fırlatmalı mıyız? Bildirim API'leri hep login gerektirir.
         if ("anonymousUser".equals(username)) {
             throw new ForbiddenException("Bu işlem için giriş yapmalısınız.");
          }
          return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found: " + username));
     }


    @Autowired
    public NotificationServiceImpl(NotificationRepository notificationRepository, UserRepository userRepository, NotificationMapper notificationMapper) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.notificationMapper = notificationMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> findMyNotifications(Pageable pageable) {
        User currentUser = getCurrentUser();
        logger.debug("Fetching notifications for user ID: {}", currentUser.getId());
        Page<Notification> notificationPage = notificationRepository.findByUserIdOrderByCreatedAtDesc(currentUser.getId(), pageable);
        return notificationPage.map(notificationMapper::toNotificationResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public UnreadNotificationCountDto getMyUnreadNotificationCount() {
        User currentUser = getCurrentUser();
        logger.trace("Getting unread notification count for user ID: {}", currentUser.getId()); // trace seviyesi yeterli
        long count = notificationRepository.countByUserIdAndIsReadFalse(currentUser.getId());
        return new UnreadNotificationCountDto(count);
    }

    @Override
    @Transactional
    public NotificationResponse markNotificationAsRead(Long notificationId) {
         User currentUser = getCurrentUser();
          logger.debug("User ID: {} marking notification ID: {} as read", currentUser.getId(), notificationId);
          Notification notification = notificationRepository.findById(notificationId)
                 .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + notificationId));

         // Bildirimin bu kullanıcıya ait olduğundan emin ol
          if (!notification.getUser().getId().equals(currentUser.getId())) {
               throw new ForbiddenException("Bu bildirimi okundu olarak işaretleme yetkiniz yok.");
          }

         // Zaten okunmuşsa tekrar kaydetmeye gerek yok
         if (!notification.isRead()) {
             notification.setRead(true);
             notification = notificationRepository.save(notification);
             logger.info("Notification ID: {} marked as read for User ID: {}", notificationId, currentUser.getId());
         } else {
              logger.debug("Notification ID: {} was already marked as read.", notificationId);
         }
          return notificationMapper.toNotificationResponse(notification);
    }

    @Override
    @Transactional
    public int markAllMyNotificationsAsRead() {
         User currentUser = getCurrentUser();
          logger.info("User ID: {} marking all notifications as read", currentUser.getId());
        // Repository'deki custom update query'sini kullan
        int affectedRows = notificationRepository.markAllAsReadByUserId(currentUser.getId());
         logger.info("{} notifications marked as read for User ID: {}", affectedRows, currentUser.getId());
        return affectedRows; // Kaç tanesi güncellendi bilgisini dönelim
    }

    @Override
    @Transactional
    public void deleteNotification(Long notificationId) {
        User currentUser = getCurrentUser();
        logger.warn("User ID: {} attempting to delete notification ID: {}", currentUser.getId(), notificationId);
         Notification notification = notificationRepository.findById(notificationId)
                 .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + notificationId));

         // Sahiplik kontrolü
         if (!notification.getUser().getId().equals(currentUser.getId())) {
             throw new ForbiddenException("Bu bildirimi silme yetkiniz yok.");
         }

        notificationRepository.delete(notification);
         logger.info("Notification ID: {} deleted successfully by User ID: {}", notificationId, currentUser.getId());
    }

    // --- System/Service Operations ---

    @Async
    @Override
    @Transactional // Ayrı transaction'da çalışması genellikle daha iyidir
    public void createNotification(User user, String message, String link, NotificationType type) {
         // Asenkron yapmak (örn: @Async) genellikle daha performanslıdır,
         // çünkü bildirim oluşturma işlemi ana işlemi (örn: sipariş verme) yavaşlatmamalı.
         // Ama başlangıçta senkron yapalım.
         logger.debug("Creating notification for User ID: {}, Type: {}", user.getId(), type);
         if(user == null){
             logger.error("Cannot create notification for null user.");
             return; // Hata logla ve çık
         }
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setMessage(message);
        notification.setLink(link);
        notification.setType(type);
        notification.setRead(false);
        notificationRepository.save(notification);
         logger.info("Notification created for User ID: {}. Message: '{}'", user.getId(), message.substring(0, Math.min(message.length(), 50)) + "..."); // Mesajın başını logla
         // TODO: Eğer gerçek zamanlı bir mekanizma varsa (WebSocket vb.) burada tetiklenebilir.
    }
}