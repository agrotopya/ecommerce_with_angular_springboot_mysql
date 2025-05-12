package com.fibiyo.ecommerce.infrastructure.persistence.repository;

import com.fibiyo.ecommerce.domain.entity.Payment;
import com.fibiyo.ecommerce.domain.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByOrderId(Long orderId);

    Optional<Payment> findByTransactionId(String transactionId);

    List<Payment> findByOrderIdAndStatus(Long orderId, PaymentStatus status);
}