package com.prince.unispot.place.infrastructure.persistence;

import com.prince.unispot.place.domain.model.Category;
import com.prince.unispot.place.domain.model.Place;
import com.prince.unispot.place.presentation.dto.PlaceSummaryDto;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// @Repository //no need
public interface PlaceRepository extends JpaRepository<Place, Long> {
    
    //eagerly fetch image urls
    @EntityGraph(attributePaths = "imageUrls") //for lazy fetch excep
    Optional<Place> findWithImagesById(Long id);

    //JPA automatically parses this method name into:
    //this need for caching
    //as using custom query no need his name, but anyways.
    @Query("""
        SELECT new com.prince.unispot.place.presentation.dto.PlaceSummaryDto(p.id, p.name, p.category, p.averageRating, p.reviewCount)
        FROM Place p 
        WHERE p.category = :category AND p.id < :lastId 
        ORDER BY p.id DESC
    """)
    List<PlaceSummaryDto> findByCategoryAndIdLessThanOrderByIdDesc(
        @Param("category") Category category, 
        @Param("lastId") Long lastId,
        Pageable pageable //passin Pageable just to utilize its LIMIT functionality, and other depending on the db
    );
}