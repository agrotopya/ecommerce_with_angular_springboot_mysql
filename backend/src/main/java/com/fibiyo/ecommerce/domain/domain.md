

entity: Veritabanı tablolarına karşılık gelen Java sınıfları (JPA Entity'ler).

Entity'ler (domain/entity):
User.java
Category.java
Product.java
Coupon.java
Order.java
OrderItem.java
Payment.java
Review.java
Notification.java
WishlistItem.java
Address.java (Opsiyonel - Eğer adresleri ayrı bir Embeddable/Entity olarak tutmak istersen JSON yerine)


enums: OrderStatus, Role, PaymentStatus, DiscountType, SubscriptionType, AuthProvider gibi Enum sınıfları.

Enum'lar (domain/enums):
Role.java
AuthProvider.java
SubscriptionType.java
DiscountType.java
OrderStatus.java
PaymentStatus.java
NotificationType.java (Opsiyonel)