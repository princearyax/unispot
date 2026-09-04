package com.prince.unispot.place.application.service;

import com.prince.unispot.place.domain.model.Category;
import com.prince.unispot.place.domain.model.Place;
import com.prince.unispot.place.infrastructure.persistence.PlaceRepository;
import com.prince.unispot.place.presentation.dto.PlaceRequest;
import com.prince.unispot.place.presentation.dto.PlaceSummaryProjection;
import com.prince.unispot.review.infrastructure.persistence.ReviewRepository;

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
public class PlaceService {

    private final PlaceRepository placeRepository;
    private final ReviewRepository reviewRepository;

    @Transactional
    public Place createPlace(PlaceRequest request) {
        Place place = Place.builder()
                .name(request.name())
                .description(request.description())
                .category(request.category())
                .build();
        //createdBy is automatically populated by our JPA Auditing configuration
        return placeRepository.save(place);
    }

    @Transactional(readOnly = true)
    public Slice<PlaceSummaryProjection> getPlacesByCategory(Category category, Pageable pageable) {
        return placeRepository.findByCategory(category, pageable);
    }

    @Transactional
    public void deletePlace(Long placeId) {
        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new IllegalArgumentException("Place not found"));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long currentUserId = Long.valueOf(auth.getName());
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        //our rbac check: admin or the original creator can delete
        if (!isAdmin && !place.getCreatedBy().equals(currentUserId)) {
            throw new AccessDeniedException("You do not have permission to delete this place.");
        }

        //Bulk delete reviews directly in DB (1 query, 0 memory overhead), not relying on cascade removal
        reviewRepository.deleteByPlaceId(placeId);
    
        placeRepository.delete(place);
    }
}