package com.prince.dto.request;

import java.util.Set;

import com.prince.model.enums.Category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PlaceRequest(
    @NotBlank(message = "name is required")
    String name,

    @NotBlank(message = "description is required")
    String description,

    @NotNull(message = "category is required")
    Category category,

    @NotBlank(message = "location is required")
    String location,

    //frontend feature, optional links for images
    Set<String> images
) {}
