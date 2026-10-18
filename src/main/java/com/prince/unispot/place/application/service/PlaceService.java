package com.prince.unispot.place.application.service;

import com.prince.unispot.core.application.storage.ImageStorageService;
import com.prince.unispot.core.exception.ResourceNotFoundException;
import com.prince.unispot.core.security.AuthorizationHelper;
import com.prince.unispot.place.domain.model.Category;
import com.prince.unispot.place.domain.model.Place;
import com.prince.unispot.place.infrastructure.persistence.PlaceRepository;
import com.prince.unispot.place.presentation.dto.CursorPageResponse;
import com.prince.unispot.place.presentation.dto.PlaceRequest;
import com.prince.unispot.place.presentation.dto.PlaceResponse;
import com.prince.unispot.place.presentation.dto.PlaceSummaryDto;
import com.prince.unispot.place.presentation.dto.PlaceUpdateRequest;
import com.prince.unispot.review.infrastructure.persistence.ReviewRepository;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Set;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class PlaceService {

    private static final int MAX_RATING_RETRY_ATTEMPTS = 3;

    private final PlaceRatingUpdater placeRatingUpdater;
    private final PlaceRepository placeRepository;
    private final ReviewRepository reviewRepository;
    private final AuthorizationHelper authorizationHelper;
    private final PlaceImageTransactionHelper placeImageTxHelper;
    private final ImageStorageService imageStorageService;

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
        Place place = placeRepository.findWithImagesById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Place not found with id: " + id));
        return PlaceResponse.from(place);
    }

    // @Transactional(readOnly = true)
    // public Slice<PlaceSummaryProjection> getPlacesByCategory(Category category, Pageable pageable) {
    //     return placeRepository.findByCategory(category, pageable);
    // }

    //update for keyset pagination
    @Transactional(readOnly = true)
    public CursorPageResponse<PlaceSummaryDto> getPlacesByCursor(Category category, Long cursor, int size) {
        int safeSize = Math.clamp(size, 1, 100);
        // fetch safeSize + 1 to detect if another page exists without a COUNT query, another one
        List<PlaceSummaryDto> fetched = placeRepository.findByCategoryAndIdLessThanOrderByIdDesc(
                category,
                cursor != null ? cursor : Long.MAX_VALUE,
                org.springframework.data.domain.PageRequest.of(0, safeSize + 1)
        );

        boolean hasNext = fetched.size() > safeSize;
        List<PlaceSummaryDto> items = hasNext ? fetched.subList(0, safeSize) : fetched;
        Long nextCursor = hasNext ? items.getLast().id() : null;

        return new CursorPageResponse<>(items, nextCursor, hasNext);
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

    //the orchestrator: intentionally no @Transactional
    //pre-check of db few ms read
    //this one upload on VT no hickariCP use
    //3.save url in phase 3 in helper
    public PlaceResponse uploadImage(Long placeId, MultipartFile file) {
        //1
        placeImageTxHelper.verifyCanUploadImage(placeId);

        //2
        String uploadedUrl = imageStorageService.uploadPlaceImage(placeId, file);

        // 3: fast DB write with compensating rollback on failure
        try {
            return placeImageTxHelper.attachUploadedImageUrl(placeId, uploadedUrl);
        } catch (RuntimeException ex) {
            imageStorageService.deleteImageByUrl(uploadedUrl);
            throw ex;
        }
    }

    /**
     * Intentionally NOT @Transactional at the orchestrator level:
     * Removes URL from DB first, then deletes asset from Cloudinary after DB commit succeeds.
     */
    public PlaceResponse deleteImage(Long placeId, String imageUrl) {
        PlaceResponse updated = placeImageTxHelper.removeImageUrlFromDb(placeId, imageUrl);
        imageStorageService.deleteImageByUrl(imageUrl);
        return updated;
    }

    /**
     * Deletes Place and Reviews in a DB transaction, then cleans up all associated Cloudinary images.
     */
    public void deletePlace(Long placeId) {
        Set<String> urlsToDelete = placeImageTxHelper.deletePlaceFromDbAndReturnImages(placeId, reviewRepository); //will handle bulk dlt review too
        for (String url : urlsToDelete) {
            imageStorageService.deleteImageByUrl(url);
        } 
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

    @ org.springframework.transaction.event.TransactionalEventListener(
        phase = org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT
    )
    public void onReviewChanged(com.prince.unispot.core.domain.ReviewChangedEvent event) {
        recalculateRating(event.placeId());
    }

}