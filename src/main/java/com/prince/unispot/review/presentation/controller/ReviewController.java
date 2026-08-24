package com.prince.unispot.review.presentation.controller;

import com.prince.unispot.review.application.service.ReviewService;
import com.prince.unispot.review.domain.model.Review;
import com.prince.unispot.review.presentation.dto.ReviewRequest;

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

    @PostMapping("/place/{placeId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Void> addReview(@Valid @PathVariable Long placeId, @RequestBody ReviewRequest request) {
        reviewService.addReview(placeId, request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/place/{placeId}")
    public ResponseEntity<Slice<Review>> getReviews(@PathVariable Long placeId, Pageable pageable) {
        return ResponseEntity.ok(reviewService.getReviewsForPlace(placeId, pageable));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Void> deleteReview(@PathVariable Long id) {
        reviewService.deleteReview(id);
        return ResponseEntity.noContent().build();
    }
}