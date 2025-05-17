-- =============================================
-- E-Ticaret Platformu Veritabanı Şeması (MySQL 8.0)
-- =============================================


-- =============================================
-- Kullanıcılar ve Roller
-- =============================================
CREATE TABLE `users` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `username` VARCHAR(100) NOT NULL UNIQUE,
  `email` VARCHAR(255) NOT NULL UNIQUE,
  `password_hash` VARCHAR(255) NULL, -- Sosyal medya ile girişte null olabilir
  `first_name` VARCHAR(100) NOT NULL,
  `last_name` VARCHAR(100) NOT NULL,
  `role` ENUM('CUSTOMER', 'SELLER', 'ADMIN') NOT NULL DEFAULT 'CUSTOMER',
  `is_active` BOOLEAN DEFAULT TRUE NOT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,

  -- Sosyal Medya Login Bilgileri
  `auth_provider` ENUM('GOOGLE', 'FACEBOOK', 'GITHUB') NULL, -- Genişletilebilir
  `provider_id` VARCHAR(255) NULL,

  -- Abonelik Bilgileri
  `subscription_type` ENUM('FREE', 'CUSTOMER_PLUS', 'SELLER_PLUS') DEFAULT 'FREE' NOT NULL,
  `subscription_expiry_date` TIMESTAMP NULL,

  -- AI ve Diğer Özellikler
  `loyalty_points` INT DEFAULT 0 NOT NULL,
  `image_gen_quota` INT DEFAULT 3 NOT NULL, -- Satıcılar için AI imaj hakkı

  CONSTRAINT unique_provider_id UNIQUE (`auth_provider`, `provider_id`) -- Aynı provider ile aynı ID'li 2 kişi olamaz
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Kategoriler (Hiyerarşik Yapı)
-- =============================================
CREATE TABLE `categories` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `name` VARCHAR(255) NOT NULL,
  `slug` VARCHAR(255) NOT NULL UNIQUE, -- SEO için URL uyumlu isim
  `description` TEXT NULL,
  `parent_category_id` BIGINT NULL, -- Ana kategori (NULL ise kök kategori)
  `image_url` VARCHAR(1024) NULL,
  `is_active` BOOLEAN DEFAULT TRUE NOT NULL,

  FOREIGN KEY (`parent_category_id`) REFERENCES `categories`(`id`) ON DELETE SET NULL -- Ana kategori silinirse alt kategoriler kök olur
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Ürünler
-- =============================================
CREATE TABLE `products` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `name` VARCHAR(255) NOT NULL,
  `slug` VARCHAR(255) NOT NULL UNIQUE, -- SEO için URL uyumlu isim
  `description` TEXT NULL,
  `price` DECIMAL(12, 2) NOT NULL CHECK (`price` > 0), -- Fiyat pozitif olmalı
  `stock` INT NOT NULL DEFAULT 0 CHECK (`stock` >= 0), -- Stok negatif olamaz
  `sku` VARCHAR(100) UNIQUE NULL, -- Stock Keeping Unit (Opsiyonel)
  `image_url` VARCHAR(1024) NULL, -- Satıcının yüklediği veya varsayılan
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,

  -- İlişkiler
  `category_id` BIGINT NULL,
  `seller_id` BIGINT NOT NULL,
  `main_image_url` VARCHAR(1024) NULL, -- Ana ürün görseli (eski image_url yerine)

  -- Yönetim ve Durum
  `is_approved` BOOLEAN DEFAULT FALSE NOT NULL, -- Admin onayı gerekli mi?
  `is_active` BOOLEAN DEFAULT TRUE NOT NULL, -- Satışta mı?
  `is_deleted` BOOLEAN DEFAULT FALSE NOT NULL, -- Soft delete için eklendi
  `average_rating` DECIMAL(3, 2) DEFAULT 0.00 NOT NULL, -- Yorum ortalaması (Servis veya trigger ile güncellenir)
  `review_count` INT DEFAULT 0 NOT NULL, -- Toplam yorum sayısı

  -- AI Özellikleri
  `review_summary_ai` TEXT NULL, -- Gemini ile oluşturulan özet
  `ai_generated_image_url` VARCHAR(1024) NULL, -- DALL-E/ChatGPT ile oluşturulan görsel

  FOREIGN KEY (`category_id`) REFERENCES `categories`(`id`) ON DELETE SET NULL, -- Kategori silinirse ürün kategorisiz kalır
  FOREIGN KEY (`seller_id`) REFERENCES `users`(`id`) ON DELETE CASCADE -- Satıcı silinirse ürünleri de silinir (VEYA is_active = false yapılır)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Ürün Resimleri (Bir ürüne ait çoklu resim)
-- =============================================
CREATE TABLE `product_images` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `product_id` BIGINT NOT NULL,
  `image_url` VARCHAR(1024) NOT NULL,
  `alt_text` VARCHAR(255) NULL,
  `is_primary` BOOLEAN DEFAULT FALSE NOT NULL, -- Bu resmin ürünün ana/varsayılan resmi olup olmadığını belirtir
  `display_order` INT DEFAULT 0 NOT NULL, -- Resimlerin gösterim sırası
  `uploaded_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

  FOREIGN KEY (`product_id`) REFERENCES `products`(`id`) ON DELETE CASCADE -- Ürün silinirse resimleri de silinir
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- İndirim Kuponları
-- =============================================
CREATE TABLE `coupons` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `code` VARCHAR(50) NOT NULL UNIQUE,
  `description` TEXT NULL,
  `discount_type` ENUM('PERCENTAGE', 'FIXED_AMOUNT') NOT NULL,
  `discount_value` DECIMAL(10, 2) NOT NULL CHECK (`discount_value` > 0),
  `expiry_date` TIMESTAMP NOT NULL,
  `min_purchase_amount` DECIMAL(12, 2) DEFAULT 0.00 NOT NULL,
  `is_active` BOOLEAN DEFAULT TRUE NOT NULL,
  `usage_limit` INT NULL, -- Null ise sınırsız kullanım
  `times_used` INT DEFAULT 0 NOT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Siparişler
-- =============================================
CREATE TABLE `orders` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `customer_id` BIGINT NOT NULL,
  `order_date` TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  `status` ENUM('PENDING_PAYMENT', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED_BY_CUSTOMER', 'CANCELLED_BY_SELLER', 'CANCELLED_BY_ADMIN', 'RETURN_REQUESTED', 'RETURN_APPROVED', 'RETURN_REJECTED') NOT NULL DEFAULT 'PENDING_PAYMENT',
  `total_amount` DECIMAL(12, 2) NOT NULL CHECK (`total_amount` >= 0), -- İndirim sonrası sepetteki ürünlerin toplamı
  `discount_amount` DECIMAL(10, 2) DEFAULT 0.00 NOT NULL,
  `shipping_fee` DECIMAL(10, 2) DEFAULT 0.00 NOT NULL,
  `final_amount` DECIMAL(12, 2) NOT NULL, -- GENERATED ALWAYS AS kaldırıldı, normal sütun oldu
  `shipping_address` JSON NOT NULL, -- { "street": "...", "city": "...", "zipCode": "...", "country": "..." }
  `billing_address` JSON NULL, -- Opsiyonel: Fatura adresi farklıysa
  `payment_method` VARCHAR(50) NULL, -- Ödeme sonrası doldurulur
  `payment_status` ENUM('PENDING', 'COMPLETED', 'FAILED', 'REFUNDED', 'PARTIALLY_REFUNDED') NOT NULL DEFAULT 'PENDING',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,
  `coupon_id` BIGINT NULL, -- Kullanılan kupon
  `tracking_number` VARCHAR(100) NULL, -- Kargo Takip No

  FOREIGN KEY (`customer_id`) REFERENCES `users`(`id`) ON DELETE RESTRICT, -- Müşteri silinirse siparişler kalmalı (ama ilişki kopabilir veya engellenebilir)
  FOREIGN KEY (`coupon_id`) REFERENCES `coupons`(`id`) ON DELETE SET NULL -- Kupon silinirse siparişteki referans kalkar
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Sipariş Kalemleri (Order Items)
-- =============================================
CREATE TABLE `order_items` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `order_id` BIGINT NOT NULL,
  `product_id` BIGINT NULL, -- Ürün silinirse burası null olabilir veya ürün silme engellenmeli
  `quantity` INT NOT NULL CHECK (`quantity` > 0),
  `price_at_purchase` DECIMAL(12, 2) NOT NULL CHECK (`price_at_purchase` >= 0), -- Sipariş anındaki birim fiyat
  `item_total` DECIMAL(14, 2) GENERATED ALWAYS AS (`quantity` * `price_at_purchase`) STORED,

  FOREIGN KEY (`order_id`) REFERENCES `orders`(`id`) ON DELETE CASCADE, -- Sipariş silinirse kalemleri de silinir
  FOREIGN KEY (`product_id`) REFERENCES `products`(`id`) ON DELETE SET NULL -- Ürün silinirse sipariş kaleminde ürün ID'si null olur (Ürün adını/bilgisini başka yerde de tutmak gerekebilir)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Ödemeler
-- =============================================
CREATE TABLE `payments` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `order_id` BIGINT NOT NULL,
  `amount` DECIMAL(12, 2) NOT NULL CHECK (`amount` > 0),
  `payment_date` TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  `payment_method` VARCHAR(50) NOT NULL, -- Örn: 'STRIPE', 'PAYPAL_SANDBOX', 'CREDIT_CARD'
  `transaction_id` VARCHAR(255) NULL UNIQUE, -- Ödeme ağ geçidinden gelen ID
  `status` ENUM('PENDING', 'COMPLETED', 'FAILED', 'REFUNDED') NOT NULL,
  `currency` CHAR(3) DEFAULT 'TRY' NOT NULL, -- Para birimi
  `gateway_response` TEXT NULL, -- Ağ geçidinden gelen detaylı yanıt (debug için)
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,

  FOREIGN KEY (`order_id`) REFERENCES `orders`(`id`) ON DELETE CASCADE -- Sipariş silinirse ilişkili ödemeler de silinir (veya RESTRICT?)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Ürün Yorumları
-- =============================================
CREATE TABLE `reviews` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `product_id` BIGINT NOT NULL,
  `customer_id` BIGINT NOT NULL,
  `order_id` BIGINT NULL, -- Opsiyonel: Yorumun hangi sipariş sonrası yapıldığı (satın almayı doğrulamak için)
  `rating` TINYINT NOT NULL CHECK (`rating` >= 1 AND `rating` <= 5),
  `comment` TEXT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  `is_approved` BOOLEAN DEFAULT TRUE NOT NULL, -- Opsiyonel: Yorum onayı gerekiyorsa

  FOREIGN KEY (`product_id`) REFERENCES `products`(`id`) ON DELETE CASCADE, -- Ürün silinirse yorumları da silinir
  FOREIGN KEY (`customer_id`) REFERENCES `users`(`id`) ON DELETE CASCADE, -- Kullanıcı silinirse yorumları da silinir
  FOREIGN KEY (`order_id`) REFERENCES `orders`(`id`) ON DELETE SET NULL, -- Sipariş silinirse yorumdaki sipariş bağlantısı kalkar
  UNIQUE KEY `unique_review_per_product_customer` (`product_id`, `customer_id`) -- Bir kullanıcı bir ürüne sadece 1 yorum yapabilir
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Bildirimler
-- =============================================
CREATE TABLE `notifications` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `user_id` BIGINT NOT NULL, -- Bildirimin gönderildiği kullanıcı
  `message` TEXT NOT NULL,
  `link` VARCHAR(1024) NULL, -- Bildirimle ilgili bir link (örn: sipariş detayı)
  `is_read` BOOLEAN DEFAULT FALSE NOT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  `type` VARCHAR(50) NULL, -- Bildirim tipi (örn: 'ORDER_STATUS', 'NEW_PRODUCT', 'PROMOTION')

  FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE -- Kullanıcı silinirse bildirimleri de silinir
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- İstek Listesi (Wishlist)
-- =============================================
CREATE TABLE `wishlist_items` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `user_id` BIGINT NOT NULL,
  `product_id` BIGINT NOT NULL,
  `added_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

  FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,
  FOREIGN KEY (`product_id`) REFERENCES `products`(`id`) ON DELETE CASCADE,
  UNIQUE KEY `unique_wishlist_item` (`user_id`, `product_id`) -- Bir kullanıcı bir ürünü listeye 1 kez ekleyebilir
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Performans için Index'ler
-- =============================================
-- Users
CREATE INDEX idx_users_email ON `users`(`email`);
CREATE INDEX idx_users_role ON `users`(`role`);
CREATE INDEX idx_users_subscription_type ON `users`(`subscription_type`);

-- Categories
CREATE INDEX idx_categories_slug ON `categories`(`slug`);
CREATE INDEX idx_categories_parent_id ON `categories`(`parent_category_id`);

-- Products
CREATE INDEX idx_products_slug ON `products`(`slug`);
CREATE INDEX idx_products_category_id ON `products`(`category_id`);
CREATE INDEX idx_products_seller_id ON `products`(`seller_id`);
CREATE INDEX idx_products_is_approved ON `products`(`is_approved`);
CREATE INDEX idx_products_is_active ON `products`(`is_active`);
CREATE INDEX idx_products_price ON `products`(`price`);
CREATE INDEX idx_products_avg_rating ON `products`(`average_rating`);

-- Product Images
CREATE INDEX idx_product_images_product_id ON `product_images`(`product_id`);
CREATE INDEX idx_product_images_is_primary ON `product_images`(`is_primary`);

-- Coupons
CREATE INDEX idx_coupons_code ON `coupons`(`code`);
CREATE INDEX idx_coupons_expiry_date ON `coupons`(`expiry_date`);
CREATE INDEX idx_coupons_is_active ON `coupons`(`is_active`);

-- Orders
CREATE INDEX idx_orders_customer_id ON `orders`(`customer_id`);
CREATE INDEX idx_orders_status ON `orders`(`status`);
CREATE INDEX idx_orders_order_date ON `orders`(`order_date`);

-- Order Items
CREATE INDEX idx_order_items_order_id ON `order_items`(`order_id`);
CREATE INDEX idx_order_items_product_id ON `order_items`(`product_id`);

-- Payments
CREATE INDEX idx_payments_order_id ON `payments`(`order_id`);
CREATE INDEX idx_payments_transaction_id ON `payments`(`transaction_id`);
CREATE INDEX idx_payments_status ON `payments`(`status`);

-- Reviews
CREATE INDEX idx_reviews_product_id ON `reviews`(`product_id`);
CREATE INDEX idx_reviews_customer_id ON `reviews`(`customer_id`);
CREATE INDEX idx_reviews_rating ON `reviews`(`rating`);

-- Notifications
CREATE INDEX idx_notifications_user_id ON `notifications`(`user_id`);
CREATE INDEX idx_notifications_is_read ON `notifications`(`is_read`);

-- Wishlist Items
CREATE INDEX idx_wishlist_items_user_id ON `wishlist_items`(`user_id`);
CREATE INDEX idx_wishlist_items_product_id ON `wishlist_items`(`product_id`);

-- =============================================
-- Alışveriş Sepetleri
-- =============================================
CREATE TABLE `carts` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `user_id` BIGINT NOT NULL UNIQUE, -- Her kullanıcının bir sepeti olur
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,

  FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE -- Kullanıcı silinirse sepeti de silinir
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Sepet Kalemleri (Cart Items)
-- =============================================
CREATE TABLE `cart_items` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `cart_id` BIGINT NOT NULL,
  `product_id` BIGINT NOT NULL,
  `quantity` INT NOT NULL CHECK (`quantity` > 0),
  `added_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL, -- Öğenin sepete eklenme zamanı

  FOREIGN KEY (`cart_id`) REFERENCES `carts`(`id`) ON DELETE CASCADE, -- Sepet silinirse kalemleri de silinir
  FOREIGN KEY (`product_id`) REFERENCES `products`(`id`) ON DELETE CASCADE, -- Ürün silinirse sepetten de kalkmalı mı? (CASCADE mantıklı)
  UNIQUE KEY `uk_cart_item_cart_product` (`cart_id`, `product_id`) -- Aynı sepete aynı üründen bir satır olmalı
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ... (İndexler vb.) ...
-- Yeni Index'ler
CREATE INDEX idx_cart_items_cart_id ON `cart_items`(`cart_id`);
CREATE INDEX idx_cart_items_product_id ON `cart_items`(`product_id`);
