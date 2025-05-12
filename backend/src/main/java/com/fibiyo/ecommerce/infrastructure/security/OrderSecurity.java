package com.fibiyo.ecommerce.infrastructure.security;

import com.fibiyo.ecommerce.domain.entity.Order;
import com.fibiyo.ecommerce.domain.entity.User;
import com.fibiyo.ecommerce.domain.enums.Role;
import com.fibiyo.ecommerce.infrastructure.persistence.repository.OrderRepository;
import com.fibiyo.ecommerce.infrastructure.persistence.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("orderSecurity")
public class OrderSecurity {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    @Autowired
    public OrderSecurity(OrderRepository orderRepository, UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
    }

    public boolean hasPermission(Long orderId, Authentication authentication) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return false;

        if (user.getRole() == Role.ADMIN) return true;

        return orderRepository.findById(orderId)
                .map(order -> order.getOrderItems().stream()
                        .anyMatch(item -> item.getProduct() != null
                                && item.getProduct().getSeller().getId().equals(user.getId())))
                .orElse(false);
    }
}
