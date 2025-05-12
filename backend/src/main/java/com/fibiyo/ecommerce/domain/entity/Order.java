package com.fibiyo.ecommerce.domain.entity;

import com.fibiyo.ecommerce.domain.enums.OrderStatus; // Enum import
import com.fibiyo.ecommerce.domain.enums.PaymentStatus; // Enum import
import jakarta.persistence.*; // JPA importları
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString; // ToString importunu ekledik
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.JdbcTypeCode; // JSON tipi için
import org.hibernate.type.SqlTypes;           // JSON tipi için
// import org.hibernate.annotations.Formula;    // Hesaplanan alan için - Artık kullanılmıyor
import org.hibernate.annotations.Generated;  // Eklendi
import org.hibernate.annotations.GenerationTime; // Eklendi

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList; // List implementasyonu için
import java.util.List; // İlişkiler için

@Entity
@Table(name = "orders", indexes = {
        @Index(name = "idx_orders_customer_id", columnList = "customer_id"),
        @Index(name = "idx_orders_status", columnList = "status"),
        @Index(name = "idx_orders_payment_status", columnList = "payment_status"),
        @Index(name = "idx_orders_order_date", columnList = "order_date")
})
@Data // Lombok: Getter, Setter, equals, hashCode, toString
@NoArgsConstructor // Lombok: Boş constructor
@AllArgsConstructor // Lombok: Tüm alanları içeren constructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreationTimestamp // Siparişin teknik olarak oluşturulma zamanı
    @Column(name = "order_date", nullable = false, updatable = false)
    private LocalDateTime orderDate;

    @NotNull(message = "Sipariş durumu boş olamaz")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30) // Enum string uzunluğuna uygun
    private OrderStatus status = OrderStatus.PENDING_PAYMENT; // Varsayılan durum

    @NotNull(message = "Toplam tutar boş olamaz")
    @DecimalMin(value = "0.00", message = "Toplam tutar negatif olamaz")
    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount; // İndirimsiz, vergisiz ürün toplamı

    @NotNull
    @DecimalMin(value = "0.00")
    @Column(name = "discount_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO; // Uygulanan indirim

    @NotNull
    @DecimalMin(value = "0.00")
    @Column(name = "shipping_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal shippingFee = BigDecimal.ZERO; // Kargo ücreti

    // finalAmount artık servis katmanında hesaplanıp set edilecek.
    // @Generated ve insertable/updatable özellikleri kaldırıldı.
    @NotNull(message = "Nihai tutar boş olamaz") // finalAmount artık uygulama tarafından set edileceği için NotNull olabilir.
    @Column(name = "final_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal finalAmount; // Ödenecek/Ödenen Son Tutar (servis tarafından hesaplanır)

    @NotNull(message = "Teslimat adresi boş olamaz")
    @JdbcTypeCode(SqlTypes.JSON) // Hibernate 6+ ile JSON tipini doğrudan yönet
    //@Column(columnDefinition = "json", nullable = false) // Eski yöntem veya String olarak saklamak istersen
    private String shippingAddress; // Adres bilgisini JSON String olarak sakla veya özel bir tipe map'le

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "billing_address", nullable = true)
    private String billingAddress; // Fatura adresi (JSON String)

    @Size(max = 50, message = "Ödeme yöntemi en fazla 50 karakter olabilir")
    @Column(name = "payment_method", length = 50, nullable = true) // Ödeme tamamlanınca dolar
    private String paymentMethod;

    @NotNull(message = "Ödeme durumu boş olamaz")
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING; // Varsayılan ödeme durumu

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt; // Kaydın teknik oluşturulma zamanı

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt; // Kaydın son güncellenme zamanı

    @Size(max = 100)
    @Column(name = "tracking_number", length = 100, nullable = true)
    private String trackingNumber; // Kargo takip numarası

    // =============================================
    // İLİŞKİLER (Relationships)
    // =============================================

    // Siparişi veren müşteri (User)
    @NotNull // Siparişin bir sahibi olmalı
    @ManyToOne(fetch = FetchType.LAZY) // Kullanıcıyı hemen yükleme
    @JoinColumn(name = "customer_id", nullable = false) // FK: customer_id
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User customer;

    // Kullanılan kupon (opsiyonel)
    @ManyToOne(fetch = FetchType.LAZY) // Kuponu hemen yükleme
    @JoinColumn(name = "coupon_id", nullable = true) // FK: coupon_id (null olabilir)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Coupon coupon;

    // Siparişteki ürün kalemleri (Order Items)
    // mappedBy: OrderItem entity'sindeki Order referansının adı ("order")
    // cascade: Sipariş kaydedildiğinde kalemleri de kaydet/güncelle, sipariş silinince kalemleri de sil.
    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<OrderItem> orderItems = new ArrayList<>();

    // Siparişe ait ödemeler (Bir sipariş için birden fazla ödeme olabilir mi? Örn: Başarısız deneme sonrası başarılı)
    // mappedBy: Payment entity'sindeki Order referansının adı ("order")
    // cascade: Sipariş silinince ödeme kayıtları da silinsin (Genellikle evet)
    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Payment> payments = new ArrayList<>();


    // --- Yardımcı Metotlar (Opsiyonel) ---

    /**
     * Siparişe bir ürün kalemi ekler ve çift yönlü ilişkiyi kurar.
     * @param item Eklenecek OrderItem.
     */
    public void addOrderItem(OrderItem item) {
        if (this.orderItems == null) {
            this.orderItems = new ArrayList<>();
        }
        this.orderItems.add(item);
        item.setOrder(this); // Çift yönlü ilişkiyi ayarla
    }

    /**
     * Siparişten bir ürün kalemini kaldırır ve çift yönlü ilişkiyi bozar.
     * @param item Kaldırılacak OrderItem.
     */
    public void removeOrderItem(OrderItem item) {
        if (this.orderItems != null) {
            this.orderItems.remove(item);
            item.setOrder(null); // İlişkiyi diğer taraftan da kaldır
        }
    }

     /**
     * Siparişe bir ödeme ekler ve çift yönlü ilişkiyi kurar.
     * @param payment Eklenecek Payment.
     */
    public void addPayment(Payment payment) {
        if (this.payments == null) {
            this.payments = new ArrayList<>();
        }
        this.payments.add(payment);
        payment.setOrder(this); // Çift yönlü ilişkiyi ayarla
    }


    // --- HashCode ve Equals ---
    // Lombok @Data kullanıldı. İlişkiler exclude edildiği için genellikle sorun olmaz.
    // Gerekirse ID bazlı manuel implementasyon yapılabilir.
}

// Açıklamalar:

/*
 * JSON Desteği:
 * Adres alanları için @JdbcTypeCode(SqlTypes.JSON) kullanıldı.
 * Bu, Hibernate 6 ve üzeri ile genellikle ek bir bağımlılığa gerek kalmadan çalışır.
 * Veritabanınızın JSON tipini desteklediğinden emin olun (MySQL 5.7.8+ destekler).
 * Eğer sorun yaşarsanız veya String olarak saklamak isterseniz
 * @Column(columnDefinition = "json") veya sadece @Column(columnDefinition = "TEXT")
 * kullanıp servis katmanında JSON dönüşümü yapabilirsiniz.
 * Şimdilik @JdbcTypeCode ile deneyelim.
 */

/*
 * Hesaplanan Alan (@Formula):
 * finalAmount alanını veritabanından okumak için @Formula kullanıldı.
 * Bu, JPA'nın bu alanı yazmaya çalışmasını engeller.
 * Eğer veritabanı şemasında GENERATED ALWAYS AS kullandıysanız ve JPA'nın bunu tanımasını istiyorsanız
 * (Hibernate buna özel annotationlar sunabilir veya sadece @Column(insertable = false, updatable = false)
 * ile yönetebilirsiniz), @Formula gereksiz olabilir.
 * @Formula daha veritabanı-bağımsız bir JPA yaklaşımıdır.
 * Şimdilik @Formula kalsın.
 */

/*
 * İlişkiler:
 * User, Coupon, OrderItem, Payment ile ilişkiler kuruldu.
 * OrderItem ve Payment için CascadeType.ALL, orphanRemoval = true ayarı,
 * siparişle birlikte bu alt kayıtların da yönetilmesini sağlar.
 */

/*
 * Yardımcı Metotlar:
 * addOrderItem, removeOrderItem, addPayment gibi metodlar,
 * çift yönlü ilişkileri (hem Order'dan Item/Payment'a hem de Item/Payment'dan Order'a referans)
 * kolayca kurmak ve yönetmek için eklenmiştir.
 * Bu, özellikle entity'leri oluştururken/güncellerken tutarlılığı sağlamaya yardımcı olur.
 */
