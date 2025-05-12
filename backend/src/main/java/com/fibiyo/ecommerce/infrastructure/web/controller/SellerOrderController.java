package com.fibiyo.ecommerce.infrastructure.web.controller;

import com.fibiyo.ecommerce.application.dto.OrderResponse;
import com.fibiyo.ecommerce.application.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/seller/orders")
public class SellerOrderController {

    private static final Logger logger = LoggerFactory.getLogger(SellerOrderController.class);

    private final OrderService orderService;

    @Autowired
    public SellerOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<Page<OrderResponse>> getMySellerOrders(
            @PageableDefault(size = 10, sort = "orderDate", direction = Sort.Direction.DESC) Pageable pageable) {
        logger.info("GET /api/seller/orders/my requested by seller");
        // TODO: orderService.findMyOrders() metodunun satıcılar için doğru çalıştığından emin olunmalı
        // veya satıcılara özel bir servis metodu (örn: findOrdersForCurrentSeller) çağrılmalı.
        // Şimdilik mevcut metodu kullanıyoruz.
        Page<OrderResponse> orders = orderService.findMyOrders(pageable);
        return ResponseEntity.ok(orders);
    }

    // Diğer satıcıya özel sipariş endpoint'leri (örn: sipariş detayı, durum güncelleme)
    // OrderController'daki @PreAuthorize("@orderSecurity.hasPermission...") gibi yetkilendirmelerle
    // burada da benzer şekilde veya daha spesifik olarak tanımlanabilir.
    // Örneğin, /api/seller/orders/{orderId}/status gibi.
}
