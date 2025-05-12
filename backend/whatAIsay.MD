**Fibiyo E-Ticaret Platformu - Detaylı Proje Raporu**

**1. Giriş ve Proje Özeti**

Fibiyo, kapsamlı bir e-ticaret platformu olarak tasarlanmıştır. Hem müşterilere alışveriş deneyimi sunmayı hem de satıcıların ürünlerini listeleyip satış yapabilmelerini hedefler. Ayrıca, platformun yönetimi için bir admin paneli bulunmaktadır. Proje, modern bir backend mimarisi (Spring Boot) ve reaktif bir frontend (Angular) ile geliştirilmiştir. Yapay zeka (AI) özellikleri (görsel üretimi, yorum özeti), kısa video ("Feels") entegrasyonu ve abonelik sistemi gibi yenilikçi özellikler içermektedir.

**Raporun Amacı:** Bu rapor, Fibiyo projesinin sistem mimarisini, backend ve frontend yapısını, kullanılan teknolojileri, temel bileşenlerini ve veri akışlarını detaylı bir şekilde açıklamayı amaçlamaktadır. Ayrıca, projenin potansiyel gelişim alanlarına da değinilecektir.

---

**2. Sistem Mimarisi (System Architecture)**

Genel sistem mimarisi, tipik bir üç katmanlı (veya n-katmanlı) web uygulaması modelini takip eder ve çeşitli mikroservis benzeri etkileşimler içerebilir (özellikle AI ve ödeme gibi dış servislerle).

**Diagram Açıklaması (Kavramsal):**

```
+---------------------+      +-----------------------+      +---------------------+
|     Kullanıcılar    |----->|  Frontend (Angular)   |<---->|  Backend (Spring   |
| (Müşteri, Satıcı,  |<-----|  (Web Tarayıcısı)     |      |  Boot / Java)       |
|      Admin)         |      +-----------------------+      +----------+----------+
+---------------------+               ^    |                          |
                                      |    | (API İstekleri/Yanıtları)  |
                                      |    v                          v
                              +-------+---------+        +----------+----------+
                              | Load Balancer   |------->|  API Gateway (Ops.) |
                              | (Opsiyonel)     |        +---------------------+
                              +-----------------+                   |
                                                                    v
                                                      +-------------+-------------+
                                                      |  Veritabanı (MySQL)       |
                                                      +---------------------------+
                                                                    ^
                                                                    |
                                  +---------------------------------+---------------------------------+
                                  |                                 |                                 |
                      +-----------+-----------+        +------------+------------+       +------------+------------+
                      |  Ödeme Servisi (Stripe)|        |  AI Servisi (OpenAI)    |       |  Dosya Depolama Servisi |
                      +-----------------------+        +-------------------------+       |  (Lokal/Cloud)        |
                                                                                         +-------------------------+
```

*   **Kullanıcılar:** Platforma web tarayıcıları üzerinden erişen müşteriler, satıcılar ve adminlerdir.
*   **Frontend (Angular):** Kullanıcı arayüzünü sunar, kullanıcı etkileşimlerini yönetir ve backend API'sine istekler gönderir. Tarayıcıda çalışır.
*   **Load Balancer (Opsiyonel):** Yüksek trafik durumlarında backend sunucularına yükü dağıtmak için kullanılabilir.
*   **API Gateway (Opsiyonel):** Birden fazla mikroservis varsa (bu projede tam olarak öyle olmasa da, dış servislerle entegrasyon düşünüldüğünde), istekleri yönlendirme, güvenlik ve rate limiting gibi işlevler için kullanılabilir. Şu anki yapıda direkt backend'e erişim olabilir.
*   **Backend (Spring Boot):** İş mantığını, veri işlemeyi, API endpoint'lerini ve dış servis entegrasyonlarını yönetir.
*   **Veritabanı (MySQL):** Tüm kalıcı verileri (kullanıcılar, ürünler, siparişler vb.) saklar.
*   **Dış Servisler:**
    *   **Ödeme Servisi (Stripe):** Güvenli ödeme işlemlerini yönetir.
    *   **AI Servisi (OpenAI):** Görsel üretimi ve potansiyel olarak metin tabanlı AI özellikleri için kullanılır.
    *   **Dosya Depolama Servisi:** Ürün görselleri, "Feel" videoları gibi dosyaların saklandığı yerdir (lokal `uploads/` klasörü veya bulut tabanlı bir çözüm olabilir).

---

**3. Backend Mimarisi**

Backend, Spring Boot çatısı kullanılarak katmanlı bir mimariyle tasarlanmıştır. Bu yapı, modülerliği, test edilebilirliği ve bakım kolaylığını hedefler.

**3.1. Kullanılan Teknolojiler ve Kütüphaneler:**

*   **Framework:** Spring Boot 3.4.5
*   **Dil:** Java 17
*   **Veritabanı:** MySQL (schema.sql ile tanımlanmış)
*   **ORM:** Spring Data JPA (Hibernate implementasyonu ile)
*   **Güvenlik:** Spring Security, JWT (JSON Web Tokens)
*   **API:** Spring Web (REST Controller'lar)
*   **Build Aracı:** Apache Maven
*   **DTO Mapping:** MapStruct
*   **Ödeme Entegrasyonu:** Stripe Java SDK
*   **AI Entegrasyonu:** OpenAI Java SDK, WebClient (HTTP istekleri için)
*   **E-posta:** Spring Boot Starter Mail
*   **Dosya İşlemleri:** Thumbnailator (görsel sıkıştırma), Apache Commons IO
*   **Diğer:** Lombok, Jakarta Validation

**3.2. Dizin Yapısı ve Katmanlama (Conceptual Layered Architecture):**

Backend kodu, sorumlulukların ayrılması prensibine göre mantıksal katmanlara ayrılmıştır.

```
+---------------------------------------------------------------------------------+
| Presentation Katmanı (Web Layer)                                                |
| +-----------------------------------------------------------------------------+ |
| |                       Controller'lar (`infrastructure/web/controller`)      | |
| | (HTTP İsteklerini Karşılama, Yanıtları Hazırlama, Servisleri Çağırma)         | |
| +-----------------------------------------------------------------------------+ |
+------------------------------------^--------------------------------------------+
                                     | (Servis Çağrıları)
+------------------------------------v--------------------------------------------+
| Application Katmanı (İş Mantığı)                                                |
| +---------------------------+  +---------------------------+  +---------------+ |
| |    Servisler (`application/service`)                    |  | DTO'lar (`application/dto`)                 |  | Mapper'lar  | |
| | (İş kuralları,        |  | (Veri Transfer Nesneleri) |  | (`application/mapper`)| |
| |  validasyonlar,       |  +---------------------------+  +---------------+ |
| |  orkestrasyon)        |  +---------------------------+  +---------------+ |
| +---------------------------+  | Exception'lar (`application/exception`)     |  | Utility'ler | |
|                                | (Özel Hata Sınıfları)   |  | (`application/util`)| |
|                                +---------------------------+  +---------------+ |
+------------------------------------^--------------------^--------------------^---+
                                     | (Repository       | (Entity Kullanımı) | (Adapter
                                     |  Kullanımı)       |                    |  Kullanımı)
+------------------------------------v--------------------v--------------------v---+
| Domain Katmanı (Çekirdek Varlıklar)                                           |
| +---------------------------+  +-------------------------------------------+ |
| |    Entity'ler (`domain/entity`)                         |  | Enum'lar (`domain/enums`)                       | |
| | (Veritabanı Varlıkları) |  | (Sabit Değerler)                          | |
| +---------------------------+  +-------------------------------------------+ |
+------------------------------------^--------------------^-----------------------+
                                     | (Veritabanı        | (Dış Servis
                                     |  Etkileşimi)       |  Etkileşimi)
+------------------------------------v--------------------v-----------------------+
| Infrastructure Katmanı (Dış Dünya ile Etkileşim)                                |
| +------------------------------+  +------------------------------------------+ |
| | Persistence (`infrastructure/persistence`) |  | Security (`infrastructure/security`)           | |
| |  - Repository'ler            |  |  - JWT, UserDetails, EntryPoint, Filter  | |
| |  - Specification'lar       |  +------------------------------------------+ |
| +------------------------------+  +------------------------------------------+ |
|                                   | Adapters (`infrastructure/adapter`)        | |
|                                   |  - Email, AI, Storage, Payment           | |
|                                   +------------------------------------------+ |
+---------------------------------------------------------------------------------+
| Configuration Katmanı (`config`)                                                |
| (Spring Bean Tanımları, Güvenlik, CORS, OpenAI, Stripe vb. ayarları)            |
+---------------------------------------------------------------------------------+
```

*   **Presentation Katmanı (Web Layer - `infrastructure/web/controller`):**
    *   Dış dünyadan gelen HTTP isteklerini alır (örn: `/api/auth/login`, `/api/products`).
    *   Gelen istekleri doğrular (DTO validasyonu ile).
    *   İlgili servis metodlarını çağırarak iş mantığını tetikler.
    *   Servislerden dönen sonuçları uygun HTTP yanıtlarına (JSON formatında DTO'lar) dönüştürerek kullanıcıya gönderir.
    *   `@RestController`, `@GetMapping`, `@PostMapping` gibi Spring MVC annotation'larını kullanır.
    *   Yetkilendirme için `@PreAuthorize` kullanır.
*   **Application Katmanı (`application` paketi):**
    *   Projenin ana iş mantığını içerir.
    *   **Servisler (`application/service` ve `impl`):** İş kurallarını uygular, farklı domain entity'leri ve repository'ler arasında koordinasyonu sağlar. Controller'lar tarafından çağrılır. Örneğin, `AuthServiceImpl` kullanıcı kaydı ve girişi mantığını yönetirken, `OrderServiceImpl` sipariş oluşturma ve güncelleme mantığını içerir.
    *   **DTO'lar (Data Transfer Objects - `application/dto`):** Katmanlar arası veri transferi için kullanılır. Özellikle API istek ve yanıtlarında kullanılır. Backend DTO'ları (örn: `LoginRequest`, `ProductResponse`) ve frontend DTO'ları (eğer farklıysa) burada tanımlanır. Validation annotation'ları (`@NotBlank`, `@Size` vb.) içerirler.
    *   **Mapper'lar (`application/mapper`):** Entity'ler ve DTO'lar arasında dönüşümü sağlar. Projede MapStruct kullanıldığı görülmektedir (`CartItemMapperImpl` gibi üretilmiş dosyalar). Bu, boilerplate dönüşüm kodunu azaltır.
    *   **Exception'lar (`application/exception`):** Özel hata sınıflarını (örn: `ResourceNotFoundException`, `BadRequestException`) ve global hata yakalayıcıyı (`GlobalExceptionHandler`) içerir.
    *   **Utility'ler (`application/util`):** Proje genelinde kullanılabilecek yardımcı sınıfları (örn: `SlugUtils`) içerir.
*   **Domain Katmanı (`domain` paketi):**
    *   Projenin çekirdek iş varlıklarını ve kurallarını tanımlar. Diğer katmanlardan bağımsız olmalıdır.
    *   **Entity'ler (`domain/entity`):** Veritabanı tablolarına karşılık gelen JPA varlıklarıdır (örn: `User`, `Product`, `Order`). `@Entity`, `@Table`, `@Id`, `@Column`, `@ManyToOne` gibi JPA annotation'larını içerirler.
    *   **Enum'lar (`domain/enums`):** Proje içinde kullanılan sabit değer kümelerini tanımlar (örn: `Role`, `OrderStatus`, `DiscountType`).
*   **Infrastructure Katmanı (`infrastructure` paketi):**
    *   Dış sistemlerle ve altyapısal konularla ilgilenir.
    *   **Persistence (`infrastructure/persistence`):**
        *   **Repository'ler (`repository`):** Spring Data JPA arayüzleridir. Veritabanı işlemlerini (CRUD) soyutlar (örn: `UserRepository`, `ProductRepository`).
        *   **Specification'lar (`specification`):** Spring Data JPA Specifications API'sini kullanarak dinamik ve karmaşık sorgular oluşturmak için kullanılır (örn: `ProductSpecifications`, `OrderSpecifications`).
    *   **Security (`infrastructure/security`):** Kimlik doğrulama ve yetkilendirme ile ilgili sınıfları içerir. `JwtTokenProvider` (JWT oluşturma ve doğrulama), `JwtAuthenticationFilter` (her istekte JWT'yi kontrol etme), `CustomUserDetailsService` (kullanıcı bilgilerini Spring Security için yükleme), `AuthEntryPointJwt` (yetkisiz erişim hatalarını handle etme) ve `ProductSecurity`, `OrderSecurity` gibi özel güvenlik kontrolleri için sınıflar.
    *   **Adapters (`infrastructure/adapter`):** Dış servislerle (AI, Ödeme, E-posta, Dosya Depolama) entegrasyonu sağlayan sınıfları içerir. Bu, dış servislerin implementasyon detaylarını iş mantığından soyutlar. Örneğin, `OpenAiServiceImpl` (aslında bir adaptör görevi de görür) veya `LocalStorageServiceImpl`.
*   **Configuration Katmanı (`config` paketi):**
    *   Spring Boot konfigürasyonlarını içerir. `SecurityConfig` (CORS ayarları, JWT filtre entegrasyonu, endpoint yetkilendirmeleri), `StripeConfig` (Stripe API anahtarını ayarlama), `OpenAiConfig` (OpenAI client bean'ini oluşturma), `WebConfig` (statik dosya sunumu) gibi sınıflar.

**3.3. Backend Component Diagram (Kavramsal Paketler ve Etkileşimleri):**

Bu diagram, backend'deki ana paketlerin/modüllerin birbirleriyle nasıl etkileşimde bulunduğunu gösterir.

```mermaid
graph LR
    subgraph "Presentation (Web)"
        A[Controller'lar]
    end

    subgraph "Application (İş Mantığı)"
        B[Servisler]
        C[DTO'lar]
        D[Mapper'lar]
        E[Exception'lar]
    end

    subgraph "Domain (Çekirdek Varlıklar)"
        F[Entity'ler]
        G[Enum'lar]
    end

    subgraph "Infrastructure (Altyapı)"
        H[Repository'ler]
        I[Güvenlik Bileşenleri]
        J[Adaptörler (AI, Ödeme vb.)]
        K[Storage Servisi]
    end

    subgraph "Configuration"
        L[Konfigürasyon Sınıfları]
    end

    A -->|kullanır| B
    A -->|kullanır| C
    A -->|kullanır| E

    B -->|kullanır| D
    B -->|kullanır| C
    B -->|kullanır| F
    B -->|kullanır| G
    B -->|kullanır| H
    B -->|kullanır| J
    B -->|kullanır| K
    B -->|kullanır| I  // Servisler güvenlik context'ini kullanabilir

    D -->|dönüştürür| C
    D -->|dönüştürür| F

    H -->|erişir| F     // Repository'ler Entity'leri yönetir
    H -->|erişir| G     // ve Enum'ları kullanabilir

    I -->|etkiler| A    // Güvenlik filtreleri Controller'lardan önce çalışır
    I -->|kullanır| F   // User entity'sini kullanır

    J -->|dış servisler| M{Dış Servisler (Stripe, OpenAI)}
    K -->|dosya sistemi| N[Dosya Sistemi/Bulut Depolama]

    L -->|yapılandırır| I
    L -->|yapılandırır| J
    L -->|yapılandırır| K
```

**Açıklama:**

*   **Controller'lar (A):** Kullanıcıdan gelen HTTP isteklerini alır, DTO'ları (C) kullanarak veriyi doğrular, iş mantığı için Servisleri (B) çağırır ve hataları (E) yakalayıp yönetebilir.
*   **Servisler (B):** Ana iş mantığını içerir. Mapper'ları (D) kullanarak DTO'lar (C) ve Entity'ler (F) arasında dönüşüm yapar. Veritabanı işlemleri için Repository'leri (H) kullanır. Gerekirse dış servisler için Adaptörleri (J) veya Storage Servisini (K) çağırır. Proje Enum'larını (G) ve güvenlik context'ini (I) kullanabilir.
*   **Repository'ler (H):** JPA Entity'lerini (F) kullanarak veritabanı ile iletişim kurar.
*   **Güvenlik Bileşenleri (I):** JWT işlemleri, kullanıcı kimlik doğrulaması ve yetkilendirme gibi güvenlik mekanizmalarını sağlar. User entity'sini (F) kullanır.
*   **Adaptörler (J) / Storage Servisi (K):** Dış servislerle (M) veya dosya sistemiyle (N) etkileşim kurar.
*   **Konfigürasyon Sınıfları (L):** Spring Security (I), OpenAI (J), Stripe (J) gibi sistemlerin yapılandırmasını sağlar.

**3.4. Veritabanı Şeması (Database Schema - `schema.sql` Özeti):**

`schema.sql` dosyanız MySQL 8.0 için detaylı bir DDL (Data Definition Language) içeriyor. Ana tablolar ve ilişkileri şunlardır:

*   **`users`**: Kullanıcı bilgileri, rolleri, sosyal medya login detayları, abonelik bilgileri, AI kota bilgileri.
*   **`categories`**: Hiyerarşik kategori yapısı (parent\_category\_id ile).
*   **`products`**: Ürün detayları, fiyat, stok, SKU, resim URL'leri, satıcı ve kategori ilişkileri, onay ve aktiflik durumu, AI özellikleri. Soft delete (`is_deleted`) ve optimistic locking (`version`) için alanlar içerir.
*   **`product_images`**: Bir ürüne ait çoklu resimler.
*   **`coupons`**: İndirim kuponları, türleri, değerleri, geçerlilik tarihleri.
*   **`orders`**: Müşteri siparişleri, durumları, adres bilgileri, ödeme detayları, kullanılan kupon.
*   **`order_items`**: Siparişteki her bir ürün kalemi, sipariş anındaki fiyatı.
*   **`payments`**: Ödeme kayıtları, yöntemleri, işlem ID'leri, durumları.
*   **`reviews`**: Ürün yorumları, puanları, onay durumu.
*   **`notifications`**: Kullanıcılara gönderilen bildirimler.
*   **`wishlist_items`**: Kullanıcıların istek listesindeki ürünler.
*   **`carts`**: Kullanıcıların aktif alışveriş sepetleri.
*   **`cart_items`**: Sepetteki ürün kalemleri.
*   **`feels`**: Satıcıların yüklediği kısa video ("Feel") içerikleri, ilişkili ürünleri, görüntülenme ve beğeni sayıları.
*   **`feel_likes`**: Kullanıcıların hangi "Feel"leri beğendiğini tutar.

Şemada foreign key kısıtlamaları, unique constraint'ler ve performans için index'ler doğru bir şekilde tanımlanmıştır. `ON DELETE SET NULL` ve `ON DELETE CASCADE` gibi stratejiler ilişkisel bütünlüğü korumak için kullanılmıştır. `product.finalAmount` gibi hesaplanan alanların (`GENERATED ALWAYS AS`) artık servis katmanında yönetileceği belirtilmiştir.

---

**4. Frontend Mimarisi**

Frontend, Angular ve TypeScript kullanılarak geliştirilmiştir. Modüler bir yapı, reaktif programlama (Signals) ve en iyi pratikler hedeflenmiştir.

**4.1. Kullanılan Teknolojiler ve Kütüphaneler:**

*   **Framework:** Angular (v19.2.x)
*   **Dil:** TypeScript (v5.7.x)
*   **Stil:** SCSS
*   **Build Aracı:** Angular CLI
*   **HTTP İstemcisi:** Angular HttpClient (muhtemelen `withFetch` ile)
*   **Routing:** Angular Router
*   **Formlar:** Angular Reactive Forms
*   **State Management:** Angular Signals (tercih edilen)
*   **UI Kütüphanesi:** Angular Material (kısmen kullanıldığı görülüyor: MatCard, MatFormField, MatInput vb.)
*   **Diğer:** `ngx-infinite-scroll` (potansiyel olarak "Feels" listesi gibi yerlerde sonsuz kaydırma için)

**4.2. Dizin Yapısı ve Modülerlik (Angular Standalone API Odaklı):**

Frontend, Angular'ın modern Standalone API'sini kullanarak modülerliği hedefler.

```
frontend/
├── src/
│   ├── app/
│   │   ├── app.component.ts           # Ana uygulama bileşeni
│   │   ├── app.config.ts              # Ana uygulama konfigürasyonu (providers)
│   │   ├── app.routes.ts              # Ana yönlendirme (lazy loading ile)
│   │   │
│   │   ├── core/                      # Temel servisler, guard'lar, interceptor'lar
│   │   │   ├── constants/             # API endpointleri gibi sabitler
│   │   │   ├── guards/                # auth.guard.ts, role.guard.ts
│   │   │   ├── interceptors/          # auth.interceptor.ts, error.interceptor.ts
│   │   │   └── services/              # api.service.ts, auth.service.ts, storage.service.ts, notification.service.ts (core)
│   │   │
│   │   ├── shared/                    # Paylaşılan, yeniden kullanılabilir öğeler
│   │   │   ├── components/            # PaginatorComponent, LoadingSpinnerComponent, ConfirmationModalComponent, NotificationContainerComponent
│   │   │   ├── models/                # Backend DTO'larına karşılık gelen arayüzler
│   │   │   ├── enums/                 # Backend enum'larına karşılık gelen enum'lar
│   │   │   └── pipes/                 # nl2br.pipe.ts, time-ago.pipe.ts
│   │   │
│   │   ├── layout/                    # Ana sayfa düzeni bileşenleri
│   │   │   ├── main-layout/           # Genel kullanıcı arayüzü için layout
│   │   │   ├── admin-layout/          # Admin paneli için layout
│   │   │   ├── header/
│   │   │   └── footer/
│   │   │
│   │   └── features/                  # Özellik bazlı modüller/klasörler (her biri kendi route, component ve servislerini içerebilir)
│   │       ├── home/
│   │       ├── auth/
│   │       ├── products/
│   │       ├── categories/
│   │       ├── cart/
│   │       ├── checkout/
│   │       ├── orders/
│   │       ├── profile/
│   │       ├── wishlist/
│   │       ├── notifications/ (bileşenleri var, servisi core'da olabilir)
│   │       ├── feels/
│   │       ├── admin/
│   │       │   ├── dashboard/
│   │       │   ├── user-management/
│   │       │   ├── product-management/
│   │       │   ├── order-management/
│   │       │   └── components/ (admin'e özel paylaşılan bileşenler)
│   │       ├── seller/
│   │       │   ├── dashboard/
│   │       │   ├── product-management/
│   │       │   ├── order-management/
│   │       │   └── components/ (seller'a özel paylaşılan bileşenler)
│   │       ├── subscriptions/
│   │       └── reports/ (gelecekte)
│   │
│   ├── assets/
│   └── environments/
├── angular.json
├── package.json
└── tsconfig.json
```

*   **`core/`**: Uygulamanın temel mantığını içerir. `ApiService` backend ile iletişimi sağlar. `AuthService` kimlik doğrulama ve kullanıcı durumunu yönetir. `AuthGuard` ve `RoleGuard` route erişimlerini kontrol eder. `AuthInterceptor` giden isteklere JWT ekler, `ErrorInterceptor` global hataları yakalar.
*   **`shared/`**: Projenin farklı yerlerinde yeniden kullanılabilen bileşenler (örn: `PaginatorComponent`, `NotificationContainerComponent`), veri modelleri (`product.model.ts`, `user.model.ts` vb.) ve enum'lar (`role.enum.ts`, `order-status.enum.ts` vb.) burada bulunur.
*   **`layout/`**: Ana sayfa düzenini oluşturan bileşenlerdir. `HeaderComponent` (navigasyon, arama, kullanıcı menüsü), `FooterComponent` ve bu ikisini içeren `MainLayoutComponent` ve `AdminLayoutComponent`.
*   **`features/`**: Uygulamanın ana işlevsel bölümleridir. Her özellik kendi alt klasöründe, kendi bileşenlerini, servislerini (gerekirse) ve route tanımlarını içerir. Örneğin, `products` özelliği ürün listeleme, detay ve filtreleme bileşenlerini ve `ProductService`'i barındırır. `admin` ve `seller` özellikleri de kendi alt özelliklerini (kullanıcı yönetimi, ürün yönetimi vb.) içerir. Bu yapı, Standalone API ile daha da basitleştirilmiştir.

**4.3. Frontend Component Diagram (Örnek Bir Akış - Ürün Listeleme ve Detay):**

Bu diagram, kullanıcı bir ürün listesini görüntülerken ve bir ürün detayına giderken bileşenlerin ve servislerin nasıl etkileşimde bulunduğunu gösterir.

```mermaid
graph TD
    User[Kullanıcı] -->|Route Navigasyonu| AppRouter[app.routes.ts]

    AppRouter -->|Lazy Load| ProductsRoutes[products.routes.ts]
    ProductsRoutes -->|Yönlendirir| PL[ProductListComponent]

    PL -->|Veri İsteği| PS[ProductService]
    PS -->|HTTP GET (Filtrelerle)| AS[ApiService]
    AS -->|API Çağrısı| BackendAPI[Backend API (/api/products)]
    BackendAPI -->|Page<Product>| AS
    AS -->|Page<Product>| PS
    PS -->|Page<Product>| PL

    PL -->|*ngFor ile render eder| PCC[ProductCardComponent]
    PCC -->|@Input() product| ProductData[Ürün Verisi]
    PCC -->|Kullanıcı Tıklaması| User
    User -->|Sepete Ekle Tıklaması| PCC
    PCC -->|Sepete Ekle İsteği| CartService[CartService]
    User -->|Detay Linki Tıklaması| AppRouter
    AppRouter -->|Lazy Load| ProductsRoutes
    ProductsRoutes -->|Route Param (:slug) ile Yönlendirir| PD[ProductDetailComponent]

    PD -->|Veri İsteği (slug ile)| PS
    PS -->|HTTP GET (slug ile)| AS
    AS -->|API Çağrısı| BackendAPI_Detail[/api/products/slug/:slug]
    BackendAPI_Detail -->|Product| AS
    AS -->|Product| PS
    PS -->|Product| PD
    PD -->|Görüntüler| ProductDetailsUI[Ürün Detayları UI]

    PD -->|Yorumları Listele| ReviewsComp[ProductReviewsComponent]
    ReviewsComp -->|Veri İsteği| ReviewService[ReviewService]
    ReviewService -->|HTTP GET| AS
    AS -->|API Çağrısı| BackendAPI_Reviews[/api/reviews/product/:id]
    BackendAPI_Reviews -->|Page<Review>| AS
    AS -->|Page<Review>| ReviewService
    ReviewService -->|Page<Review>| ReviewsComp

    subgraph "Product Feature"
        PL
        PCC
        PD
        PS
        ReviewsComp
        ReviewService
    end

    subgraph "Core"
        AppRouter
        AS
        CartService
    end
```

**Açıklama:**

1.  Kullanıcı `/products` (veya filtreli bir yol) adresine gider.
2.  `app.routes.ts` ilgili `products.routes.ts`'i lazy load eder.
3.  `ProductListComponent` aktifleşir.
4.  `ProductListComponent`, `ProductService`'i kullanarak (filtreler ve sayfalama bilgisiyle) backend'den ürünleri ister.
5.  `ProductService`, `ApiService` aracılığıyla `/api/products` endpoint'ine istek atar.
6.  Gelen `Page<Product>` verisi `ProductListComponent`'e döner.
7.  `ProductListComponent`, her bir ürün için `ProductCardComponent`'i render eder ve ürün verisini `@Input()` ile geçer.
8.  Kullanıcı bir ürün kartındaki detay linkine tıklarsa, `:slug` parametresi ile `ProductDetailComponent`'e yönlendirilir.
9.  `ProductDetailComponent`, `slug` bilgisini kullanarak `ProductService.getPublicProductBySlug()` ile ürün detaylarını çeker.
10. Ürün detayları gösterilirken, `ProductReviewsComponent` (eğer varsa) ilgili ürün ID'si ile `ReviewService` üzerinden yorumları çeker.
11. Kullanıcı "Sepete Ekle" butonuna tıklarsa, `ProductCardComponent` veya `ProductDetailComponent` `CartService.addItemToCart()`'ı çağırır.

**4.4. State Management (Angular Signals):**

Projenin `.clinerules/rules.md` dosyasında Angular Signals'ın tercih edildiği belirtilmiş. Bu, reaktifliği ve performansı artırmak için modern bir yaklaşımdır.

*   **Servislerde Durum:** `AuthService`'teki `currentUser` ve `isAuthenticated` gibi global durumlar `signal` ile yönetilir. `CartService`'teki sepet durumu da (`cart` ve `totalCartItems`) `signal` kullanır.
*   **Bileşenlerde Durum:** Bileşenler, kendi yerel durumları için veya servislerden gelen sinyalleri okumak için `signal` kullanır. `computed` sinyaller, diğer sinyallerden türetilen değerler için kullanılır (örn: `PaginatorComponent`'teki `pages`). `effect` ise sinyal değişikliklerine tepki olarak yan etkiler (örn: API çağrısı, loglama) çalıştırmak için kullanılır (`AuthService` ve `CartService` constructor'larında olduğu gibi).

**4.5. Routing ve Lazy Loading:**

*   `app.routes.ts` ana yönlendirme yapılandırmasını içerir.
*   Tüm `features/` altındaki modüller/klasörler için `loadChildren` kullanılarak **lazy loading** uygulanmıştır. Bu, başlangıç yükleme süresini optimize eder.
*   Route'lar, `AuthGuard` ve `RoleGuard` kullanılarak korunur.

---

**5. Deployment Mimarisi (Conceptual)**

Projenin canlı ortama nasıl dağıtılacağına dair spesifik bilgi olmasa da, tipik bir web uygulaması dağıtım mimarisi şöyle olabilir:

```
+---------------------+      +---------------------+      +---------------------+      +-----------------------+
| Kullanıcı Tarayıcısı|----->| CDN (Opsiyonel)     |----->|  Load Balancer      |----->| Web Sunucuları        |
+---------------------+      | (Frontend Statikleri)|      |                     |      | (Nginx/Apache vb.     |
                             +---------------------+      +----------+----------+      |  Angular Build'ini   |
                                                                     |                 |  Sunar)              |
                                                                     |                 +-----------------------+
                                                                     |
                                                                     | (API İstekleri Yönlendirme)
                                                                     v
                                                        +----------+----------+
                                                        |  Uygulama Sunucuları  |
                                                        |  (Spring Boot Jar    |
                                                        |   Cluster'ı)         |
                                                        +----------+----------+
                                                                   |
                                                                   v
                                                         +---------+---------+
                                                         | Veritabanı Sunucusu |
                                                         | (MySQL)           |
                                                         +-------------------+
                                                                   |
                                           +-----------------------+-----------------------+
                                           |                                               |
                               +-----------+-----------+                      +------------+------------+
                               | Ödeme Servisi (Stripe)|                      | AI Servisi (OpenAI)     |
                               +-----------------------+                      +-------------------------+
```

*   **CDN (Content Delivery Network):** Frontend'in statik dosyaları (JS, CSS, resimler) için kullanılarak global erişim hızlandırılabilir.
*   **Load Balancer:** Hem frontend (web sunucuları) hem de backend (uygulama sunucuları) için kullanılabilir, gelen istekleri dağıtarak performansı ve yedekliliği artırır.
*   **Web Sunucuları:** Nginx veya Apache gibi sunucular, Angular build'inden çıkan statik dosyaları sunar ve API isteklerini backend uygulama sunucularına yönlendirir.
*   **Uygulama Sunucuları:** Spring Boot uygulamasının çalıştığı sunuculardır. Genellikle bir cluster (küme) halinde çalışırlar.
*   **Veritabanı Sunucusu:** MySQL veritabanının çalıştığı sunucu. Yedeklilik ve performans için replikasyon veya cluster yapıları kullanılabilir.
*   **Dış Servisler:** Stripe ve OpenAI gibi servisler kendi altyapılarında çalışır ve API'ler aracılığıyla entegre olunur.

---

**6. Temel Özellikler (API ve Frontend Yapısına Göre)**

1.  **Kullanıcı Yönetimi ve Kimlik Doğrulama:**
    *   Kayıt, giriş, şifremi unuttum, şifre sıfırlama.
    *   Profil görüntüleme ve güncelleme.
    *   Rol tabanlı yetkilendirme (Müşteri, Satıcı, Admin).
2.  **Kategori Yönetimi:**
    *   Kategori listeleme (aktif, tümü, alt kategoriler, kök kategoriler).
    *   Kategori detay görüntüleme (ID ve slug ile).
    *   Admin: Kategori oluşturma, güncelleme, silme.
3.  **Ürün Yönetimi:**
    *   Herkese açık, onaylı ürünleri listeleme (sayfalı, filtreli, sıralı).
    *   Ürün detaylarını ID ve slug ile görüntüleme.
    *   Satıcı: Yeni ürün oluşturma, kendi ürünlerini listeleme/güncelleme/silme, aktiflik durumunu ayarlama, görsel yükleme.
    *   Admin: Tüm ürünleri listeleme, ürün onaylama/reddetme, aktiflik durumunu ayarlama, ürün silme.
4.  **Alışveriş Sepeti:**
    *   Sepeti görüntüleme, ürün ekleme, miktar güncelleme, ürün silme, sepeti temizleme.
5.  **İstek Listesi (Wishlist):**
    *   İstek listesini görüntüleme, ürün ekleme/kaldırma, ürünün listede olup olmadığını kontrol etme.
6.  **Sipariş Yönetimi:**
    *   Müşteri: Sipariş oluşturma (sepetten), kendi siparişlerini listeleme/detay görüntüleme, siparişi iptal etme.
    *   Admin/Satıcı: Siparişleri listeleme (filtreli), sipariş detaylarını görme, sipariş durumunu güncelleme, kargo takip numarası ekleme.
7.  **Yorum Yönetimi:**
    *   Ürün yorumlarını listeleme (onaylı/tümü).
    *   Müşteri: Yorum ekleme, kendi yorumunu silme.
    *   Admin: Tüm yorumları listeleme, yorum onaylama/reddetme, yorum silme.
8.  **Kupon Yönetimi:**
    *   Kupon doğrulama (sepet toplamına göre).
    *   Admin: Kupon oluşturma, listeleme, güncelleme, silme.
9.  **Bildirim Sistemi:**
    *   Kullanıcının kendi bildirimlerini listeleme, okunmamış sayısını görme.
    *   Bildirimleri okundu olarak işaretleme (tekli/tümü), bildirim silme.
    *   Sistem tarafından otomatik bildirim oluşturma (sipariş durumu, kayıt vb.).
10. **Ödeme Sistemi (Stripe Entegrasyonu):**
    *   Sipariş ve abonelikler için Stripe Checkout oturumu oluşturma.
    *   Stripe webhook'larını dinleyerek ödeme durumunu ve siparişleri güncelleme.
11. **Abonelik Sistemi:**
    *   Kullanıcının abonelik durumunu görme.
    *   Abonelik için ödeme oturumu oluşturma.
    *   Süresi dolan abonelikleri periyodik olarak kontrol edip güncelleme.
12. **"Feels" (Kısa Video) Özelliği:**
    *   Satıcı: Feel oluşturma (video/thumbnail yükleme), kendi feel'lerini listeleme/silme.
    *   Herkese açık: Feel listeleme (genel, ürüne göre, satıcıya göre), feel detayını görme (izlenme sayacı ile).
    *   Giriş yapmış kullanıcı: Feel beğenme/beğeniyi geri alma.
    *   Admin: Feel'leri yönetme (aktif/pasif yapma).
13. **AI Özellikleri (Satıcılar İçin):**
    *   Prompt ile ürün görseli üretme/düzenleme (kota kontrolü ile).
    *   Üretilen AI görselini ürün ana görseli olarak ayarlama.
    *   (Gelecekte) Ürün yorumlarından AI ile özet çıkarma.

---

**7. API Dokümantasyonu Özeti**

`apidocs.md` dosyası, backend API endpoint'lerini, istek (Request) ve yanıt (Response) DTO'larını, yetkilendirme kurallarını detaylı bir şekilde açıklamaktadır. Bu doküman, frontend geliştirme için birincil referans kaynağıdır. Başlıca bölümleri şunlardır:

*   Genel Bilgiler (Base URL, Kimlik Doğrulama, Hata Yanıtları)
*   Controller Bazlı Endpoint Grupları:
    *   Auth Controller
    *   User Controller
    *   Category Controller
    *   Product Controller (Public, Seller, Admin)
    *   SellerProductController
    *   Review Controller
    *   Cart Controller
    *   Wishlist Controller
    *   Order Controller
    *   Coupon Controller
    *   Notification Controller
    *   Payment Controller (Stripe entegrasyonu)
    *   Subscription Controller
    *   AdminUserController
    *   FeelController
*   Ek DTO Tanımları

---

**8. Geliştirme Standartları ve En İyi Pratikler (Frontend Odaklı)**

`.clinerules/angular.md` ve `.clinerules/rules.md` dosyalarında belirtilen prensipler projenin kalitesini ve sürdürülebilirliğini artırmayı hedefler:

*   **Modülerlik:** Standalone API tercihi, özellik bazlı klasör yapısı.
*   **Performans:** Lazy loading, Angular Signals, OnPush change detection, `NgOptimizedImage`, `@defer` blokları.
*   **Kod Kalitesi:** Katı TypeScript kullanımı (`any` yok), anlamlı isimlendirme, dosya isimlendirme standartları, SCSS kullanımı, kod temizliği.
*   **Angular Best Practices:** `inject()` fonksiyonu, `async` pipe, `trackBy`, component composition.
*   **Erişilebilirlik:** Semantik HTML ve ARIA.
*   **Güvenlik:** XSS önleme, sanitization.
*   **Test:** AAA paterni ile unit testler.

---

**9. Potansiyel Gelişim Alanları ve Gelecek Çalışmalar**

*   **Kapsamlı Testler:** Unit testlerin yanı sıra entegrasyon ve E2E testlerinin artırılması.
*   **CI/CD Pipeline:** Otomatik build, test ve deployment süreçlerinin kurulması.
*   **Gelişmiş State Management:** Proje büyüdükçe, Angular Signals'a ek olarak veya yerine daha merkezi bir state management (NgRx, Akita) düşünülebilir.
*   **Gerçek Zamanlı Bildirimler:** WebSocket entegrasyonu ile anlık bildirimler.
*   **Detaylı Raporlama:** Admin ve Satıcı panelleri için daha kapsamlı satış, ürün performansı vb. raporları.
*   **Gelişmiş AI Özellikleri:** Yorum analizi, kişiselleştirilmiş ürün önerileri, chatbot entegrasyonu.
*   **Mikroservis Mimarisine Geçiş (Gerekirse):** Proje çok büyürse, belirli modüller (örn: ödeme, bildirim, AI) ayrı mikroservislere dönüştürülebilir.
*   **Mobil Uygulama:** Platform için native veya cross-platform mobil uygulamalar geliştirilebilir.
*   **UI/UX İyileştirmeleri:** Kullanıcı deneyimini artırmak için sürekli iyileştirmeler ve kullanıcı testleri.
*   **Docker ve Konteynerizasyon:** Kolay deployment ve ölçeklenebilirlik için.
*   **Frontend `TODO.md`'deki Maddeler:** Özellikle admin paneli, satıcı paneli ve raporlama gibi henüz tamamlanmamış özelliklerin geliştirilmesi.

---

