package com.prince.unispot.review.presentation.dto;

public record ReviewRequest(
    Integer rating,
    String comment
) {}