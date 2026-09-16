package com.prince.unispot.review.presentation.dto;

import java.time.Instant;

import com.prince.unispot.review.domain.model.Review;

public record ReviewResponse(
    Long id, Integer rating, String comment,
    Long placeId, Long reviewerId, Instant createdAt
) {
    public static ReviewResponse from(Review review) {
        return new ReviewResponse(
            review.getId(), review.getRating(), review.getComment(),
            review.getPlace().getId(), review.getUser().getId(),
            review.getCreatedAt()
        ); //.getId doesnt trigger db hit, it uses proxy
    }
}