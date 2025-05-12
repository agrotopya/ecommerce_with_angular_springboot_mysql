package com.fibiyo.ecommerce.domain.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;


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
        @UniqueConstraint(columnNames = "username", name = "uk_user_username"), // Kısıtlamaya isim vermek best practice tir
        @UniqueConstraint(columnNames = "email", name = "uk_user_email"),
        @UniqueConstraint(name = "uk_user_provider_id", columnNames = {"auth_provider", "provider_id"})
})
@Data // Lombok: Getter, Setter, equals, hashCode, toString
@NoArgsConstructor // Lombok: Boş constructor
@AllArgsConstructor // Lombok: Tüm alanları içeren constructor
public class User {

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

    @Size(max = 255) // BCrypt hash genellikle 60 karakterdir, ama geniş tutmak iyidir
    @Column(name = "password_hash", nullable = true) // Sosyal medya ile girişte null olabilir
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
    @Enumerated(EnumType.STRING) // Enum ı veritabanına String olarak kaydet
    @Column(nullable = false, length = 20)
    private Role role = Role.CUSTOMER; // Varsayılan rol

    @NotNull
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true; // Varsayılan olarak aktif

    @CreationTimestamp // Otomatik oluşturma zamanı (ilk kayıt)
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp // Otomatik güncelleme zamanı
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // --- Sosyal Medya Login ---
    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider", nullable = true, length = 20)
    private AuthProvider authProvider;

    @Column(name = "provider_id", nullable = true, length = 255) // Sağlayıcının kullanıcı ID si
    private String providerId;

    // --- Abonelik ---
    @NotNull(message = "Abonelik türü boş olamaz")
    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_type", nullable = false, length = 20)
    private SubscriptionType subscriptionType = SubscriptionType.FREE; // Varsayılan abonelik

    @Column(name = "subscription_expiry_date", nullable = true)
    private LocalDateTime subscriptionExpiryDate;

    // --- Diğer Özellikler ---
    @NotNull
    @Column(name = "loyalty_points", nullable = false)
    private int loyaltyPoints = 0; // Başlangıç sadakat puanı

    @NotNull
    @Column(name = "image_gen_quota", nullable = false)
    private int imageGenQuota = 3; // Satıcılar için varsayılan AI imaj hakkı

    @Column(name = "avatar", length = 1024)
    private String avatar; // Avatar URL

    // =============================================
    // İLİŞKİLER (Relationships)
    // =============================================
    // Bir kullanıcının sattığı ürünler (Seller ise)
    // mappedBy: Product entity sindeki User referansının adı ("seller")
    // fetch: LAZY loading önerilir, ihtiyaç oldukça yüklenir
    // cascade: Kullanıcı silinince ürünleri de silinsin mi? (CASCADE) Yoksa ürünleri pasif mi yapmalı? Proje gereksinimine bağlı.
    //          Şimdilik, ürünlerin pasif yapılması veya başka bir satıcıya devri daha mantıklı olabilir. Bu yüzden cascade koymuyoruz, silme işlemi serviste ele alınır.
    @OneToMany(mappedBy = "seller", fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Product> productsSold = new ArrayList<>(); // NullPointerException önlemek için initialize et

    // Kullanıcının verdiği siparişler (Customer ise)
    // mappedBy: Order entity sindeki User referansının adı ("customer")
    // cascade: Kullanıcı silinince siparişleri de silinsin mi? Genellikle sipariş kayıtları saklanır. (RESTRICT) Bu yüzden cascade koymuyoruz.
    @OneToMany(mappedBy = "customer", fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Order> orders = new ArrayList<>();

    // Kullanıcının yazdığı yorumlar
    // mappedBy: Review entity sindeki User referansının adı ("customer")
    // cascade: Kullanıcı silinince yorumları da silinsin. (ALL veya REMOVE)
    @OneToMany(mappedBy = "customer", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Review> reviews = new ArrayList<>();

    // Kullanıcıya gelen bildirimler
    // mappedBy: Notification entity sindeki User referansının adı ("user")
    // cascade: Kullanıcı silinince bildirimleri de silinsin. (ALL veya REMOVE)
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Notification> notifications = new ArrayList<>();

    // Kullanıcının istek listesi
    // mappedBy: WishlistItem entity sindeki User referansının adı ("user")
    // cascade: Kullanıcı silinince istek listesi de silinsin. (ALL veya REMOVE)
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<WishlistItem> wishlistItems = new ArrayList<>();

    // Satıcının oluşturduğu "Feel"ler (Seller ise)
    @OneToMany(mappedBy = "seller", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Feel> feels = new ArrayList<>();

    // Lombok @Data constructor, getter/setter vb. ekler. Manuel metoda gerek yok şimdilik.

    // --- HashCode ve Equals (Lombok @Data tarafından sağlanır ama dikkat) ---
    // İlişkiler (Listeler) üzerinden hashCode/equals hesaplaması performansı düşürebilir ve döngülere neden olabilir.
    // Eğer sorun yaşanırsa Lombok @EqualsAndHashCode(exclude = {"productsSold", "orders", "reviews", "notifications", "wishlistItems", "feels"}) kullanılabilir.
    // Veya sadece id üzerinden kontrol eden özel equals/hashCode yazılabilir.


@Column(name = "password_reset_token", nullable = true, length = 100) // Token uzunluğunu ayarla
private String passwordResetToken;

@Column(name = "password_reset_token_expiry", nullable = true)
private LocalDateTime passwordResetTokenExpiry;



}

// Açıklamalar:

// Paket:
// com.fibiyo.ecommerce.domain.entity olarak ayarlandı.
// Enum ların paketinin com.fibiyo.ecommerce.domain.enums olduğu varsayıldı.

/*
 * Importlar:
 * Gerekli JPA, Validation, Lombok ve Java Time importları eklendi.
 */

/*
 * JPA Annotation ları:
 * @Entity, @Table, @Id, @GeneratedValue, @Column (isim, null durumu, uzunluk, unique),
 * @Enumerated(EnumType.STRING) (Enum ların veritabanına metin olarak kaydedilmesi için) kullanıldı.
 */

/*
 * Validation Annotation ları:
 * @NotBlank, @Email, @Size, @NotNull gibi temel doğrulamalar eklendi.
 * Bu, DTO dan entity e dönüşüm sırasında veya JPA kaydetme öncesi ekstra bir güvenlik katmanı sağlar.
 */

/*
 * Lombok:
 * @Data, @NoArgsConstructor, @AllArgsConstructor ile kod kalabalığı azaltıldı.
 */

/*
 * Zaman Damgaları:
 * @CreationTimestamp ve @UpdateTimestamp ile otomatik tarih/saat yönetimi sağlandı.
 */

/*
 * İlişkiler:
 * Diğer entity lerle olan @OneToMany ilişkileri tanımlandı.
 */

/*
 * mappedBy:
 * İlişkinin diğer taraftaki entity de hangi alan ile kurulduğunu belirtir.
 */

/*
 * fetch = FetchType.LAZY:
 * Performans için genellikle en iyi seçenektir.
 * İlişkili entity ler sadece ihtiyaç duyulduğunda (get metodu çağrıldığında) veritabanından yüklenir.
 * EAGER genellikle önerilmez.
 */

/*
 * cascade:
 * Bir entity üzerinde yapılan işlemin (kaydetme, silme vb.) ilişkili entity leri nasıl etkileyeceğini belirler.
 * Dikkatli kullanılmalıdır.
 * Yorumlar, bildirimler, istek listesi gibi doğrudan kullanıcıya ait olanlar için
 * CascadeType.ALL, orphanRemoval = true mantıklıdır (kullanıcı silinirse bunlar da silinir).
 * Siparişler veya ürünler gibi iş mantığı gerektiren durumlar için
 * cascade kullanmak yerine servis katmanında bu işlemler yönetilmelidir.
 */

// List ler initialize edildi (= new ArrayList<>()) - bu NullPointerException hatalarını önlemek için önemlidir.

