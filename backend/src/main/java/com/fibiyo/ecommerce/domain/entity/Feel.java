package com.fibiyo.ecommerce.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "feels")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Feel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 255)
    private String title;

    @Lob
    private String description;

    @NotBlank
    @Column(nullable = false, length = 1024)
    private String videoUrl;

    @Column(length = 1024)
    private String thumbnailUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false, columnDefinition = "BIGINT default 0")
    private Long views = 0L;

    @Column(nullable = false, columnDefinition = "BIGINT default 0")
    private Long likes = 0L;

    @Column(name = "is_active", nullable = false, columnDefinition = "BOOLEAN default true") // Veritabanı sütun adını koru
    private boolean active = true; // Alan adını "active" olarak değiştir
}
