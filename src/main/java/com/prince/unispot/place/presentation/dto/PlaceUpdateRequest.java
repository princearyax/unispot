package com.prince.unispot.place.presentation.dto;

import com.prince.unispot.place.domain.model.Category;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PlaceUpdateRequest(

    @NotBlank(message = "name is required")
    String name,

    @NotBlank(message = "description is required")
    @Size(max = 300, message = "description must be under 300 characters")
    String description,

    @NotNull(message = "category is required")
    Category category,

    @NotNull(message = "version is required")
    Integer version

) {}