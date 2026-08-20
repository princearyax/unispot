package com.prince.unispot.review.application.service;

import com.prince.unispot.place.domain.model.Place;
import com.prince.unispot.place.infrastructure.persistence.PlaceRepository;
import com.prince.unispot.review.domain.model.Review;
import com.prince.unispot.review.infrastructure.persistence.ReviewRepository;
import com.prince.unispot.review.presentation.dto.ReviewRequest;
import com.prince.unispot.user.domain.model.User;
import com.prince.unispot.user.infrastructure.persistence.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final PlaceRepository placeRepository;
    private final UserRepository userRepository;

    @Transactional
    public void addReview(Long placeId, ReviewRequest request) {
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
    public Slice<Review> getReviewsForPlace(Long placeId, Pageable pageable) {
        return reviewRepository.findByPlaceId(placeId, pageable);
    }

    @Transactional
    public void deleteReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Review not found"));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long currentUserId = Long.valueOf(auth.getName());
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        //rbac 
        if (!isAdmin && !review.getUser().getId().equals(currentUserId)) {
            throw new AccessDeniedException("You can only delete your own reviews.");
        }

        reviewRepository.delete(review);
    }
}