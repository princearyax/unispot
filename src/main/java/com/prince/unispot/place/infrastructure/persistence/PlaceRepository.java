package com.prince.unispot.place.infrastructure.persistence;

import com.prince.unispot.place.domain.model.Category;
import com.prince.unispot.place.domain.model.Place;
import com.prince.unispot.place.presentation.dto.PlaceSummaryProjection;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

// @Repository //no need
public interface PlaceRepository extends JpaRepository<Place, Long> {
    
    // Returns a lightweight projection instead of the heavy entity
    Slice<PlaceSummaryProjection> findByCategory(Category category, Pageable pageable);

    // Used for RBAC authorization checks before deletion
    boolean existsByIdAndCreatedBy(Long id, Long createdBy);
}