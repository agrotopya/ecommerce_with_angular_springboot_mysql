package com.fibiyo.ecommerce.domain.entity;

import com.fibiyo.ecommerce.domain.enums.NotificationType; // NotificationType enum'ını import et
import jakarta.persistence.*; // JPA importları
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString; // ToString importunu ekledik
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications", indexes = {
        // Kullanıcı bazında ve okunma durumuna göre sorgulama hızlanabilir
        @Index(name = "idx_notifications_user_read", columnList = "user_id, is_read")
})
@Data // Lombok: Getter, Setter, equals, hashCode, toString
@NoArgsConstructor // Lombok: Boş constructor
@AllArgsConstructor // Lombok: Tüm alanları içeren constructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Bildirim mesajı boş olamaz")
    @Lob // Mesaj uzun olabilir
    @Column(nullable = false, columnDefinition = "TEXT")
    private String message; // Bildirim içeriği

    @Size(max = 1024, message = "Link en fazla 1024 karakter olabilir")
    @Column(length = 1024, nullable = true)
    private String link; // Bildirimle ilgili bir link (örn: /orders/123)

    @NotNull
    @Column(name = "is_read", nullable = false)
    private boolean isRead = false; // Varsayılan olarak okunmamış

    @CreationTimestamp // Bildirimin oluşturulma zamanı
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(length = 50, nullable = true) // Null olabilir veya 'GENERIC' gibi bir default olabilir
    private NotificationType type; // Bildirim türü (Filtreleme/gruplama için)


    // =============================================
    // İLİŞKİLER (Relationships)
    // =============================================

    // Bildirimin gönderildiği kullanıcı
    @NotNull // Her bildirim bir kullanıcıya ait olmalı
    @ManyToOne(fetch = FetchType.LAZY) // Kullanıcıyı direkt yüklemeye gerek yok
    @JoinColumn(name = "user_id", nullable = false) // FK: user_id
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User user;


    // --- HashCode ve Equals ---
    // Lombok @Data yeterli olmalı, ilişki LAZY. Sorun olursa @EqualsAndHashCode(exclude = {"user"}) kullanılabilir.
}

// Açıklamalar:

/*
 * @Lob:
 * message alanı uzun olabileceği için veritabanında TEXT gibi uygun bir tipe maplenmesine yardımcı olur.
 */

/*
 * link:
 * Kullanıcıyı bildirime tıklayınca ilgili sayfaya (sipariş, ürün vb.) yönlendirmek için kullanılır.
 */

/*
 * isRead:
 * Kullanıcının bildirimi okuyup okumadığını belirtir.
 * Bu alan, okunmamış bildirim sayısını göstermek için önemlidir.
 */

/*
 * type:
 * Bildirimleri kategorize etmek (örn: sadece sipariş güncellemelerini göster) ve
 * belki ikonları ayırt etmek için kullanılır.
 * NotificationType enum'ı ile tanımlandı.
 */

/*
 * Index:
 * user_id ve is_read alanları üzerine bir index ekledik.
 * Bu, "kullanıcının okunmamış bildirimlerini getir" gibi sorguları hızlandırabilir.
 */

/*
 * İlişki:
 * User ile zorunlu @ManyToOne ilişkisi kuruldu (FetchType.LAZY).
 */

/*
 * Cascade:
 * Bildirim silindiğinde kullanıcının silinmesi istenmez.
 * Kullanıcı silindiğinde bildirimlerinin silinmesi,
 * User entity'sindeki @OneToMany ilişkisi ile yönetiliyor.
 */
