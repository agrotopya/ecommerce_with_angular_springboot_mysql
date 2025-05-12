package com.fibiyo.ecommerce.infrastructure.persistence.repository;

import com.fibiyo.ecommerce.domain.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor; // Filtreleme için gerekli
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> { // Specification Executor eklendi

    boolean existsBySlug(String slug);
 
    Optional<Product> findBySlug(String slug);

    Optional<Product> findBySku(String sku); // SKU ile bulma

    // Aktif ve onaylanmış ürünleri sayfalama ile getirme (Genel kullanıcı listesi için)
    Page<Product> findByIsActiveTrueAndIsApprovedTrue(Pageable pageable);

    // Belirli bir kategorideki aktif ve onaylanmış ürünler
    Page<Product> findByCategoryIdAndIsActiveTrueAndIsApprovedTrue(Long categoryId, Pageable pageable);

    // Belirli bir satıcının ürünleri
    Page<Product> findBySellerId(Long sellerId, Pageable pageable);

    // Admin onayı bekleyen ürünler (Onaylanmamış ve aktif olanlar?)
    Page<Product> findByIsApprovedFalse(Pageable pageable); // Duruma göre 'findByIsApprovedFalseAndIsActiveTrue' olabilir

}
/*
 * Not: Gelişmiş filtreleme (price < X, rating > Y vb.) JpaSpecificationExecutor ile Specification API kullanılarak servis katmanında yapılacak.
 */