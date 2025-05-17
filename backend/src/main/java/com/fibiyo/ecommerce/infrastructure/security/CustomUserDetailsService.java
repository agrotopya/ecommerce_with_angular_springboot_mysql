package com.fibiyo.ecommerce.infrastructure.security;

import com.fibiyo.ecommerce.domain.entity.User;
import com.fibiyo.ecommerce.infrastructure.persistence.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
// GrantedAuthority ve SimpleGrantedAuthority User entity'sinden gelecek (UserDetails implementasyonu ile)
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Collection, Collections, Collectors, Stream importları User entity'sinde veya burada gereksiz olabilir
// User entity'si UserDetails'i implemente ettiği için authorities'i kendi içinde halledecek.

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(CustomUserDetailsService.class);

    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional(readOnly = true) // readOnly = true eklendi
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        logger.debug("Attempting to load user by username or email: {}", usernameOrEmail);
        User user = userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
                .orElseThrow(() -> {
                    logger.warn("User not found with username or email: {}", usernameOrEmail);
                    return new UsernameNotFoundException("User not found with username or email: " + usernameOrEmail);
                });

        // User sınıfı UserDetails'i implemente ettiği için doğrudan user nesnesini döndürebiliriz.
        // GrantedAuthority'ler User sınıfındaki getAuthorities() metodu tarafından sağlanacak.
        logger.info("User found: {} with roles: {}", user.getUsername(), user.getAuthorities());

        return user; // Doğrudan User nesnesini döndür
    }

    // loadUserById metodu JWT filtresi için gerekirse açılabilir ve benzer şekilde User döndürebilir.
    /*
    @Transactional(readOnly = true)
    public UserDetails loadUserById(Long id) {
         logger.debug("Attempting to load user by ID: {}", id);
        User user = userRepository.findById(id).orElseThrow(
                () -> {
                    logger.warn("User not found with ID: {}", id);
                    return new UsernameNotFoundException("User not found with id : " + id);
                }
        );
         logger.info("User found by ID: {} with roles: {}", user.getUsername(), user.getAuthorities());
         return user;
    }
    */
}
