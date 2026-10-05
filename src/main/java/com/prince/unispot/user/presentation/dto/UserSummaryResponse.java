package com.prince.unispot.user.presentation.dto;

import com.prince.unispot.user.domain.model.Role;
import com.prince.unispot.user.domain.model.User;

public record UserSummaryResponse(Long id, String email, Role role) {
    public static UserSummaryResponse from(User user) {
        return new UserSummaryResponse(user.getId(), user.getEmail(), user.getRole());
    }
}