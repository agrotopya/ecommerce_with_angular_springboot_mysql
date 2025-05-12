package com.fibiyo.ecommerce.infrastructure.security;

import com.fibiyo.ecommerce.domain.entity.Product;
import com.fibiyo.ecommerce.domain.entity.User;
import com.fibiyo.ecommerce.domain.enums.Role;
import com.fibiyo.ecommerce.infrastructure.persistence.repository.ProductRepository;
import com.fibiyo.ecommerce.infrastructure.persistence.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("productSecurity")
public class ProductSecurity {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    public boolean hasPermission(Long productId, Authentication authentication) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return false;

        if (user.getRole() == Role.ADMIN) return true;

        return productRepository.findById(productId)
                .map(product -> product.getSeller().getId().equals(user.getId()))
                .orElse(false);
    }
}
