package com.fibiyo.ecommerce.application.service.impl;

import com.fibiyo.ecommerce.application.dto.request.CreateFeelRequestDto;
import com.fibiyo.ecommerce.application.dto.response.FeelResponseDto;
import com.fibiyo.ecommerce.application.exception.ConflictException; // ConflictException import edildi
import com.fibiyo.ecommerce.application.exception.ResourceNotFoundException;
import com.fibiyo.ecommerce.application.mapper.FeelMapper;
import com.fibiyo.ecommerce.application.service.FeelService;
import com.fibiyo.ecommerce.application.service.StorageService;
import com.fibiyo.ecommerce.domain.entity.Feel;
import com.fibiyo.ecommerce.domain.entity.FeelLike;
import com.fibiyo.ecommerce.domain.entity.Product;
import com.fibiyo.ecommerce.domain.entity.User;
import com.fibiyo.ecommerce.domain.enums.Role;
import com.fibiyo.ecommerce.infrastructure.persistence.repository.FeelLikeRepository;
import com.fibiyo.ecommerce.infrastructure.persistence.repository.FeelRepository;
import com.fibiyo.ecommerce.infrastructure.persistence.repository.ProductRepository;
import com.fibiyo.ecommerce.infrastructure.persistence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.Nullable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional
public class FeelServiceImpl implements FeelService {

    private final FeelRepository feelRepository;
    private final FeelLikeRepository feelLikeRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;
    private final FeelMapper feelMapper;

    @Override
    public FeelResponseDto createFeel(CreateFeelRequestDto dto, User seller, MultipartFile videoFile, @Nullable MultipartFile thumbnailFile) {
        Objects.requireNonNull(seller, "Authenticated seller (currentUser) cannot be null when creating a feel.");
        Objects.requireNonNull(dto.getProductId(), "Product ID cannot be null when creating a feel."); // DTO'da @NotNull var ama burada da kontrol edelim.

        // "Her Ürünün 1 Feeli Olsun" kuralı
        if (feelRepository.existsByProductId(dto.getProductId())) {
            throw new ConflictException("A feel already exists for product ID: " + dto.getProductId());
        }

        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + dto.getProductId()));

        if (product.getSeller() == null || (!product.getSeller().getId().equals(seller.getId()) && !seller.getRole().equals(Role.ADMIN))) {
            throw new AccessDeniedException("You do not have permission to create a feel for this product.");
        }

        String videoUrl = storageService.storeFile(videoFile, "feels/videos", "video");
        String thumbnailUrl = null;
        if (thumbnailFile != null && !thumbnailFile.isEmpty()) {
            thumbnailUrl = storageService.storeFile(thumbnailFile, "feels/thumbnails", "image");
        }

        Feel feel = Feel.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .videoUrl(videoUrl)
                .thumbnailUrl(thumbnailUrl)
                .product(product)
                .seller(seller)
                .active(true)
                .views(0L)
                .likes(0L)
                .build();
        feel = feelRepository.save(feel);
        return feelMapper.toDto(feel);
    }

    @Override
    @Transactional(readOnly = true)
    public FeelResponseDto getFeelById(Long feelId) {
        Feel feel = feelRepository.findById(feelId)
                .orElseThrow(() -> new ResourceNotFoundException("Feel not found with id: " + feelId));
        if (!feel.isActive()) {
            throw new ResourceNotFoundException("Feel not found or is inactive: " + feelId);
        }
        return feelMapper.toDto(feel);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FeelResponseDto> getAllPublicFeels(Pageable pageable) {
        return feelRepository.findAllByActiveTrue(pageable).map(feelMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FeelResponseDto> getFeelsByProduct(Long productId, Pageable pageable) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product not found with id: " + productId);
        }
        return feelRepository.findByProductIdAndActiveTrue(productId, pageable).map(feelMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FeelResponseDto> getFeelsBySeller(Long sellerId, Pageable pageable, boolean activeOnly) {
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller not found with id: " + sellerId));
        if (activeOnly) {
            return feelRepository.findBySellerIdAndActiveTrue(sellerId, pageable).map(feelMapper::toDto);
        } else {
            return feelRepository.findBySellerId(sellerId, pageable).map(feelMapper::toDto);
        }
    }

    @Override
    public void deleteFeel(Long feelId, User currentUser) {
        Feel feel = feelRepository.findById(feelId)
                .orElseThrow(() -> new ResourceNotFoundException("Feel not found with id: " + feelId));

        if (feel.getSeller() == null || (!feel.getSeller().getId().equals(currentUser.getId()) && !currentUser.getRole().equals(Role.ADMIN))) {
            throw new AccessDeniedException("You do not have permission to delete this feel.");
        }

        try {
            if (feel.getVideoUrl() != null) {
                storageService.deleteFile(feel.getVideoUrl());
            }
            if (feel.getThumbnailUrl() != null) {
                storageService.deleteFile(feel.getThumbnailUrl());
            }
        } catch (Exception e) {
            System.err.println("Error deleting feel files: " + e.getMessage());
        }
        feelLikeRepository.findAll().stream()
                .filter(fl -> fl.getFeel() != null && fl.getFeel().getId().equals(feelId))
                .forEach(feelLikeRepository::delete);

        feelRepository.delete(feel);
    }

    @Override
    public void incrementFeelViewCount(Long feelId) {
        Feel feel = feelRepository.findById(feelId)
                .orElseThrow(() -> new ResourceNotFoundException("Feel not found with id: " + feelId));
        if (!feel.isActive()){
            return;
        }
        feel.setViews(feel.getViews() + 1);
        feelRepository.save(feel);
    }

    @Override
    public void likeFeel(Long feelId, User currentUser) {
        Feel feel = feelRepository.findById(feelId)
                .orElseThrow(() -> new ResourceNotFoundException("Feel not found with id: " + feelId));
        if (!feel.isActive()){
             throw new AccessDeniedException("Cannot like an inactive feel.");
        }

        if (feelLikeRepository.findByUserIdAndFeelId(currentUser.getId(), feelId).isPresent()) {
            throw new IllegalStateException("User has already liked this feel.");
        }

        FeelLike feelLike = FeelLike.builder()
                .user(currentUser)
                .feel(feel)
                .build();
        feelLikeRepository.save(feelLike);

        feel.setLikes(feel.getLikes() + 1);
        feelRepository.save(feel);
    }

    @Override
    public void unlikeFeel(Long feelId, User currentUser) {
        Feel feel = feelRepository.findById(feelId)
                .orElseThrow(() -> new ResourceNotFoundException("Feel not found with id: " + feelId));

        FeelLike feelLike = feelLikeRepository.findByUserIdAndFeelId(currentUser.getId(), feelId)
                .orElseThrow(() -> new ResourceNotFoundException("FeelLike record not found for this user and feel."));

        feelLikeRepository.delete(feelLike);

        if (feel.getLikes() > 0) {
            feel.setLikes(feel.getLikes() - 1);
            feelRepository.save(feel);
        }
    }

    @Override
    public FeelResponseDto updateFeelStatusByAdmin(Long feelId, boolean isActive) {
        Feel feel = feelRepository.findById(feelId)
                .orElseThrow(() -> new ResourceNotFoundException("Feel not found with id: " + feelId));
        feel.setActive(isActive);
        feel = feelRepository.save(feel);
        return feelMapper.toDto(feel);
    }
}
