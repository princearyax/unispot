package com.prince.unispot.user.presentation.dto;

import com.prince.unispot.user.domain.model.Role;
import jakarta.validation.constraints.NotNull;

public record UpdateUserRoleRequest(
    @NotNull(message = "role is required")
    Role role
) {}