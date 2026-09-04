package com.prince.unispot.review.infrastructure.persistence;


import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.prince.unispot.review.domain.model.Review;

public interface ReviewRepository extends JpaRepository<Review, Long>{
    Slice<Review> findByPlaceId(Long placeId, Pageable pageable);

    @Modifying // so that executeUpdate not executeQuery
    @Query("DELETE FROM Review r WHERE r.place.id = :placeId")
    void deleteByPlaceId(@Param("placeId") Long placeId);
} 
