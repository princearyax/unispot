package com.prince.dto.response;

//object to send jwt
public record AuthResponse(
    String token,
    String message
) {}
