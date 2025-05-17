package com.fibiyo.ecommerce.application.mapper;

import com.fibiyo.ecommerce.application.dto.OrderResponse;
import com.fibiyo.ecommerce.domain.entity.Coupon;
import com.fibiyo.ecommerce.domain.entity.Order;
import com.fibiyo.ecommerce.domain.entity.User;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-05-17T12:02:50+0300",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.42.0.v20250508-0718, environment: Java 21.0.7 (Eclipse Adoptium)"
)
@Component
public class OrderMapperImpl implements OrderMapper {

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Override
    public OrderResponse toOrderResponse(Order order) {
        if ( order == null ) {
            return null;
        }

        OrderResponse orderResponse = new OrderResponse();

        orderResponse.setCustomerId( orderCustomerId( order ) );
        orderResponse.setCustomerUsername( orderCustomerUsername( order ) );
        orderResponse.setCouponCode( orderCouponCode( order ) );
        orderResponse.setShippingAddress( jsonToAddressDto( order.getShippingAddress() ) );
        orderResponse.setBillingAddress( jsonToAddressDto( order.getBillingAddress() ) );
        orderResponse.setOrderItems( orderItemMapper.toOrderItemResponseList( order.getOrderItems() ) );
        orderResponse.setCreatedAt( order.getCreatedAt() );
        orderResponse.setDiscountAmount( order.getDiscountAmount() );
        orderResponse.setFinalAmount( order.getFinalAmount() );
        orderResponse.setId( order.getId() );
        orderResponse.setOrderDate( order.getOrderDate() );
        orderResponse.setPaymentMethod( order.getPaymentMethod() );
        orderResponse.setPaymentStatus( order.getPaymentStatus() );
        orderResponse.setShippingFee( order.getShippingFee() );
        orderResponse.setStatus( order.getStatus() );
        orderResponse.setTotalAmount( order.getTotalAmount() );
        orderResponse.setTrackingNumber( order.getTrackingNumber() );
        orderResponse.setUpdatedAt( order.getUpdatedAt() );

        return orderResponse;
    }

    @Override
    public List<OrderResponse> toOrderResponseList(List<Order> orders) {
        if ( orders == null ) {
            return null;
        }

        List<OrderResponse> list = new ArrayList<OrderResponse>( orders.size() );
        for ( Order order : orders ) {
            list.add( toOrderResponse( order ) );
        }

        return list;
    }

    private Long orderCustomerId(Order order) {
        if ( order == null ) {
            return null;
        }
        User customer = order.getCustomer();
        if ( customer == null ) {
            return null;
        }
        Long id = customer.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    private String orderCustomerUsername(Order order) {
        if ( order == null ) {
            return null;
        }
        User customer = order.getCustomer();
        if ( customer == null ) {
            return null;
        }
        String username = customer.getUsername();
        if ( username == null ) {
            return null;
        }
        return username;
    }

    private String orderCouponCode(Order order) {
        if ( order == null ) {
            return null;
        }
        Coupon coupon = order.getCoupon();
        if ( coupon == null ) {
            return null;
        }
        String code = coupon.getCode();
        if ( code == null ) {
            return null;
        }
        return code;
    }
}
