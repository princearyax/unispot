package com.prince.unispot.review.presentation.controller;

import com.prince.unispot.place.application.service.PlaceService;
import com.prince.unispot.review.application.service.ReviewService;
import com.prince.unispot.review.presentation.dto.ReviewRequest;
import com.prince.unispot.review.presentation.dto.ReviewResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    private final PlaceService placeService;

    @PostMapping("/place/{placeId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Void> addReview(@PathVariable Long placeId, @Valid @RequestBody ReviewRequest request) {
        //two separate sequential calls on purpose so: recalculateRating
        //must not run inside addReview's own transaction.
        reviewService.addReview(placeId, request);
        placeService.recalculateRating(placeId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/place/{placeId}")
    public ResponseEntity<Slice<ReviewResponse>> getReviews(@PathVariable Long placeId, Pageable pageable) {
        return ResponseEntity.ok(reviewService.getReviewsForPlace(placeId, pageable));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Void> deleteReview(@PathVariable Long id) {
        Long placeId = reviewService.deleteReview(id);
        placeService.recalculateRating(placeId); //recalc after dlt
        return ResponseEntity.noContent().build();
    }
}