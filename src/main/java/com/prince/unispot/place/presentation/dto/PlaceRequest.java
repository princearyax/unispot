package com.prince.unispot.place.presentation.dto;

import com.prince.unispot.place.domain.model.Category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PlaceRequest(

    @NotBlank(message = "name is required")
    String name,

    @NotBlank(message = "description is required")
    @Size(max = 300, message = "description must be under 300 characters") //same as in entity
    String description,

    @NotNull(message = "category is required")
    Category category
    
) {}