package com.fibiyo.ecommerce.domain.entity;

import jakarta.persistence.*; // JPA importları
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString; // ToString importunu ekledik
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "reviews", uniqueConstraints = {
        // Bir kullanıcı bir ürüne sadece 1 yorum yapabilmeli
        @UniqueConstraint(columnNames = {"product_id", "customer_id"}, name = "uk_review_product_customer")
})
@Data // Lombok: Getter, Setter, equals, hashCode, toString
@NoArgsConstructor // Lombok: Boş constructor
@AllArgsConstructor // Lombok: Tüm alanları içeren constructor
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Puan boş olamaz")
    @Min(value = 1, message = "Puan en az 1 olmalıdır")
    @Max(value = 5, message = "Puan en fazla 5 olabilir")
    @Column(nullable = false) // TINYINT (1 byte) genellikle yeterlidir
    private Byte rating; // 1-5 arası puan

    @Lob // Yorum metni uzun olabilir, TEXT tipine uygun
    @Column(columnDefinition = "TEXT", nullable = true)
    private String comment; // Müşterinin yorumu

    @CreationTimestamp // Yorumun oluşturulma zamanı
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @NotNull // Yorum onayının null olmaması iyi bir pratik
    @Column(name = "is_approved", nullable = false)
    private boolean isApproved = true; // Varsayılan olarak onaylı (Proje gereksinimine göre false olabilir)

    // =============================================
    // İLİŞKİLER (Relationships)
    // =============================================

    // Yorumun yapıldığı ürün
    @NotNull // Her yorum bir ürüne ait olmalı
    @ManyToOne(fetch = FetchType.LAZY) // Ürünü her zaman yüklemeye gerek yok
    @JoinColumn(name = "product_id", nullable = false) // FK: product_id
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Product product;

    // Yorumu yapan müşteri (User)
    @NotNull // Her yorum bir kullanıcıya ait olmalı
    @ManyToOne(fetch = FetchType.LAZY) // Müşteriyi her zaman yüklemeye gerek yok
    @JoinColumn(name = "customer_id", nullable = false) // FK: customer_id
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User customer;

    // Opsiyonel: Yorumun hangi sipariş sonrası yapıldığını belirtmek için
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = true) // Null olabilir, her yorum bir siparişe bağlı olmayabilir
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Order order; // Sipariş referansı


    // --- HashCode ve Equals ---
    // Lombok @Data yeterli olmalı, ilişkiler LAZY olduğu için sorun çıkma ihtimali düşük.
    // Sorun olursa @EqualsAndHashCode(exclude = {"product", "customer", "order"}) kullanılabilir.
}

