package com.prince.unispot.review.infrastructure.persistence;

public record RatingAggregate(Double average, Long count) {}