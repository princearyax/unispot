package com.prince.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

//not exposing the actual entity, but using dto
//record  immutable data cariier, final fields, canonical constructor, getter, etc
public record RegisterRequest(
    @NotBlank(message = "email is required")
    @Email(message = "invalid email format")
    String email,

    @NotBlank(message = "password is required")
    @Size(min = 6, message = "password must be atleast 6 chars")
    String password
) {}
