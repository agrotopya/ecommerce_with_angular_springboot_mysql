package com.fibiyo.ecommerce.domain.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection; // Eklendi
import java.util.List;
import java.util.stream.Collectors; // Eklendi

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority; // Eklendi
import org.springframework.security.core.authority.SimpleGrantedAuthority; // Eklendi
import org.springframework.security.core.userdetails.UserDetails; // Eklendi

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.fibiyo.ecommerce.domain.enums.AuthProvider;
import com.fibiyo.ecommerce.domain.enums.Role;
import com.fibiyo.ecommerce.domain.enums.SubscriptionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(columnNames = "username", name = "uk_user_username"),
        @UniqueConstraint(columnNames = "email", name = "uk_user_email"),
        @UniqueConstraint(name = "uk_user_provider_id", columnNames = {"auth_provider", "provider_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User implements UserDetails { // UserDetails implementasyonu eklendi

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Kullanıcı adı boş olamaz")
    @Size(min = 3, max = 100, message = "Kullanıcı adı 3 ile 100 karakter arasında olmalıdır")
    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @NotBlank(message = "E-posta boş olamaz")
    @Email(message = "Geçerli bir e-posta adresi giriniz")
    @Size(max = 255, message = "E-posta en fazla 255 karakter olabilir")
    @Column(nullable = false, unique = true)
    private String email;

    @Size(max = 255)
    @Column(name = "password_hash", nullable = true)
    @ToString.Exclude // Şifre toString'e dahil edilmesin
    private String passwordHash;

    @NotBlank(message = "İsim boş olamaz")
    @Size(max = 100, message = "İsim en fazla 100 karakter olabilir")
    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @NotBlank(message = "Soyisim boş olamaz")
    @Size(max = 100, message = "Soyisim en fazla 100 karakter olabilir")
    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @NotNull(message = "Rol boş olamaz")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role = Role.CUSTOMER;

    @NotNull
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider", nullable = true, length = 20)
    private AuthProvider authProvider;

    @Column(name = "provider_id", nullable = true, length = 255)
    private String providerId;

    @NotNull(message = "Abonelik türü boş olamaz")
    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_type", nullable = false, length = 20)
    private SubscriptionType subscriptionType = SubscriptionType.FREE;

    @Column(name = "subscription_expiry_date", nullable = true)
    private LocalDateTime subscriptionExpiryDate;

    @NotNull
    @Column(name = "loyalty_points", nullable = false)
    private int loyaltyPoints = 0;

    @NotNull
    @Column(name = "image_gen_quota", nullable = false)
    private int imageGenQuota = 3;

    @Column(name = "avatar", length = 1024)
    private String avatar;

    @OneToMany(mappedBy = "seller", fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Product> productsSold = new ArrayList<>();

    @OneToMany(mappedBy = "customer", fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Order> orders = new ArrayList<>();

    @OneToMany(mappedBy = "customer", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Review> reviews = new ArrayList<>();

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Notification> notifications = new ArrayList<>();

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<WishlistItem> wishlistItems = new ArrayList<>();

    @OneToMany(mappedBy = "seller", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Feel> feels = new ArrayList<>();

    @Column(name = "password_reset_token", nullable = true, length = 100)
    @ToString.Exclude
    private String passwordResetToken;

    @Column(name = "password_reset_token_expiry", nullable = true)
    private LocalDateTime passwordResetTokenExpiry;

    // UserDetails metodları
    @Override
    @Transient // Bu alan veritabanında bir sütuna karşılık gelmiyor
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Rolü GrantedAuthority listesine çevir
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return this.passwordHash;
    }

    // getUsername() metodu zaten Lombok @Data tarafından sağlanıyor (this.username)

    @Override
    @Transient
    public boolean isAccountNonExpired() {
        return true; // Veya ek bir alanla yönetilebilir
    }

    @Override
    @Transient
    public boolean isAccountNonLocked() {
        return true; // Veya ek bir alanla yönetilebilir
    }

    @Override
    @Transient
    public boolean isCredentialsNonExpired() {
        return true; // Veya ek bir alanla yönetilebilir
    }

    @Override
    @Transient
    public boolean isEnabled() {
        return this.isActive;
    }
}
