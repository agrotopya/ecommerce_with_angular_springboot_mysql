package com.fibiyo.ecommerce.infrastructure.web.controller;

import com.fibiyo.ecommerce.application.dto.request.CreateFeelRequestDto;
import com.fibiyo.ecommerce.application.dto.ApiResponse;
import com.fibiyo.ecommerce.application.dto.response.FeelResponseDto;
import com.fibiyo.ecommerce.application.service.FeelService;
import com.fibiyo.ecommerce.domain.entity.User;
import com.fibiyo.ecommerce.infrastructure.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/feels")
@RequiredArgsConstructor
public class FeelController {

    private final FeelService feelService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole(\'SELLER\')")
    public ResponseEntity<FeelResponseDto> createFeel(@Valid @RequestPart("dto") CreateFeelRequestDto dto,
                                                      @RequestPart("videoFile") MultipartFile videoFile,
                                                      @RequestPart(name = "thumbnailFile", required = false) @Nullable MultipartFile thumbnailFile,
                                                      @CurrentUser User currentUser) {
        FeelResponseDto createdFeel = feelService.createFeel(dto, currentUser, videoFile, thumbnailFile);
        return new ResponseEntity<>(createdFeel, HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize("permitAll()")
    public ResponseEntity<Page<FeelResponseDto>> getAllPublicFeels(Pageable pageable) {
        Page<FeelResponseDto> feels = feelService.getAllPublicFeels(pageable);
        return ResponseEntity.ok(feels);
    }

    @GetMapping("/{feelId}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<FeelResponseDto> getFeelById(@PathVariable Long feelId) {
        // Increment view count when a feel is fetched by ID
        // This could be done asynchronously or in a separate call if performance is critical
        try {
            feelService.incrementFeelViewCount(feelId);
        } catch (Exception e) {
            // Log error or handle if view count increment fails, but still return the feel if found
            System.err.println("Failed to increment view count for feel: " + feelId + " - " + e.getMessage());
        }
        FeelResponseDto feel = feelService.getFeelById(feelId);
        return ResponseEntity.ok(feel);
    }

    @GetMapping("/product/{productId}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<Page<FeelResponseDto>> getFeelsByProduct(@PathVariable Long productId, Pageable pageable) {
        Page<FeelResponseDto> feels = feelService.getFeelsByProduct(productId, pageable);
        return ResponseEntity.ok(feels);
    }

    @GetMapping("/seller/{sellerId}")
    @PreAuthorize("permitAll()") // Publicly viewable active feels for a seller
    public ResponseEntity<Page<FeelResponseDto>> getFeelsBySeller(@PathVariable Long sellerId, Pageable pageable) {
        Page<FeelResponseDto> feels = feelService.getFeelsBySeller(sellerId, pageable, true);
        return ResponseEntity.ok(feels);
    }

    @GetMapping("/seller/my")
    @PreAuthorize("hasRole(\'SELLER\')")
    public ResponseEntity<Page<FeelResponseDto>> getCurrentSellerFeels(Pageable pageable, @CurrentUser User currentUser) {
        Page<FeelResponseDto> feels = feelService.getFeelsBySeller(currentUser.getId(), pageable, false); // false to get active & inactive
        return ResponseEntity.ok(feels);
    }

    @DeleteMapping("/{feelId}")
    @PreAuthorize("hasRole(\'SELLER\') or hasRole(\'ADMIN\')")
    public ResponseEntity<ApiResponse> deleteFeel(@PathVariable Long feelId, @CurrentUser User currentUser) {
        feelService.deleteFeel(feelId, currentUser);
        return ResponseEntity.ok(new ApiResponse(true, "Feel deleted successfully."));
    }

    @PostMapping("/{feelId}/like")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse> likeFeel(@PathVariable Long feelId, @CurrentUser User currentUser) {
        feelService.likeFeel(feelId, currentUser);
        return ResponseEntity.ok(new ApiResponse(true, "Feel liked successfully."));
    }

    @DeleteMapping("/{feelId}/like")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse> unlikeFeel(@PathVariable Long feelId, @CurrentUser User currentUser) {
        feelService.unlikeFeel(feelId, currentUser);
        return ResponseEntity.ok(new ApiResponse(true, "Feel unliked successfully."));
    }

    @PatchMapping("/admin/{feelId}/status")
    @PreAuthorize("hasRole(\'ADMIN\')")
    public ResponseEntity<FeelResponseDto> updateFeelStatusByAdmin(@PathVariable Long feelId, @RequestParam boolean isActive) {
        FeelResponseDto updatedFeel = feelService.updateFeelStatusByAdmin(feelId, isActive);
        return ResponseEntity.ok(updatedFeel);
    }
}

