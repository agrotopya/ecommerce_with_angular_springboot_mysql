package com.fibiyo.ecommerce.domain.entity;

import jakarta.persistence.*; // JPA importları
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString; // ToString importunu ekledik
import org.hibernate.annotations.Formula;    // Hesaplanan alan için (Opsiyonel, şemaya göre karar ver)

import java.math.BigDecimal;

@Entity
@Table(name = "order_items", indexes = {
        // Sipariş ID'si ve Ürün ID'sine göre sorgular yaygın olabilir
        @Index(name = "idx_order_items_order_id", columnList = "order_id"),
        @Index(name = "idx_order_items_product_id", columnList = "product_id")
})
@Data // Lombok: Getter, Setter, equals, hashCode, toString
@NoArgsConstructor // Lombok: Boş constructor
@AllArgsConstructor // Lombok: Tüm alanları içeren constructor
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Miktar boş olamaz")
    @Min(value = 1, message = "Miktar en az 1 olmalıdır")
    @Column(nullable = false)
    private Integer quantity; // Bu üründen kaç adet alındığı

    @NotNull(message = "Satın alma fiyatı boş olamaz")
    @DecimalMin(value = "0.00", message = "Satın alma fiyatı negatif olamaz")
    @Column(name = "price_at_purchase", nullable = false, precision = 12, scale = 2)
    private BigDecimal priceAtPurchase; // Ürünün SİPARİŞ VERİLDİĞİ ANDAKİ birim fiyatı

    // Veritabanındaki GENERATED ALWAYS AS sütununa karşılık gelir.
    // @Formula kullanımı alternatif olabilir, ama GENERATED daha veritabanı odaklı.
    // JPA'nın bunu yönetmesi için insertable/updatable = false kullanılır.
    @Column(name = "item_total", nullable = false, precision = 14, scale = 2) 
    private BigDecimal itemTotal; // Kalemin toplam tutarı (miktar * fiyat)

    // =============================================
    // İLİŞKİLER (Relationships)
    // =============================================

    // Bu kalemin ait olduğu sipariş
    @NotNull // Her kalem bir siparişe ait olmalı
    @ManyToOne(fetch = FetchType.LAZY) // Siparişi her zaman yükleme
    @JoinColumn(name = "order_id", nullable = false) // FK: order_id
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Order order;

    // Bu kalemin referans verdiği ürün
    // @NotNull KULLANILMAMALI! Şemada ON DELETE SET NULL dedik, yani ürün silinirse burası null olabilir.
    @ManyToOne(fetch = FetchType.LAZY) // Ürünü her zaman yükleme
    @JoinColumn(name = "product_id", nullable = true) // FK: product_id (NULL olabilir)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Product product;

    @PrePersist
@PreUpdate
private void calculateItemTotal() {
    if (priceAtPurchase != null && quantity != null) {
        this.itemTotal = priceAtPurchase.multiply(BigDecimal.valueOf(quantity));
    }
}

    // --- HashCode ve Equals ---
    // Lombok @Data kullanıldı. İlişkiler exclude edildiği için genellikle sorun olmaz.
    // Gerekirse ID bazlı manuel implementasyon yapılabilir.
}

// Açıklamalar:

/*
 * priceAtPurchase:
 * Bu çok önemli bir alandır.
 * Ürünün fiyatı zamanla değişebilir, ancak siparişte, siparişin verildiği andaki fiyatın kaydedilmesi gerekir.
 */

/*
 * itemTotal:
 * Şemamızda bu alan GENERATED ALWAYS AS (quantity * price_at_purchase) STORED olarak tanımlanmıştı.
 * Veritabanı bu hesaplamayı otomatik yapar.
 * JPA'nın bu sütuna yazmaya çalışmasını engellemek için
 * @Column annotation'ında insertable = false ve updatable = false özelliklerini kullandık.
 * Alternatif olarak, şemada generated olmasaydı ve sadece okuma amaçlı JPA'da hesaplansın isteseydik
 * @Formula("(quantity * price_at_purchase)") kullanabilirdik,
 * ama veritabanında olması daha tutarlıdır.
 */

/*
 * İlişkiler:
 * Order ile @ManyToOne ilişkisi kuruldu ve @NotNull ile zorunlu kılındı.
 * Product ile @ManyToOne ilişkisi kuruldu ancak @NotNull kullanılmadı.
 * Çünkü veritabanı şemasında products tablosundan bir ürün silinirse
 * order_items.product_id sütununun NULL olarak ayarlanmasını (ON DELETE SET NULL) belirttik.
 * Bu, geçmiş sipariş kayıtlarında ürün bilgisi kaybolsa bile siparişin bütünlüğünü korur.
 * Eğer ürün bilgisini (name, sku vb.) OrderItem içinde de saklamak istersen (denormalizasyon),
 * bu ilişkiyi kaldırıp ilgili alanları direkt ekleyebilirsin,
 * ancak bu genellikle tavsiye edilmez.
 * Ürün silindiğinde ne olacağı iş kuralıdır ve servis katmanında yönetilebilir
 * (örn: Siparişte "Silinmiş Ürün" yazdırılabilir).
 */

/*
 * FetchType ve Exclude:
 * Standart olarak FetchType.LAZY ve Exclude kullanıldı.
 */
