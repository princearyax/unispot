package com.prince.unispot.place.presentation.dto;

import com.prince.unispot.place.domain.model.Category;

public record PlaceRequest(
    String name,
    String description,
    Category category
) {}