package com.fibiyo.ecommerce.domain.entity;


import com.fibiyo.ecommerce.domain.enums.DiscountType; // DiscountType enum'ını import et
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
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "coupons", uniqueConstraints = {
        @UniqueConstraint(columnNames = "code", name = "uk_coupon_code") // Kupon kodu unique olmalı
})
@Data // Lombok: Getter, Setter, equals, hashCode, toString
@NoArgsConstructor // Lombok: Boş constructor
@AllArgsConstructor // Lombok: Tüm alanları içeren constructor
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Kupon kodu boş olamaz")
    @Size(max = 50, message = "Kupon kodu en fazla 50 karakter olabilir")
    @Column(nullable = false, unique = true, length = 50)
    private String code; // Kupon kodu (örn: BAHAR2024)

    @Column(columnDefinition = "TEXT", nullable = true)
    private String description; // Kupon açıklaması (admin veya kullanıcı için)

    @NotNull(message = "İndirim tipi boş olamaz")
    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 20)
    private DiscountType discountType; // Yüzdesel mi, sabit tutar mı?

    @NotNull(message = "İndirim değeri boş olamaz")
    @DecimalMin(value = "0.01", message = "İndirim değeri pozitif olmalıdır")
    @Digits(integer = 8, fraction = 2) // Yüksek indirimlere izin verelim
    @Column(name = "discount_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountValue; // İndirim miktarı veya yüzdesi

    @NotNull(message = "Geçerlilik bitiş tarihi boş olamaz")
    @Future(message = "Geçerlilik bitiş tarihi geçmişte olamaz") // Geçerlilik gelecekte olmalı
    @Column(name = "expiry_date", nullable = false)
    private LocalDateTime expiryDate; // Kuponun son kullanma tarihi

    @NotNull(message = "Minimum alışveriş tutarı boş olamaz")
    @DecimalMin(value = "0.00", message = "Minimum alışveriş tutarı negatif olamaz")
    @Column(name = "min_purchase_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal minPurchaseAmount = BigDecimal.ZERO; // Kuponun geçerli olması için min sepet tutarı

    @NotNull
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true; // Kupon aktif mi?

    @Min(value = 0, message = "Kullanım limiti negatif olamaz")
    @Column(name = "usage_limit", nullable = true) // NULL ise sınırsız kullanım
    private Integer usageLimit;

    @NotNull
    @Min(value = 0)
    @Column(name = "times_used", nullable = false)
    private int timesUsed = 0; // Kuponun kaç kere kullanıldığı

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // =============================================
    // İLİŞKİLER (Relationships)
    // =============================================

    // Bu kuponun kullanıldığı siparişler
    // mappedBy: Order entity'sindeki Coupon referansının adı ("coupon")
    // cascade: Kupon silinirse siparişlerdeki referanslar null olur (schema.sql'e göre SET NULL)
    @OneToMany(mappedBy = "coupon", fetch = FetchType.LAZY)
    @ToString.Exclude // Çok fazla sipariş olabilir, toString'e ekleme
    @EqualsAndHashCode.Exclude // Çok fazla sipariş olabilir, equals/hashCode'a ekleme
    private List<Order> orders = new ArrayList<>();


    // --- Yardımcı Metotlar (Opsiyonel) ---

    /**
     * Kuponun kullanım süresinin dolup dolmadığını kontrol eder.
     * @return True eğer süre dolmuşsa, false aksi takdirde.
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiryDate);
    }

    /**
     * Kuponun kullanım limitine ulaşıp ulaşmadığını kontrol eder.
     * @return True eğer limit dolmuşsa, false aksi takdirde.
     */
    public boolean isUsageLimitReached() {
        return this.usageLimit != null && this.timesUsed >= this.usageLimit;
    }

    /**
     * Kuponun genel olarak geçerli olup olmadığını kontrol eder (aktif, süresi dolmamış, limit dolmamış).
     * @return True eğer kupon geçerliyse.
     */
    public boolean isValid() {
        return this.isActive && !isExpired() && !isUsageLimitReached();
    }

    /**
     * Kuponun kullanım sayısını bir artırır.
     */
    public void incrementTimesUsed() {
        this.timesUsed++;
    }
}// Açıklamalar:

/*
 * Annotation'lar:
 * JPA, Validation ve Lombok annotation'ları uygun şekilde kullanıldı.
 */

/*
 * Alanlar:
 * Kupon kodu, tipi, değeri, son kullanma tarihi, minimum alışveriş tutarı,
 * aktiflik durumu, kullanım limiti ve kaç kez kullanıldığı gibi alanlar eklendi.
 */

/*
 * Validation:
 * @NotBlank, @Size, @NotNull, @Enumerated, @DecimalMin, @Digits, @Future, @Min gibi doğrulamalar
 * kuponun mantıksal olarak doğru oluşturulmasını sağlamak için eklendi.
 */

/*
 * İlişki:
 * Kuponun kullanıldığı siparişleri takip etmek için Order ile @OneToMany ilişkisi tanımlandı.
 * Yine Exclude annotation'ları döngüleri engellemek için kullanıldı.
 */

/*
 * Yardımcı Metotlar:
 * isExpired(), isUsageLimitReached(), isValid() gibi metodlar kuponun geçerliliğini kontrol etmeyi kolaylaştırır.
 * Bu kontroller servis katmanında da yapılabilir, ancak entity içinde olması da pratik olabilir.
 * incrementTimesUsed() metodu kullanım sayısını artırır.
 */
