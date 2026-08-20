package com.prince.unispot.review.infrastructure.persistence;


import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

import com.prince.unispot.review.domain.model.Review;

public interface ReviewRepository extends JpaRepository<Review, Long>{
    Slice<Review> findByPlaceId(Long placeId, Pageable pageable);
} 
