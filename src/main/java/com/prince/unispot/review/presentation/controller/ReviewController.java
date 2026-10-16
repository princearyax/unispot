package com.prince.unispot.review.presentation.controller;

import com.prince.unispot.review.application.service.ReviewService;
import com.prince.unispot.review.presentation.dto.ReviewRequest;
import com.prince.unispot.review.presentation.dto.ReviewResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Sort;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/place/{placeId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ReviewResponse> addReview(
        @PathVariable Long placeId,
        @Valid @RequestBody ReviewRequest request
    ) {
        //will use observer pattern: review event to do
        ReviewResponse created = reviewService.addReview(placeId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/place/{placeId}")
    public ResponseEntity<Slice<ReviewResponse>> getReviews(
        @PathVariable Long placeId,
        @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(reviewService.getReviewsForPlace(placeId, pageable));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Void> deleteReview(@PathVariable Long id) {
        reviewService.deleteReview(id);
        return ResponseEntity.noContent().build();
    }
}