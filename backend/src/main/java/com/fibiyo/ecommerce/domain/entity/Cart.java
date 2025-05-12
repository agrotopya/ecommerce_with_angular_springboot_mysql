package com.fibiyo.ecommerce.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*; // İhtiyaç duyulan Lombok importları
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "carts", uniqueConstraints = {
    @UniqueConstraint(columnNames = "user_id", name = "uk_cart_user_id") // Her kullanıcının tek sepeti
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @OneToOne(fetch = FetchType.LAZY) // Kullanıcıyı hemen yükleme
    @JoinColumn(name = "user_id", nullable = false, unique = true) // FK: user_id (unique)
    @ToString.Exclude // Karşılıklı ilişki toString'de sorun çıkarabilir
    @EqualsAndHashCode.Exclude
    private User user;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Sepetteki ürün kalemleri
    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<CartItem> items = new ArrayList<>();




    
    // Yardımcı metodlar
    public void addItem(CartItem item) {
         if(this.items == null) this.items = new ArrayList<>();
         this.items.add(item);
         item.setCart(this); // İlişkiyi kur
    }
    public void removeItem(CartItem item) {
        if(this.items != null) {
            this.items.remove(item);
            item.setCart(null); // İlişkiyi kaldır
        }
    }
}