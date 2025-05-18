package com.fibiyo.ecommerce.infrastructure.persistence.repository;

import com.fibiyo.ecommerce.domain.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query; // Özel JPQL/SQL için
import org.springframework.stereotype.Repository;

// Page ve Pageable importları eklendi
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findBySlug(String slug);

    // Sadece aktif kategorileri getiren örnek bir custom query (JPQL)
    @Query("SELECT c FROM Category c WHERE c.isActive = true")
    List<Category> findAllActive();

    // Ana kategorileri (parent'ı null olanlar) getiren örnek sorgu
    List<Category> findByParentCategoryIsNull();

    // Belirli bir üst kategoriye ait alt kategorileri getirme
    List<Category> findByParentCategoryId(Long parentId);

    Boolean existsByName(String name); // İsim kontrolü için

    Boolean existsBySlug(String slug); // Slug kontrolü için

    // Admin paneli için isme göre arama ve sayfalama
    Page<Category> findByNameContainingIgnoreCase(String name, Pageable pageable);
}