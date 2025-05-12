package com.fibiyo.ecommerce.application.service;

import com.fibiyo.ecommerce.application.dto.request.CreateFeelRequestDto;
import com.fibiyo.ecommerce.application.dto.response.FeelResponseDto;
import com.fibiyo.ecommerce.domain.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.Nullable;
import org.springframework.web.multipart.MultipartFile;

public interface FeelService {

    FeelResponseDto createFeel(CreateFeelRequestDto dto, User seller, MultipartFile videoFile, @Nullable MultipartFile thumbnailFile);

    FeelResponseDto getFeelById(Long feelId);

    Page<FeelResponseDto> getAllPublicFeels(Pageable pageable);

    Page<FeelResponseDto> getFeelsByProduct(Long productId, Pageable pageable);

    Page<FeelResponseDto> getFeelsBySeller(Long sellerId, Pageable pageable, boolean activeOnly);

    void deleteFeel(Long feelId, User currentUser);

    void incrementFeelViewCount(Long feelId);

    void likeFeel(Long feelId, User currentUser);

    void unlikeFeel(Long feelId, User currentUser);

    FeelResponseDto updateFeelStatusByAdmin(Long feelId, boolean isActive);
}

