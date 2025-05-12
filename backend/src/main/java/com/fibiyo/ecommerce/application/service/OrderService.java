package com.fibiyo.ecommerce.application.service;

import com.fibiyo.ecommerce.application.dto.OrderRequest;
import com.fibiyo.ecommerce.application.dto.OrderResponse;
import com.fibiyo.ecommerce.domain.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderService {

    // --- Customer Operations ---
    OrderResponse createOrder(OrderRequest orderRequest);
    Page<OrderResponse> findMyOrders(Pageable pageable);
    OrderResponse findMyOrderById(Long orderId); // Kendi siparişini görme
    OrderResponse cancelMyOrder(Long orderId); // Siparişi iptal etme (belirli durumlarda)

    // --- Admin/Seller Operations ---
    Page<OrderResponse> findAllOrders(Pageable pageable, Long customerId, OrderStatus status /*.. more filters */); // Tüm siparişler (Admin)
    Page<OrderResponse> findSellerOrders(Pageable pageable, OrderStatus status /*.. more filters */); // Satıcının siparişleri (Admin/Seller)
    OrderResponse findOrderById(Long orderId); // ID ile herhangi bir siparişi görme (Admin/Seller)
    OrderResponse updateOrderStatus(Long orderId, OrderStatus newStatus); // Sipariş durumunu güncelle (Admin/Seller)
    OrderResponse addTrackingNumber(Long orderId, String trackingNumber); // Kargo takip no ekle (Admin/Seller)

    // TODO: Refund/Return işlemleri için metodlar eklenecek

}