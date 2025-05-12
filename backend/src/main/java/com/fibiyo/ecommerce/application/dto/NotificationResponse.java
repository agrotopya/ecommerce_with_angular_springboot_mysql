package com.fibiyo.ecommerce.application.dto;

import com.fibiyo.ecommerce.domain.enums.NotificationType; // Enum'ı import et
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class NotificationResponse {
    private Long id;
    private String message;
    private String link;
    private boolean isRead;
    private LocalDateTime createdAt;
    private NotificationType type; // Enum olarak gönderelim
    // userId'yi genellikle göndermeyiz, çünkü bu "benim" bildirimlerim olacak.
    // private Long userId;
}