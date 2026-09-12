package com.prince.unispot.place.presentation.dto;

import com.prince.unispot.place.domain.model.Category;
import java.io.Serializable;

//records are natively supported by Jackson for JSON deserialization, unlike projection
//can be seralised need to use while caching
public record PlaceSummaryDto(
    Long id,
    String name,
    Category category
) implements Serializable {}