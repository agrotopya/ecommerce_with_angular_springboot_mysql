package com.fibiyo.ecommerce.application.mapper;

import com.fibiyo.ecommerce.application.dto.NotificationResponse;
import com.fibiyo.ecommerce.domain.entity.Notification;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-05-17T11:59:19+0300",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.42.0.v20250508-0718, environment: Java 21.0.7 (Eclipse Adoptium)"
)
@Component
public class NotificationMapperImpl implements NotificationMapper {

    @Override
    public NotificationResponse toNotificationResponse(Notification notification) {
        if ( notification == null ) {
            return null;
        }

        NotificationResponse notificationResponse = new NotificationResponse();

        notificationResponse.setCreatedAt( notification.getCreatedAt() );
        notificationResponse.setId( notification.getId() );
        notificationResponse.setLink( notification.getLink() );
        notificationResponse.setMessage( notification.getMessage() );
        notificationResponse.setRead( notification.isRead() );
        notificationResponse.setType( notification.getType() );

        return notificationResponse;
    }

    @Override
    public List<NotificationResponse> toNotificationResponseList(List<Notification> notifications) {
        if ( notifications == null ) {
            return null;
        }

        List<NotificationResponse> list = new ArrayList<NotificationResponse>( notifications.size() );
        for ( Notification notification : notifications ) {
            list.add( toNotificationResponse( notification ) );
        }

        return list;
    }
}
