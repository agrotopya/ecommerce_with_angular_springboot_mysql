package com.fibiyo.ecommerce.infrastructure.web.controller;

import com.fibiyo.ecommerce.application.dto.ApiResponse;
import com.fibiyo.ecommerce.application.dto.OrderRequest;
import com.fibiyo.ecommerce.application.dto.OrderResponse;
import com.fibiyo.ecommerce.application.service.OrderService;
import com.fibiyo.ecommerce.domain.enums.OrderStatus; // Enum import
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import com.fibiyo.ecommerce.application.dto.MonthlySalesDto; // MonthlySalesDto import edildi
import com.fibiyo.ecommerce.domain.entity.User; // User import edildi
import com.fibiyo.ecommerce.application.exception.ResourceNotFoundException; // ResourceNotFoundException import edildi
import com.fibiyo.ecommerce.infrastructure.persistence.repository.UserRepository; // UserRepository import edildi

import java.util.List; // List import edildi


@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    private final OrderService orderService;
    private final UserRepository userRepository; // UserRepository inject edildi

    @Autowired
    public OrderController(OrderService orderService, UserRepository userRepository) {
        this.orderService = orderService;
        this.userRepository = userRepository; // UserRepository inject edildi
    }

    // --- Helper method to get current authenticated user's ID ---
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            // Bu durum @PreAuthorize ile engellenmeli ama yine de kontrol edelim
            throw new IllegalStateException("User not authenticated");
        }
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
        return user.getId();
    }

    // --- Customer Endpoints ---

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody OrderRequest orderRequest) {
        logger.info("POST /api/orders requested");
        OrderResponse createdOrder = orderService.createOrder(orderRequest);
        return new ResponseEntity<>(createdOrder, HttpStatus.CREATED);
    }

    @GetMapping("/my")
    public ResponseEntity<Page<OrderResponse>> getMyOrders(
            @PageableDefault(size = 10, sort = "orderDate", direction = Sort.Direction.DESC) Pageable pageable) {
        logger.info("GET /api/orders/my requested");
        Page<OrderResponse> orders = orderService.findMyOrders(pageable);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/my/{orderId}")
    public ResponseEntity<OrderResponse> getMyOrderDetails(@PathVariable Long orderId) {
        logger.info("GET /api/orders/my/{} requested", orderId);
        OrderResponse order = orderService.findMyOrderById(orderId); // Servis katmanı sahiplik kontrolü yapar
        return ResponseEntity.ok(order);
    }

     @PatchMapping("/my/{orderId}/cancel") // Müşterinin siparişini iptal etmesi
     public ResponseEntity<OrderResponse> cancelMyOrder(@PathVariable Long orderId) {
          logger.warn("PATCH /api/orders/my/{}/cancel requested", orderId);
          OrderResponse cancelledOrder = orderService.cancelMyOrder(orderId);
         return ResponseEntity.ok(cancelledOrder);
     }

    // --- Seller Specific Endpoints ---

    @GetMapping("/seller/my") // YENİ ENDPOINT: /api/orders/seller/my
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<Page<OrderResponse>> getMySellerOrders(
            @PageableDefault(size = 10, sort = "orderDate", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) OrderStatus status) {
        logger.info("GET /api/orders/seller/my requested by SELLER");
        // OrderService'teki mevcut findSellerOrders metodu, giriş yapmış kullanıcı SELLER ise
        // sadece kendi siparişlerini döndürecek şekilde implemente edilmiş olmalı.
        Page<OrderResponse> orders = orderService.findSellerOrders(pageable, status);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/seller/my-sales-summary")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<List<MonthlySalesDto>> getMySellerSalesSummary() {
        Long sellerId = getCurrentUserId();
        logger.info("GET /api/orders/seller/my-sales-summary requested by SELLER ID: {}", sellerId);
        List<MonthlySalesDto> salesSummary = orderService.getMonthlySalesForSeller(sellerId);
        return ResponseEntity.ok(salesSummary);
    }

    // --- Admin/Seller Endpoints ---

    // Not: Bu endpoint'leri /api/admin/orders ve /api/seller/orders altına taşımak daha düzenli olabilir.
    // Şimdilik burada tutalım, PreAuthorize ile ayıralım.

    @GetMapping // Tüm siparişleri listele (Admin)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<OrderResponse>> getAllOrders(
             @PageableDefault(size = 20, sort = "orderDate", direction = Sort.Direction.DESC) Pageable pageable,
             @RequestParam(required = false) Long customerId,
             @RequestParam(required = false) OrderStatus status) {
        logger.info("GET /api/orders requested (Admin)");
         Page<OrderResponse> orders = orderService.findAllOrders(pageable, customerId, status);
        return ResponseEntity.ok(orders);
    }

     @GetMapping("/{orderId}") // ID ile herhangi bir siparişin detayını gör (Admin/Seller)
     @PreAuthorize("hasRole('ADMIN') or @orderSecurity.hasPermission(#orderId, authentication)") 
 // Seller sadece kendi ürünü olanı görmeli - bu kontrol serviste!
     public ResponseEntity<OrderResponse> getOrderDetails(@PathVariable Long orderId) {
         logger.info("GET /api/orders/{} requested (Admin/Seller)", orderId);
         // TODO: Seller'ın sadece kendi ürününü içeren siparişi görme kontrolü serviste eklenmeli!
         OrderResponse order = orderService.findOrderById(orderId);
         return ResponseEntity.ok(order);
     }

     @PatchMapping("/{orderId}/status") // Sipariş durumunu güncelle (Admin/Seller)
     @PreAuthorize("hasRole('ADMIN') or @orderSecurity.hasPermission(#orderId, authentication)") 
     public ResponseEntity<OrderResponse> updateOrderStatus(@PathVariable Long orderId, @RequestParam OrderStatus status) {
         logger.info("PATCH /api/orders/{}/status requested with status: {}", orderId, status);
         // TODO: Seller'ın sadece kendi ürününü içeren siparişin durumunu değiştirebilme kontrolü serviste! YAPTIM

         OrderResponse updatedOrder = orderService.updateOrderStatus(orderId, status);
         return ResponseEntity.ok(updatedOrder);
     }

     @PatchMapping("/{orderId}/tracking") // Kargo takip no ekle (Admin/Seller)
     @PreAuthorize("hasRole('ADMIN') or @orderSecurity.hasPermission(#orderId, authentication)") // bu gelecek
     public ResponseEntity<OrderResponse> addTrackingNumber(
            @PathVariable Long orderId,
            @RequestParam @NotBlank @Size(max = 100) String trackingNumber) {
           logger.info("PATCH /api/orders/{}/tracking requested with number: {}", orderId, trackingNumber);
           // TODO: Seller kontrolü serviste!
            OrderResponse updatedOrder = orderService.addTrackingNumber(orderId, trackingNumber);
           return ResponseEntity.ok(updatedOrder);
       }

       // TODO: Refund endpoint'i (Admin)


}
