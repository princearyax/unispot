package com.prince.unispot.user.application.service;

public record AuthResult(
    String accessToken,
    String refreshToken,
    String role
) {}