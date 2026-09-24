package com.prince.unispot.review.application.service;

import com.prince.unispot.core.exception.ResourceNotFoundException;
import com.prince.unispot.core.security.AuthorizationHelper;
import com.prince.unispot.place.domain.model.Place;
import com.prince.unispot.place.infrastructure.persistence.PlaceRepository;
import com.prince.unispot.review.domain.model.Review;
import com.prince.unispot.review.infrastructure.persistence.ReviewRepository;
import com.prince.unispot.review.presentation.dto.ReviewRequest;
import com.prince.unispot.review.presentation.dto.ReviewResponse;
import com.prince.unispot.user.domain.model.User;
import com.prince.unispot.user.infrastructure.persistence.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final PlaceRepository placeRepository;
    private final UserRepository userRepository;
    private final AuthorizationHelper authorizationHelper;

    @Transactional
    public void addReview(Long placeId, ReviewRequest request) {
        //if not exisst throw 404
        //doing this fires SELECT 1 ... WHERE id = ? LIMIT 1
        //cheaper than SELECT * and can use proxy
        if (!placeRepository.existsById(placeId)) {
            throw new ResourceNotFoundException("Place not found with id: " + placeId);
        }

        Long currentUserId = Long.valueOf(SecurityContextHolder.getContext().getAuthentication().getName());

        //not using findById() , but this jpa repo method, as
        //only need the fk references to save the review
        //so use proxy just holding id in the obj
        Place placeProxy = placeRepository.getReferenceById(placeId);
        User userProxy = userRepository.getReferenceById(currentUserId);

        Review review = Review.builder()
                .place(placeProxy)
                .user(userProxy)
                .rating(request.rating())
                .comment(request.comment())
                .build();

        reviewRepository.save(review);
    }

    @Transactional(readOnly = true)
    public Slice<ReviewResponse> getReviewsForPlace(Long placeId, Pageable pageable) {
        return reviewRepository.findByPlaceId(placeId, pageable).map(ReviewResponse::from);
    }

    @Transactional
    public Long deleteReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + reviewId));

        authorizationHelper.requireOwnerOrAdmin(review.getUser().getId(),
                "You can only delete your own reviews.");

        Long placeId = review.getPlace().getId();
        reviewRepository.delete(review);
        return placeId;
    }
}