package com.fibiyo.ecommerce.domain.entity;

import jakarta.persistence.*; // JPA importları
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString; // ToString importunu ekledik

import java.util.ArrayList; // List implementasyonu için
import java.util.List; // İlişkiler için

@Entity
@Table(name = "categories", uniqueConstraints = {
        @UniqueConstraint(columnNames = "slug", name = "uk_category_slug") // slug unique olmalı
})
@Data // Lombok: Getter, Setter, equals, hashCode, toString
@NoArgsConstructor // Lombok: Boş constructor
@AllArgsConstructor // Lombok: Tüm alanları içeren constructor
public class Category {


    public Category getParentCategory() {
        return this.parentCategory;
    }
    public Long getId() {
        return this.id;
    }
    
    public String getName() {
        return this.name;
    }
    

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Kategori adı boş olamaz")
    @Size(max = 255, message = "Kategori adı en fazla 255 karakter olabilir")
    @Column(nullable = false)
    private String name;

    @NotBlank(message = "Kategori slug boş olamaz")
    @Size(max = 255, message = "Kategori slug en fazla 255 karakter olabilir")
    @Column(nullable = false, unique = true)
    private String slug; // SEO uyumlu URL parçası (örn: "elektronik-urunler")

    @Column(columnDefinition = "TEXT", nullable = true)
    private String description;

    @Column(name = "image_url", length = 1024, nullable = true)
    private String imageUrl;

    @NotNull
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true; // Varsayılan olarak aktif

    // =============================================
    // İLİŞKİLER (Relationships)
    // =============================================

    // Hiyerarşik Kategori İlişkisi (Kendine referans)
    @ManyToOne(fetch = FetchType.LAZY) // Üst kategoriyi direkt yüklemeye gerek yok genelde
    @JoinColumn(name = "parent_category_id", nullable = true) // FK: parent_category_id
    @ToString.Exclude // toString döngüsünü engellemek için
    @EqualsAndHashCode.Exclude // equals/hashCode döngüsünü engellemek için
    private Category parentCategory; // Üst kategori referansı

    // Bu kategorinin alt kategorileri
    @OneToMany(mappedBy = "parentCategory", fetch = FetchType.LAZY, cascade = CascadeType.ALL) // Alt kategorileri silmek gerekebilir? Veya SET NULL. Şemaya göre SET NULL
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Category> subCategories = new ArrayList<>();

    // Bu kategoriye ait ürünler
    // mappedBy: Product entity'sindeki Category referansının adı ("category")
    // cascade: Kategori silinince ürünler silinsin mi? Hayır, genelde ürünler kalır (ya da SET NULL olur).
    // Şemada ON DELETE SET NULL demişiz. Cascade eklemeyelim, JPA otomatik olarak FK'yı null yapacaktır (eğer Product.categoryId nullable ise).
    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
    @ToString.Exclude // toString içine tüm ürünleri dahil etmemek için
    @EqualsAndHashCode.Exclude // equals/hashCode içine tüm ürünleri dahil etmemek için
    private List<Product> products = new ArrayList<>();

    // Constructor'lar, getter/setter'lar Lombok tarafından sağlanıyor.

    // --- HashCode ve Equals (Lombok @Data'ya Ek Dikkat) ---
    // Özellikle hiyerarşik ve karşılıklı ilişkilerde Lombok'un default equals/hashCode metotları
    // StackOverflowError'a neden olabilir. Bu nedenle @ToString.Exclude ve @EqualsAndHashCode.Exclude kullandık.
    // Gerekirse sadece ID üzerinden kontrol eden manuel metodlar yazılabilir.

}

// Paket ve Importlar:
// Doğru pakette (com.fibiyo.ecommerce.domain.entity) ve gerekli importlar mevcut.

/*
 * Slug:
 * Kategori için SEO dostu URL oluşturmada kullanılan unique bir alan eklendi (slug).
 * Bu alan genellikle kategori adı üzerinden otomatik olarak (örn: servis katmanında) oluşturulur.
 */

/*
 * Hiyerarşi:
 * Kategori kendi kendine @ManyToOne (üst kategori) ve @OneToMany (alt kategoriler) ilişkisi kurarak
 * hiyerarşik yapıyı destekler.
 */

/*
 * Ürün İlişkisi:
 * Kategoriye ait ürünleri gösteren @OneToMany ilişkisi kuruldu.
 */

/*
 * @ToString.Exclude ve @EqualsAndHashCode.Exclude:
 * Özellikle çift yönlü ve hiyerarşik ilişkilerde sonsuz döngüleri (StackOverflowError) önlemek için
 * ilişkili alanları Lombok'un toString(), equals(), ve hashCode() metodlarından dışarıda bıraktık.
 * Bu önemlidir.
 */

/*
 * Cascade:
 * subCategories için cascade = CascadeType.ALL kullanıldı, bu üst kategori kaydedildiğinde
 * alt kategorilerin de kaydedilmesini sağlar.
 * Ancak products için cascade eklemedik, çünkü kategori silindiğinde ürünlerin ne olacağı
 * (SET NULL, pasife çekme vb.) genellikle iş mantığı ile servis katmanında belirlenir.
 * Şemadaki ON DELETE SET NULL tanımı veritabanı seviyesinde bunu yönetir.
 */
