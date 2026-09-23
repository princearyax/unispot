package com.prince.unispot.place.application.service;

import com.prince.unispot.core.exception.ResourceNotFoundException;
import com.prince.unispot.core.security.AuthorizationHelper;
import com.prince.unispot.place.domain.model.Category;
import com.prince.unispot.place.domain.model.Place;
import com.prince.unispot.place.infrastructure.persistence.PlaceRepository;
import com.prince.unispot.place.presentation.dto.PlaceRequest;
import com.prince.unispot.place.presentation.dto.PlaceResponse;
import com.prince.unispot.place.presentation.dto.PlaceSummaryDto;
import com.prince.unispot.place.presentation.dto.PlaceUpdateRequest;
import com.prince.unispot.review.infrastructure.persistence.ReviewRepository;

import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
// import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlaceService {

    private static final int MAX_RATING_RETRY_ATTEMPTS = 3;

    private final PlaceRatingUpdater placeRatingUpdater;
    private final PlaceRepository placeRepository;
    private final ReviewRepository reviewRepository;
    private final AuthorizationHelper authorizationHelper;

    @Transactional
    public PlaceResponse createPlace(PlaceRequest request) {
        Place place = Place.builder()
                .name(request.name())
                .description(request.description())
                .category(request.category())
                .build();
        //createdBy is automatically populated by our JPA Auditing configuration
        Place saved = placeRepository.save(place);
        return PlaceResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public PlaceResponse getPlace(Long id) {
        Place place = placeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Place not found with id: " + id));
        return PlaceResponse.from(place);
    }

    // @Transactional(readOnly = true)
    // public Slice<PlaceSummaryProjection> getPlacesByCategory(Category category, Pageable pageable) {
    //     return placeRepository.findByCategory(category, pageable);
    // }

    //updated for keyset pagination
    @Transactional(readOnly = true)
    public List<PlaceSummaryDto> getPlacesByCursor(Category category, Long cursor, Pageable pageable) {
        return placeRepository.findByCategoryAndIdLessThanOrderByIdDesc(category, cursor, pageable);
    }

    @Transactional
    public PlaceResponse updatePlace(Long id, PlaceUpdateRequest request) {
        Place place = placeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Place not found with id: " + id));

        authorizationHelper.requireOwnerOrAdmin(place.getCreatedBy(),
                "You do not have permission to modify this place.");

        if (!place.getVersion().equals(request.version())) {
            throw new ObjectOptimisticLockingFailureException(Place.class, id);
        }

        place.setName(request.name());
        place.setDescription(request.description());
        place.setCategory(request.category());

        //flushing so get new version
        Place saved = placeRepository.saveAndFlush(place);
        return PlaceResponse.from(saved);
    }

    @Transactional
    public void deletePlace(Long placeId) {
        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new ResourceNotFoundException("Place not found with id: " + placeId));

        authorizationHelper.requireOwnerOrAdmin(place.getCreatedBy(),
                "You do not have permission to delete this place.");

        //Bulk delete reviews directly in DB (1 query, 0 memory overhead), not relying on cascade removal
        reviewRepository.deleteByPlaceId(placeId);
        placeRepository.delete(place);
    }

    //call after review write committed
    //no transactional , just orchestrator
    public void recalculateRating(Long placeId) {
        for (int attempt = 1; attempt <= MAX_RATING_RETRY_ATTEMPTS; attempt++) {
            try {
                placeRatingUpdater.applyLatestAggregate(placeId);
                return;
            } catch (ObjectOptimisticLockingFailureException ex) {
                if (attempt == MAX_RATING_RETRY_ATTEMPTS) {
                    throw ex; // surfaces as 409 via GlobalExceptionHandler
                }
                // next iteration calls into a brand-new REQUIRES_NEW transaction so it re-reads the row fresh instead of retrying the same already-conflicted instance
            }
        }
    }

}