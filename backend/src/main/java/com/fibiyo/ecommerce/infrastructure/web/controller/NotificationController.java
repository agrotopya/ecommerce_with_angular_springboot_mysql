package com.fibiyo.ecommerce.infrastructure.web.controller;

import com.fibiyo.ecommerce.application.dto.ApiResponse;
import com.fibiyo.ecommerce.application.dto.NotificationResponse;
import com.fibiyo.ecommerce.application.dto.UnreadNotificationCountDto;
import com.fibiyo.ecommerce.application.service.NotificationService;
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
@RequestMapping("/api/notifications")
@PreAuthorize("isAuthenticated()") // Tüm bildirim endpoint'leri login gerektirir
public class NotificationController {

    private static final Logger logger = LoggerFactory.getLogger(NotificationController.class);

    private final NotificationService notificationService;

    @Autowired
    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    // Kullanıcının kendi bildirimlerini getir (Sayfalı)
    @GetMapping("/my")
    public ResponseEntity<Page<NotificationResponse>> getMyNotifications(
            @PageableDefault(size = 15, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
         logger.info("GET /api/notifications/my requested");
         Page<NotificationResponse> notifications = notificationService.findMyNotifications(pageable);
        return ResponseEntity.ok(notifications);
    }

    // Kullanıcının okunmamış bildirim sayısını getir
    @GetMapping("/my/unread-count")
    public ResponseEntity<UnreadNotificationCountDto> getMyUnreadCount() {
         logger.debug("GET /api/notifications/my/unread-count requested"); // Bu sık çağrılabilir, debug logu yeterli
        UnreadNotificationCountDto countDto = notificationService.getMyUnreadNotificationCount();
        return ResponseEntity.ok(countDto);
    }

    // Belirli bir bildirimi okundu olarak işaretle
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<NotificationResponse> markAsRead(@PathVariable Long notificationId) {
         logger.info("PATCH /api/notifications/{}/read requested", notificationId);
         NotificationResponse updatedNotification = notificationService.markNotificationAsRead(notificationId);
        return ResponseEntity.ok(updatedNotification);
    }

    // Tüm bildirimleri okundu olarak işaretle
    @PatchMapping("/my/read-all")
    public ResponseEntity<ApiResponse> markAllAsRead() {
        logger.info("PATCH /api/notifications/my/read-all requested");
        int markedCount = notificationService.markAllMyNotificationsAsRead();
         return ResponseEntity.ok(new ApiResponse(true, markedCount + " bildirim okundu olarak işaretlendi."));
    }


    // Belirli bir bildirimi sil
     @DeleteMapping("/{notificationId}")
    public ResponseEntity<ApiResponse> deleteNotification(@PathVariable Long notificationId) {
          logger.warn("DELETE /api/notifications/{} requested", notificationId);
          notificationService.deleteNotification(notificationId);
         return ResponseEntity.ok(new ApiResponse(true, "Bildirim başarıyla silindi."));
     }

    // --- NOT: Bildirim oluşturma endpoint'i genellikle public olmaz. ---
    // Bildirimler genellikle diğer servisler (OrderService, CouponService vb.) tarafından
    // NotificationService.createNotification() metodu çağrılarak oluşturulur.
    // Test amacıyla veya özel durumlarda bir POST endpoint eklenebilir (örn: Admin duyuru gönderme).

}