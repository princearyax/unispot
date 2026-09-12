package com.prince.unispot.place.infrastructure.persistence;

import com.prince.unispot.place.domain.model.Category;
import com.prince.unispot.place.domain.model.Place;
import com.prince.unispot.place.presentation.dto.PlaceSummaryDto;
import com.prince.unispot.place.presentation.dto.PlaceSummaryProjectionExample;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

// @Repository //no need
public interface PlaceRepository extends JpaRepository<Place, Long> {
    
    // Returns a lightweight projection instead of the heavy entity
    //not used tho
    Slice<PlaceSummaryProjectionExample> findByCategory(Category category, Pageable pageable);

    // Used for RBAC authorization checks before deletion
    boolean existsByIdAndCreatedBy(Long id, Long createdBy);

    //JPA automatically parses this method name into:
    // SELECT * FROM places WHERE category = ? AND id < ? ORDER BY id DESC LIMIT ?
    //this need for caching
    List<PlaceSummaryDto> findByCategoryAndIdLessThanOrderByIdDesc(
        Category category, 
        Long lastId, 
        Pageable pageable //passin Pageable just to utilize its LIMIT functionality
    );
}