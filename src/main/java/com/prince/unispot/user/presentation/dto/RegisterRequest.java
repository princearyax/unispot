package com.prince.unispot.user.presentation.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

//immutable , inherently thread safe, private final fields and canonical contructor, only gettr, as the name of fields  
public record RegisterRequest(

    @NotBlank(message = "email is required")
    @Email(message = "provide a valid email")
    String email,

    @NotBlank(message = "password is required")
    @Size(min = 8, message = "must be at least 8 characters")
    String password
    
) {}