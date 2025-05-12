package com.fibiyo.ecommerce.domain.entity;

import jakarta.persistence.*; // JPA importları
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString; // ToString importunu ekledik
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "wishlist_items", uniqueConstraints = {
        // Bir kullanıcı bir ürünü istek listesine sadece 1 kez ekleyebilmeli
        @UniqueConstraint(columnNames = {"user_id", "product_id"}, name = "uk_wishlist_user_product")
})
@Data // Lombok: Getter, Setter, equals, hashCode, toString
@NoArgsConstructor // Lombok: Boş constructor
@AllArgsConstructor // Lombok: Tüm alanları içeren constructor
public class WishlistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreationTimestamp // Ürünün istek listesine eklenme zamanı
    @Column(name = "added_at", nullable = false, updatable = false)
    private LocalDateTime addedAt;

    // =============================================
    // İLİŞKİLER (Relationships)
    // =============================================

    // İstek listesi kaleminin ait olduğu kullanıcı
    @NotNull // Her kayıt bir kullanıcıya ait olmalı
    @ManyToOne(fetch = FetchType.LAZY) // Kullanıcıyı direkt yüklemeye gerek yok
    @JoinColumn(name = "user_id", nullable = false) // FK: user_id
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User user;

    // İstek listesine eklenen ürün
    @NotNull // Her kayıt bir ürüne ait olmalı
    @ManyToOne(fetch = FetchType.LAZY) // Ürünü direkt yüklemeye gerek yok
    @JoinColumn(name = "product_id", nullable = false) // FK: product_id
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Product product;

    // --- HashCode ve Equals ---
    // Lombok @Data yeterli olmalı, ilişkiler LAZY olduğu için sorun çıkma ihtimali düşük.
    // Sorun olursa @EqualsAndHashCode(exclude = {"user", "product"}) kullanılabilir.
}
// Açıklamalar:

/*
 * Basit Yapı:
 * Bu entity'nin amacı sadece kullanıcı ile ürün arasındaki "istek listesi" ilişkisini kurmak olduğu için
 * yapısı oldukça basit. Ekstra alanlara (miktar, not vb.) genellikle gerek duyulmaz.
 */

/*
 * Unique Constraint:
 * Veritabanı seviyesinde (user_id, product_id) çiftinin unique olması sağlandı.
 * Bu, aynı ürünün aynı kullanıcı tarafından listeye tekrar eklenmesini engeller.
 */

/*
 * addedAt:
 * Ürünün ne zaman eklendiğini takip etmek için @CreationTimestamp kullanıldı.
 */

/*
 * İlişkiler:
 * User ve Product ile zorunlu (@NotNull) @ManyToOne ilişkileri kuruldu.
 * Performans ve döngüleri önlemek için yine FetchType.LAZY ve Exclude annotation'ları tercih edildi.
 */

/*
 * Cascade:
 * Genellikle WishlistItem silindiğinde User veya Product'ın silinmesi istenmez,
 * bu yüzden cascade ayarı eklenmedi.
 * Tersi (User veya Product silinince WishlistItem'ın silinmesi),
 * ilişkilerin diğer ucundaki (User ve Product entity'lerindeki @OneToMany ilişkilerindeki)
 * cascade = CascadeType.ALL, orphanRemoval = true ayarlarıyla yönetilir.
 */
