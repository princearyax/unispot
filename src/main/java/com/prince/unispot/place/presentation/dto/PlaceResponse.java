package com.prince.unispot.place.presentation.dto;

import com.prince.unispot.place.domain.model.Category;
import com.prince.unispot.place.domain.model.Place;

import java.time.Instant;
import java.util.Set;

public record PlaceResponse(
    Long id,
    String name,
    String description,
    Category category,
    Integer version,
    Double averageRating, //may optimize later, one int or something and half bits but naah just
    Integer reviewCount,
    Set<String> imageUrls,
    Instant createdAt,
    Instant updatedAt
) {
    public static PlaceResponse from(Place place) {
        return new PlaceResponse(
            place.getId(),
            place.getName(),
            place.getDescription(),
            place.getCategory(),
            place.getVersion(),
            place.getAverageRating(),
            place.getReviewCount(),
            Set.copyOf(place.getImageUrls()), //defensive copy inside transaction avoids LazyInitializationException
            place.getCreatedAt(),
            place.getUpdatedAt()
        );
    }
}