package com.prince.unispot.user.presentation.dto;

public record AuthResponse(
    String accessToken,
    String role
) {}