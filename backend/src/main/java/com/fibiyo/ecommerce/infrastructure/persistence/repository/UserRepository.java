package com.fibiyo.ecommerce.infrastructure.persistence.repository;
import com.fibiyo.ecommerce.domain.enums.Role;
import com.fibiyo.ecommerce.domain.enums.SubscriptionType;
import com.fibiyo.ecommerce.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor; // Dinamik sorgular için
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository // Bu annotation'ı eklemek iyi bir pratiktir, Spring bean olarak tanınmasını sağlar.
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> { // User entity ve ID tipi (Long)

    // Spring Data JPA, method isimlendirmesinden sorguyu otomatik oluşturur:
    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    // Kullanıcı adı veya email ile bulma (Login için kullanışlı)
    Optional<User> findByUsernameOrEmail(String username, String email);

    // Belirli bir provider ve providerId ile kullanıcı bulma (Sosyal medya login)
    Optional<User> findByAuthProviderAndProviderId(com.fibiyo.ecommerce.domain.enums.AuthProvider provider, String providerId);

    Boolean existsByUsername(String username);
// ... (UserRepository içinde) ...
List<User> findBySubscriptionTypeNotAndSubscriptionExpiryDateBefore(SubscriptionType type, LocalDateTime dateTime);
    Boolean existsByEmail(String email);
    long countByRole(Role role); // bu eklenecek
Optional<User> findByPasswordResetToken(String token); // Token ile kullanıcı bulma metodu
}

//Not: Kullanıcıları filtrelemek (rol, abonelik vb.) gerekebileceği için JpaSpecificationExecutor ekledik.
