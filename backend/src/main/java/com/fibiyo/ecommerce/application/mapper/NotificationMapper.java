package com.fibiyo.ecommerce.application.mapper;

import com.fibiyo.ecommerce.application.dto.NotificationResponse;
import com.fibiyo.ecommerce.domain.entity.Notification;
import org.mapstruct.Mapper;
// import org.mapstruct.Mapping; // Direkt eşleşme olduğu için @Mapping'e gerek yok gibi

import java.util.List;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    // Notification -> NotificationResponse
    // @Mapping(source = "user.id", target = "userId") // DTO'da userId yoksa gerek yok
    NotificationResponse toNotificationResponse(Notification notification);

    List<NotificationResponse> toNotificationResponseList(List<Notification> notifications);

    // Notification oluşturma işlemi genellikle servis içinde manuel yapılır,
    // request DTO'su kullanmak yerine parametrelerle yapılabilir.
}