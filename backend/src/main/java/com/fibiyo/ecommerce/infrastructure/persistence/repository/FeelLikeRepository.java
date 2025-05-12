package com.fibiyo.ecommerce.infrastructure.persistence.repository;

import com.fibiyo.ecommerce.domain.entity.FeelLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FeelLikeRepository extends JpaRepository<FeelLike, Long> {
    Optional<FeelLike> findByUserIdAndFeelId(Long userId, Long feelId);
    void deleteByUserIdAndFeelId(Long userId, Long feelId);
}

