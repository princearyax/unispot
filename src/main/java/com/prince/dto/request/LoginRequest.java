package com.prince.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest (
    @NotBlank(message = "Email should not be blank")
    @Email(message = "Invalid email format")
    String email,

    @NotBlank(message = "Password is required")
    String password
){}
