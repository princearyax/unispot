package com.prince.unispot.review.presentation.controller;

import com.prince.unispot.review.application.service.ReviewService;
import com.prince.unispot.review.presentation.dto.ReviewRequest;
import com.prince.unispot.review.presentation.dto.ReviewResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springdoc.core.annotations.ParameterObject;
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
@Tag(name = "3. Reviews & Ratings", description = "Place reviews with AFTER_COMMIT event-driven rating aggregation")
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/place/{placeId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation (
        summary = "Submit a review for a place (1 review per user per place)",
        description = "Triggers asynchronous AFTER_COMMIT rating recalculation with REQUIRES_NEW optimistic lock retries."
    )
    public ResponseEntity<ReviewResponse> addReview(
        @PathVariable Long placeId,
        @Valid @RequestBody ReviewRequest request
    ) {
        //will use observer pattern: review event to do
        ReviewResponse created = reviewService.addReview(placeId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/place/{placeId}")
    @SecurityRequirements()
    @Operation(summary = "Get paginated reviews for a place using Slice (avoids total COUNT query)")
    public ResponseEntity<Slice<ReviewResponse>> getReviews(
        @PathVariable Long placeId,
        @ParameterObject @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(reviewService.getReviewsForPlace(placeId, pageable));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Delete a review (Owner or ADMIN) and recalculate place rating")
    public ResponseEntity<Void> deleteReview(@PathVariable Long id) {
        reviewService.deleteReview(id);
        return ResponseEntity.noContent().build();
    }
}

//@ParameterObject, let swagger know its pageable not some complex object