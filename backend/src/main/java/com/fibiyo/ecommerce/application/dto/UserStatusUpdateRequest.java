package com.fibiyo.ecommerce.application.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserStatusUpdateRequest {
    @NotNull(message = "Aktiflik durumu boş olamaz")
    private Boolean isActive;
}