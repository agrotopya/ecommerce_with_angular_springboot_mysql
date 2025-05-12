package com.fibiyo.ecommerce.application.dto;


import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ReviewResponse {
    private Long id;
    private Byte rating;
    private String comment;
    private LocalDateTime createdAt;
    private boolean isApproved; // Onay durumu

    // İlişkili Bilgiler
    private Long productId;
    private String productName; // Kolaylık için

    private Long customerId;
    private String customerUsername; // Kolaylık için

    // private Long orderId; // Gerekirse eklenebilir
}