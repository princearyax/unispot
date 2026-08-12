package com.prince.unispot.user.presentation.dto;

public record LoginRequest(
    String email,
    String password
) {}