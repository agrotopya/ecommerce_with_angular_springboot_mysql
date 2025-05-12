Harika bir ilerleme kaydetmişsiniz! Projenizin mevcut durumunu ve `apidocs.md` ile `frontend.md`'deki planlarınızı dikkate alarak detaylı bir yapılacaklar listesi hazırladım.

**Önemli Not:** Bu liste, sağladığınız dosya yapısı ve içeriklerine dayanmaktadır. Bazı `[X]` işaretli maddeler, temel yapının kurulmuş olduğu anlamına gelir, ancak daha detaylı test, hata ayıklama veya UI iyileştirmeleri gerektirebilir. `[ ]` işaretli maddeler ise ya eksik ya da plandan farklı implementasyonlar veya tamamlanması gereken önemli adımlar içerir.

---

**Kritik Sorunlar ve Öncelikli Yapılacaklar**

*   **[X] Modül Stratejisi ve Standalone Bileşenler**
*   **[X] Servis ve Bileşen Duplikasyonu/Konsolidasyonu**
*   **[X] API Endpoint Sabitleri (`api-endpoints.ts`)**
*   **[X] Paylaşılan Modeller (`shared/models/`)**
*   **[X] Paylaşılan Enum'lar (`shared/enums/`)**
*   **[X] Genel İyileştirmeler ve Refactoring**
*   **[X] Mevcut Hataların Çözümü (ng serve çıktıları ve Kullanıcı Geri Bildirimi)**
    *   **[X]** PaginatorComponent tip hatası giderildi.
    *   **[X]** ErrorInterceptor 401 ve diğer hata durumları için güncellendi.
    *   **[X]** SellerOrderController oluşturularak `/api/seller/orders/my` için 500/401 hatası giderildi.
    *   **[X]** MySellerOrderDetailComponent'te SSR sırasında API isteği yapılması engellenerek 401 hatası giderildi.
    *   **[X]** `FeelDetailComponent`'te `feelId`'nin `string`'den `number`'a dönüştürülmesi hatası giderildi.

---

**Detaylı Yapılacaklar Listesi (Kalanlar ve Kullanıcı Geri Bildirimine Göre Yeni Eklenenler)**

**I. Checkout ve Ödeme Akışı İyileştirmeleri**
*   (Tamamlandı)

**II. Shared Models & Enums (`src/app/shared/`)**
    *   (Tamamlandı)

**III. Core Services Implementation/Refinement (`src/app/core/services/`)**
*   **[X] `NotificationService`**
    *   (Tamamlandı)

**IV. Styling & Theming (Devam Ediyor)**
*   **[X] Global Stiller (`styles.scss`)**
*   **[X] `HeaderComponent` Tasarımı**
*   **[X] `HomeComponent` Tasarımı (Başlangıç)**
*   **[X] `HomeComponent` Hero Alanı İyileştirmesi**
*   **[X] `ProductCardComponent` Stilleri (Favori butonu ikon görünümü ve yerleşimi iyileştirildi).**
*   **[X] `ProductDetailComponent` Stilleri (Favori butonu ikon görünümü iyileştirildi, gereksiz CSS kaldırıldı).**
*   **[X] Material Icons fontu `index.html`'e eklendi (Favori ikonu sorunu için).**
*   **[ ] Genel Stil Uyumu:**
    *   `[ ]` Diğer bileşenlerin (butonlar, formlar, modallar vb.) renk paletine ve genel tasarım diline uygunluğunu sağla.
*   **[ ] (Opsiyonel) `HeaderComponent` Kullanıcı Menüsü:**
    *   `[ ]` Dışarı tıklandığında kapanmasını sağla.

**V. Feature Implementation (Kalanlar)**
*   **[X] Backend'de ürün silme işlemindeki 500 hatasını çöz.**
*   **[ ] Backend'deki ürün silme (soft delete) özelliğini kapsamlıca test et.**
*   **[X] Backend'de resim yüklerken sıkıştırma özelliği eklendi ve dosya yükleme limiti sorunu giderildi.**
*   **[X] Backend'deki resim yükleme, sıkıştırma ve dosya limiti özelliklerini kapsamlıca test et.**
*   **[X] Backend'de "En Çok Satanlar" özelliği implemente edildi.**
*   **[X] Ana Sayfa - "En Çok Satanlar" Bölümü: Frontend güncellendi.**
*   **[ ] Ana Sayfa - "En Çok Satanlar" bölümünün işlevselliğini (backend sayım ve frontend sıralama) kapsamlıca test et.**
*   **[ ] Ana Sayfa - "Flaş Ürünler" Bölümü: Kriteri netleştir ve implemente et.**
*   **[ ] Ürün Resimleri ve Stok Bilgileri (Tekrar Kontrol).**
*   **[X] Admin Panel - User Management (Tekrar Kontrol).**
*   **[X] Admin Panel - Product Management (Tekrar Kontrol).**
*   **[X] Admin Paneli - Order Management (Listeleme ve Filtreleme - Frontend).**
*   **[ ] Backend'e Admin Sipariş Listeleme için eklenen yeni filtreleri (tarih aralığı, müşteri anahtar kelimesi) implemente et.** (Backend)
*   **[X] Admin Paneli - Product Management (Tekrar Kontrol).**
*   **[ ] Admin Paneli - Order Management (Detay ve Güncelleme).**
*   **[ ] Seller Dashboard Buton Yönlendirme Sorunu.**
*   **[ ] APP_INITIALIZER Kullanımı (Gözden Geçir).**
*   **[ ] `ProductService` (`product.service.ts`) metod birleştirme.**
*   **[ ] `ProductListComponent` (`product-list.component.ts/.html`) kategori filtresi.**
*   **[X] Ürün Yorumları Özelliği.**
*   **[X] İstek Listesi (Wishlist) Özelliği (Temel).**
    *   **[X] İstek Listesi işlevselliğini kapsamlıca test et.**
*   **[X] Paylaşılan Bileşenler:**
    *   **[X]** `LoadingSpinnerComponent` oluşturuldu ve entegre edildi.
    *   **[X]** `PaginatorComponent` oluşturuldu ve entegre edildi.
*   **[X] `OrderListComponent` (`order-list.component.ts/.html`) sayfalama.**
*   **[X] "Feels" Özelliği (Temel, Satıcı Yönetimi - Oluşturma/Düzenleme/Listeleme, Admin Yönetimi - Listeleme/Durum Güncelleme):**
    *   (Tamamlandı)
*   **[X] "Feels" Özelliği (İleri Düzey ve Test):**
    *   `[ ]` `FeelDetailComponent`'te `constructor` içindeki `effect` ile `paramMap` kullanımı gözden geçirilmeli (belki `toSignal` ile).
    *   **[X]** `MyFeelListComponent` ve `AdminFeelListComponent`'e silme onayı için `ConfirmationModalComponent` entegre edildi.
    *   **[X]** Tüm "Feels" akışları (listeleme, detay, oluşturma, düzenleme, beğenme, satıcı yönetimi, admin yönetimi) kapsamlıca test edildi. (Kullanıcı onayıyla)
*   **[ ] `Profile Feature` Eksik Bileşenler:**
    *   `[ ]` (Gelecek) `MySubscriptionsComponent` oluştur ve rotasını ekle.
*   **[ ] `Admin Feature` Eksik Bileşenler (Gelecek):** Admin Kupon, Yorum Yönetimi.
*   **[ ] `Seller Feature` Eksik Bileşenler (Gelecek):** Satıcı Sipariş (Detay ve Güncelleme), AI Görsel Üretici.
*   **[ ] `Categories Feature` (Admin için):**
    *   `[ ]` `CategoryListComponent` ve `CategoryFormComponent`'i admin paneline entegre et, stillerini güncelle.
*   **[ ] Diğer Özellikler (Notifications, Subscriptions):**
    *   Bu özellikler için ilgili servis metodlarını, bileşenleri ve rotaları oluştur/tamamla.

**VI. Testler ve Son Kontroller**

1.  `[ ]` **Fonksiyonel Testler:** Tüm kullanıcı akışlarını test edin.
2.  `[ ]` **API Entegrasyonu:** Tüm API çağrılarının beklendiği gibi çalıştığını ve verilerin doğru işlendiğini doğrulayın.
3.  `[ ]` **Hata Yönetimi:** API hataları ve form validasyonları için kullanıcı dostu hata mesajlarının gösterildiğinden emin olun.
4.  `[ ]` **Duyarlılık (Responsiveness):** Uygulamayı farklı ekran boyutlarında test edin.
5.  `[ ]` **Kod Kalitesi:** Linter kullanımı, kod gözden geçirmeleri.

---

**Geliştirmeler (İleride Yapılacaklar)**
*   (Değişiklik yok)

Bu liste oldukça kapsamlı oldu. Önceliklerinizi belirleyerek adım adım ilerleyebilirsiniz. Başarılar!
