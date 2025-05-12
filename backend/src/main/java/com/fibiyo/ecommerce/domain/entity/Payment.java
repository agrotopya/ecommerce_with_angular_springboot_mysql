package com.fibiyo.ecommerce.domain.entity;

import com.fibiyo.ecommerce.domain.enums.PaymentStatus; // PaymentStatus enum'ını import et
import jakarta.persistence.*; // JPA importları
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString; // ToString importunu ekledik
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments", uniqueConstraints = {
        // Ödeme ağ geçidinin işlem ID'si unique olmalı (null değilse)
        @UniqueConstraint(columnNames = "transaction_id", name = "uk_payment_transaction_id")
}, indexes = {
        // Sipariş ID ve duruma göre sorgular olabilir
        @Index(name = "idx_payments_order_id", columnList = "order_id"),
        @Index(name = "idx_payments_status", columnList = "status")
})
@Data // Lombok: Getter, Setter, equals, hashCode, toString
@NoArgsConstructor // Lombok: Boş constructor
@AllArgsConstructor // Lombok: Tüm alanları içeren constructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Ödeme tutarı boş olamaz")
    @DecimalMin(value = "0.01", message = "Ödeme tutarı pozitif olmalıdır")
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount; // Ödenen veya iade edilen tutar

    @CreationTimestamp // Ödemenin veritabanına kaydedilme/başlama zamanı
    @Column(name = "payment_date", nullable = false, updatable = false)
    private LocalDateTime paymentDate;

    @NotBlank(message = "Ödeme yöntemi boş olamaz")
    @Size(max = 50, message = "Ödeme yöntemi en fazla 50 karakter olabilir")
    @Column(name = "payment_method", nullable = false, length = 50)
    private String paymentMethod; // Örn: STRIPE, PAYPAL_SANDBOX, BANK_TRANSFER

    @Size(max = 255, message = "Transaction ID en fazla 255 karakter olabilir")
    @Column(name = "transaction_id", unique = true, length = 255, nullable = true) // Başarılı ödemelerde gateway'den gelir, yoksa null olabilir
    private String transactionId; // Ödeme ağ geçidi işlem ID'si

    @NotNull(message = "Ödeme durumu boş olamaz")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status = PaymentStatus.PENDING; // Varsayılan durum

    @NotNull(message = "Para birimi boş olamaz")
    @Size(min = 3, max = 3, message = "Para birimi 3 karakter olmalıdır (örn: TRY)")
    @Column(nullable = false, length = 3)
    private String currency = "TRY"; // Varsayılan para birimi

    @Lob // Gateway yanıtı uzun olabilir
    @Column(name = "gateway_response", nullable = true, columnDefinition = "TEXT")
    private String gatewayResponse; // Debug veya referans için ağ geçidi yanıtı

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;


    // =============================================
    // İLİŞKİLER (Relationships)
    // =============================================

    // Bu ödemenin ait olduğu sipariş
    @NotNull // Her ödeme bir siparişe bağlı olmalı
    @ManyToOne(fetch = FetchType.LAZY) // Siparişi her zaman yükleme
    @JoinColumn(name = "order_id", nullable = false) // FK: order_id
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Order order;


    // --- HashCode ve Equals ---
    // Lombok @Data kullanıldı. İlişki exclude edildiği için genellikle sorun olmaz.
    // Gerekirse ID bazlı manuel implementasyon yapılabilir.
}// Açıklamalar:

/*
 * transactionId:
 * Ödeme ağ geçitlerinden (Stripe, PayPal vb.) dönen benzersiz işlem kimliğini saklamak için kullanılır.
 * Başarısız ödemelerde veya ödeme işlemi başlamadan önce null olabilir,
 * bu yüzden nullable = true ve unique = true olarak ayarlandı.
 */

/*
 * paymentMethod:
 * Hangi yöntemle ödeme yapıldığını belirtir (Stripe, PayPal, Kredi Kartı, Banka Havalesi vb.).
 */

/*
 * currency:
 * İşlemin yapıldığı para birimi (örn: "TRY", "USD", "EUR").
 * Varsayılan olarak "TRY" ayarlandı.
 */

/*
 * gatewayResponse:
 * Ödeme ağ geçidinden gelen detaylı yanıtı saklamak için kullanılır.
 * Hata ayıklama (debugging) veya ileride referans amaçlı faydalı olabilir.
 * @Lob ile uzun metinlere uygun hale getirildi.
 */

/*
 * İlişki:
 * Order ile zorunlu (@NotNull) @ManyToOne ilişkisi kuruldu.
 */

/*
 * Cascade:
 * Order silindiğinde ilişkili Payment kayıtlarının da silinmesi genellikle istenen bir davranıştır.
 * Bu, Order entity'sindeki @OneToMany ilişkisindeki
 * cascade = CascadeType.ALL, orphanRemoval = true ayarı ile yönetilir.
 * Bu tarafta cascade ayarı yapmaya gerek yoktur.
 */
