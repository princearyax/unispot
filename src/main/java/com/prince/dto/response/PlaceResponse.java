package com.prince.dto.response;

import java.time.LocalDateTime;
import java.util.Set;

import com.prince.model.enums.Category;

// send this to the frontend
public record PlaceResponse(
    Long id,
    String name,
    String description,
    Category category,
    String location,
    Set<String> images,
    String ownerName, // just user name
    LocalDateTime createdAt
) {}