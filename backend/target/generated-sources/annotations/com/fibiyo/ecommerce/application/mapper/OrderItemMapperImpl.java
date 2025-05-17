package com.fibiyo.ecommerce.application.mapper;

import com.fibiyo.ecommerce.application.dto.OrderItemRequest;
import com.fibiyo.ecommerce.application.dto.OrderItemResponse;
import com.fibiyo.ecommerce.domain.entity.OrderItem;
import com.fibiyo.ecommerce.domain.entity.Product;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-05-17T12:02:50+0300",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.42.0.v20250508-0718, environment: Java 21.0.7 (Eclipse Adoptium)"
)
@Component
class OrderItemMapperImpl implements OrderItemMapper {

    @Override
    public OrderItemResponse toOrderItemResponse(OrderItem orderItem) {
        if ( orderItem == null ) {
            return null;
        }

        OrderItemResponse orderItemResponse = new OrderItemResponse();

        orderItemResponse.setProductId( orderItemProductId( orderItem ) );
        orderItemResponse.setProductName( orderItemProductName( orderItem ) );
        orderItemResponse.setProductSlug( orderItemProductSlug( orderItem ) );
        orderItemResponse.setProductImageUrl( orderItemProductMainImageUrl( orderItem ) );
        orderItemResponse.setId( orderItem.getId() );
        orderItemResponse.setItemTotal( orderItem.getItemTotal() );
        orderItemResponse.setPriceAtPurchase( orderItem.getPriceAtPurchase() );
        orderItemResponse.setQuantity( orderItem.getQuantity() );

        return orderItemResponse;
    }

    @Override
    public List<OrderItemResponse> toOrderItemResponseList(List<OrderItem> orderItems) {
        if ( orderItems == null ) {
            return null;
        }

        List<OrderItemResponse> list = new ArrayList<OrderItemResponse>( orderItems.size() );
        for ( OrderItem orderItem : orderItems ) {
            list.add( toOrderItemResponse( orderItem ) );
        }

        return list;
    }

    @Override
    public OrderItem toOrderItem(OrderItemRequest request) {
        if ( request == null ) {
            return null;
        }

        OrderItem orderItem = new OrderItem();

        orderItem.setQuantity( request.getQuantity() );

        return orderItem;
    }

    private Long orderItemProductId(OrderItem orderItem) {
        if ( orderItem == null ) {
            return null;
        }
        Product product = orderItem.getProduct();
        if ( product == null ) {
            return null;
        }
        Long id = product.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    private String orderItemProductName(OrderItem orderItem) {
        if ( orderItem == null ) {
            return null;
        }
        Product product = orderItem.getProduct();
        if ( product == null ) {
            return null;
        }
        String name = product.getName();
        if ( name == null ) {
            return null;
        }
        return name;
    }

    private String orderItemProductSlug(OrderItem orderItem) {
        if ( orderItem == null ) {
            return null;
        }
        Product product = orderItem.getProduct();
        if ( product == null ) {
            return null;
        }
        String slug = product.getSlug();
        if ( slug == null ) {
            return null;
        }
        return slug;
    }

    private String orderItemProductMainImageUrl(OrderItem orderItem) {
        if ( orderItem == null ) {
            return null;
        }
        Product product = orderItem.getProduct();
        if ( product == null ) {
            return null;
        }
        String mainImageUrl = product.getMainImageUrl();
        if ( mainImageUrl == null ) {
            return null;
        }
        return mainImageUrl;
    }
}
