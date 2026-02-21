package com.prince.dto.response;

public record AuthResponse(
    String token,
    String message
) {}
