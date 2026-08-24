package com.prince.unispot.user.presentation.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(

    @NotBlank(message = "provide an email")
    @Email(message = "doesn't seem valid")
    String email,

    @NotBlank(message = "fill in password")
    String password

) {}