package com.fibiyo.ecommerce.application.dto;

import com.fibiyo.ecommerce.domain.enums.Role; // Role enum import
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserRoleUpdateRequest {
    @NotNull(message = "Rol boş olamaz")
    private Role role;
}