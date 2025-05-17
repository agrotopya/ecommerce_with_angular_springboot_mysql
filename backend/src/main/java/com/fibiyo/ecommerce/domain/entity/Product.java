package com.fibiyo.ecommerce.domain.entity;

import jakarta.persistence.*; // JPA importları
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter; // Eklendi
import lombok.NoArgsConstructor;
import lombok.Setter; // Eklendi
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLDelete; // Eklendi
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.Where; // Eklendi

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList; // List implementasyonu için
import java.util.List; // İlişkiler için

@Entity
@Table(name = "products", uniqueConstraints = {
        @UniqueConstraint(columnNames = "slug", name = "uk_product_slug"), // slug unique olmalı
        @UniqueConstraint(columnNames = "sku", name = "uk_product_sku") // SKU unique olmalı (kullanılıyorsa)
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE products SET is_deleted = true, updated_at = CURRENT_TIMESTAMP WHERE id = ? AND version = ?") // Soft delete için SQL
@Where(clause = "is_deleted = false") // Sadece silinmemiş ürünleri getir
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Ürün adı boş olamaz")
    @Size(max = 255, message = "Ürün adı en fazla 255 karakter olabilir")
    @Column(nullable = false)
    private String name;

    @NotBlank(message = "Ürün slug boş olamaz")
    @Size(max = 255, message = "Ürün slug en fazla 255 karakter olabilir")
    @Column(nullable = false, unique = true)
    private String slug; // SEO uyumlu URL parçası

    @Column(columnDefinition = "TEXT", nullable = true)
    private String description;

    @NotNull(message = "Fiyat boş olamaz")
    @DecimalMin(value = "0.01", message = "Fiyat en az 0.01 olmalıdır") // 0 dan büyük olmalı
    @Digits(integer = 10, fraction = 2, message = "Fiyat formatı geçersiz (örn: 1234.56)") // Veritabanı ile uyumlu scale/precision
    @Column(nullable = false, precision = 12, scale = 2) // precision: toplam basamak, scale: ondalık basamak
    private BigDecimal price;

    @NotNull(message = "Stok miktarı boş olamaz")
    @Min(value = 0, message = "Stok miktarı negatif olamaz")
    @Column(nullable = false)
    private Integer stock = 0; // Varsayılan stok


    @Version
    @Column(nullable = false)
    private Long version= 0L; // JPA versiyon kontrolü için (optimistic locking)

    @Size(max = 100)
    @Column(length = 100, unique = true, nullable = true)
    private String sku; // Stock Keeping Unit (Opsiyonel)

    @Size(max = 1024, message = "Ana görsel URL si en fazla 1024 karakter olabilir")
    @Column(name = "main_image_url", length = 1024, nullable = true) // image_url -> main_image_url
    private String mainImageUrl; 

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted = false; // Soft delete için alan

    // --- Yönetim ve Durum ---
    @NotNull
    @Column(name = "is_approved", nullable = false)
    private boolean isApproved = false; // Admin onayı varsayılan olarak false

    @NotNull
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true; // Satıcı eklediğinde varsayılan olarak aktif (ama onaylanmamış olabilir)

    @NotNull
    @DecimalMin(value = "0.0")
    @DecimalMax(value = "5.0")
    @Digits(integer = 1, fraction = 2) // Örn: 4.75 gibi bir ortalama için
    @Column(name = "average_rating", nullable = false)
    private BigDecimal averageRating = BigDecimal.ZERO; // Varsayılan ortalama puan

    @NotNull
    @Min(value = 0)
    @Column(name = "review_count", nullable = false)
    private int reviewCount = 0; // Varsayılan yorum sayısı

    @NotNull
    @Min(value = 0)
    @NotNull
    @Min(value = 0)
    @Column(name = "sales_count", nullable = false)
    private int salesCount = 0; // Toplam satılan miktar

    // --- AI Özellikleri ---
    @Lob // Büyük metinler için (CLOB veya TEXT)
    @Column(name = "review_summary_ai", nullable = true)
    private String reviewSummaryAi;

    @Size(max = 1024, message = "AI Görsel URL si en fazla 1024 karakter olabilir")
    @Column(name = "ai_generated_image_url", length = 1024, nullable = true)
    private String aiGeneratedImageUrl;

    // =============================================
    // İLİŞKİLER (Relationships)
    // =============================================

    // Ürünün ait olduğu kategori
    @ManyToOne(fetch = FetchType.LAZY) // Kategoriyi her zaman yüklemeye gerek yok
    @JoinColumn(name = "category_id", nullable = true) // FK: category_id
    @ToString.Exclude // toString döngüsünü engelle
    @EqualsAndHashCode.Exclude // equals/hashCode döngüsünü engelle
    private Category category;

    // Ürünü satan satıcı (User)
    @NotNull // Her ürünün bir satıcısı olmalı
    @ManyToOne(fetch = FetchType.LAZY) // Satıcıyı her zaman yüklemeye gerek yok
    @JoinColumn(name = "seller_id", nullable = false) // FK: seller_id
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User seller;

    // Bu ürünün geçtiği sipariş kalemleri
    // mappedBy: OrderItem entity sindeki Product referansının adı ("product")
    // cascade: Ürün silinince sipariş kalemleri silinmemeli (kayıt tutulmalı). Şemadaki SET NULL bunu sağlar.
    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<OrderItem> orderItems = new ArrayList<>();

    // Bu ürüne yapılan yorumlar
    // mappedBy: Review entity sindeki Product referansının adı ("product")
    // cascade: Ürün silinince yorumları da silinsin (CascadeType.ALL veya REMOVE).
    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Review> reviews = new ArrayList<>();

    // Bu ürünün bulunduğu istek listeleri
    // mappedBy: WishlistItem entity sindeki Product referansının adı ("product")
    // cascade: Ürün silinince istek listesi kayıtları da silinsin (CascadeType.ALL veya REMOVE).
    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<WishlistItem> wishlistItems = new ArrayList<>();

    // Bu ürüne ait "Feel"ler
    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Feel> feels = new ArrayList<>();

    // Ürüne ait diğer resimler
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<ProductImage> productImages = new ArrayList<>();


    // --- Yardımcı Metotlar (Opsiyonel ama faydalı olabilir) ---
    public void incrementReviewCount() {
        this.reviewCount++;
    }

    public void decrementReviewCount() {
        if (this.reviewCount > 0) {
            this.reviewCount--;
        }
    }

    // Ortalama rating güncellemek için bir metod da eklenebilir (Servis katmanında da yapılabilir)
    // public void updateAverageRating(BigDecimal newRatingSum, int totalReviews) { ... }

    public String getAiReviewSummary() {
        return this.reviewSummaryAi;
    }

    public void setAiReviewSummary(String aiReviewSummary) {
        this.reviewSummaryAi = aiReviewSummary;
    }

    // --- HashCode ve Equals ---
    // Yine, ilişkiler nedeniyle dikkatli olunmalı. Lombok @Data nın ürettikleri yeterli olabilir,
    // ancak sorun olursa @EqualsAndHashCode(exclude = {iliski alanları}) kullanılmalı.
}

// Açıklamalar:

// Importlar, Paket:
// Doğru şekilde ayarlandı.

/*
 * Annotation lar:
 * @Entity, @Table, @Id, @Column vb. şemaya uygun olarak kullanıldı.
 */

/*
 * Validation:
 * @NotBlank, @Size, @NotNull, @DecimalMin, @Min, @Digits gibi doğrulamalar eklendi.
 * Özellikle BigDecimal için @Digits kullanarak veritabanı DECIMAL(12, 2) tanımıyla uyumlu olmasını sağladık.
 */

/*
 * Slug ve SKU:
 * Ürünler için de unique slug ve opsiyonel unique sku alanları eklendi.
 */

/*
 * averageRating, reviewCount:
 * Bu alanlar, her yeni yorum eklendiğinde/silindiğinde güncellenebilir.
 * Başlangıçta servis katmanında güncelleyebiliriz, ileride performans için trigger
 * veya başka mekanizmalar düşünülebilir.
 * Varsayılan değerleri eklendi.
 */

/*
 * AI Alanları:
 * reviewSummaryAi (TEXT olacağı için @Lob eklenebilir) ve aiGeneratedImageUrl eklendi.
 */

/*
 * İlişkiler:
 * Category ve User (seller) ile @ManyToOne,
 * OrderItem, Review ve WishlistItem ile @OneToMany ilişkileri tanımlandı.
 * FetchType.LAZY kullanıldı ve döngüleri önlemek için @ToString.Exclude, @EqualsAndHashCode.Exclude eklendi.
 */

/*
 * Cascade:
 * Yorumlar ve istek listesi gibi doğrudan ürüne bağlı ve ürün silindiğinde anlamını yitirecek ilişkilerde
 * CascadeType.ALL, orphanRemoval = true kullanıldı.
 * Sipariş kalemleri için cascade eklenmedi (ürün silinse de sipariş kaydı önemlidir).
 */
