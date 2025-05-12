package com.fibiyo.ecommerce.application.service;

import com.fibiyo.ecommerce.application.dto.NotificationResponse;
import com.fibiyo.ecommerce.application.dto.UnreadNotificationCountDto; // DTO import
import com.fibiyo.ecommerce.domain.entity.User; // Parametre için
import com.fibiyo.ecommerce.domain.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationService {

    // --- User Operations ---
    Page<NotificationResponse> findMyNotifications(Pageable pageable); // Kullanıcının bildirimleri
    UnreadNotificationCountDto getMyUnreadNotificationCount(); // Okunmamış sayısını getir
    NotificationResponse markNotificationAsRead(Long notificationId); // Tek bildirimi okundu yap
    int markAllMyNotificationsAsRead(); // Tümünü okundu yap, etkilenen satır sayısını dön
    void deleteNotification(Long notificationId); // Tek bildirimi sil (Kullanıcı için)

    // --- System/Service Operations (Internal Usage) ---
    // Diğer servislerin bildirim oluşturmak için çağıracağı metodlar
    void createNotification(User user, String message, String link, NotificationType type);
    // Örnek özel metodlar:
    // void sendOrderStatusUpdateNotification(Order order);
    // void sendNewPromotionNotification(Coupon coupon, List<User> targetUsers);

}