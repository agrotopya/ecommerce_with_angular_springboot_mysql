package com.fibiyo.ecommerce.infrastructure.security;

import com.fibiyo.ecommerce.domain.entity.User;
import com.fibiyo.ecommerce.infrastructure.persistence.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // ReadOnly transaction için

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service // Spring Security tarafından bulunabilmesi için @Service olmalı
public class CustomUserDetailsService implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(CustomUserDetailsService.class);

    @Autowired
    private UserRepository userRepository;

    // Spring Security bu metodu çağırır (genellikle login sırasında)
    @Override
    @Transactional // Veritabanından okuma yaptığı için readOnly transaction yeterli
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        logger.debug("Attempting to load user by username or email: {}", usernameOrEmail);
        User user = userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
                .orElseThrow(() -> {
                    logger.warn("User not found with username or email: {}", usernameOrEmail);
                    return new UsernameNotFoundException("User not found with username or email: " + usernameOrEmail);
                });

        // Roller "ROLE_" prefix'i ile oluşturulmalı (Spring Security standardı)
        Collection<? extends GrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
        );

         logger.info("User found: {} with roles: {}", user.getUsername(), authorities);

        // Spring Security'nin UserDetails implementasyonu olan User nesnesini dönüyoruz
        // return new org.springframework.security.core.userdetails.User(
        //         user.getUsername(),        // Principal olarak username kullanalım
        //         user.getPasswordHash(),    // Veritabanındaki hashlenmiş şifre
        //         user.isActive(),           // Hesap aktif mi? (Spring Security bunu kullanır)
        //         true,                      // accountNonExpired
        //         true,                      // credentialsNonExpired
        //         true,                      // accountNonLocked
        //         authorities);              // Kullanıcının rolleri
        return new UserPrincipal(user); // UserPrincipal döndür
    }

    // JWT filtresi tarafından token doğrulandıktan sonra kullanıcıyı ID ile yüklemek için kullanılabilir
    // Ancak filtrede genellikle username kullanıldığı için bu şart olmayabilir. İhtiyaç olursa açılabilir.
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

         Collection<? extends GrantedAuthority> authorities = Collections.singletonList(
                 new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
         );

         logger.info("User found by ID: {} with roles: {}", user.getUsername(), authorities);

         return new org.springframework.security.core.userdetails.User(
                 user.getUsername(),
                 user.getPasswordHash(),
                 user.isActive(),
                 true,
                 true,
                 true,
                 authorities);
    }
    */
}
