package com.prince.unispot.place.application.service;

import com.prince.unispot.core.exception.ResourceNotFoundException;
import com.prince.unispot.core.security.AuthorizationHelper;
import com.prince.unispot.place.domain.model.Place;
import com.prince.unispot.place.infrastructure.persistence.PlaceRepository;
import com.prince.unispot.place.presentation.dto.PlaceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Component
@RequiredArgsConstructor
class PlaceImageTransactionHelper {

    static final int MAX_IMAGES_PER_PLACE = 5; //limiting it

    private final PlaceRepository placeRepository;
    private final AuthorizationHelper authorizationHelper;

    //will do these in phase so that hickariCP dont become bottleneck and dies XD

    /**
     * Phase 1 of upload: fast read-only check (< 2ms).
     * Verifies place exists, caller is owner/admin, and image quota is not exceeded.
     * Transaction closes immediately upon return, freeing the HikariCP connection BEFORE Cloudinary I/O.
     */
    @Transactional(readOnly = true)
    public void verifyCanUploadImage(Long placeId) {
        Place place = placeRepository.findWithImagesById(placeId)
                .orElseThrow(() -> new ResourceNotFoundException("Place not found with id: " + placeId));

        authorizationHelper.requireOwnerOrAdmin(place.getCreatedBy(),
                "You do not have permission to upload images to this place.");

        if (place.getImageUrls().size() >= MAX_IMAGES_PER_PLACE) {
            throw new IllegalArgumentException(
                    "Maximum of " + MAX_IMAGES_PER_PLACE + " images allowed per place.");
        }
    }
    //phase 2 , the actual uploading (as using server proxy-upload, not direct) inside PlaceService upload image

    /**
     * Phase 3 of upload: fast write transaction (< 3ms).
     * Attaches the uploaded URL to the Place entity and flushes.
     */
    //last phase
    @Transactional
    public PlaceResponse attachUploadedImageUrl(Long placeId, String imageUrl) {
        Place place = placeRepository.findWithImagesById(placeId)
                .orElseThrow(() -> new ResourceNotFoundException("Place not found with id: " + placeId));

        authorizationHelper.requireOwnerOrAdmin(place.getCreatedBy(),
                "You do not have permission to upload images to this place.");

        if (place.getImageUrls().size() >= MAX_IMAGES_PER_PLACE) {
            throw new IllegalArgumentException(
                    "Maximum of " + MAX_IMAGES_PER_PLACE + " images allowed per place.");
        }

        place.getImageUrls().add(imageUrl);
        Place saved = placeRepository.saveAndFlush(place);
        return PlaceResponse.from(saved);
    }

    /**
     * Removes a single image URL from the DB inside a short transaction.
     */
    @Transactional
    public PlaceResponse removeImageUrlFromDb(Long placeId, String imageUrl) {
        Place place = placeRepository.findWithImagesById(placeId)
                .orElseThrow(() -> new ResourceNotFoundException("Place not found with id: " + placeId));

        authorizationHelper.requireOwnerOrAdmin(place.getCreatedBy(),
                "You do not have permission to delete images from this place.");

        boolean removed = place.getImageUrls().remove(imageUrl);
        if (!removed) {
            throw new ResourceNotFoundException("Image URL not found on place with id: " + placeId);
        }

        Place saved = placeRepository.saveAndFlush(place);
        return PlaceResponse.from(saved);
    }

    /**
     * Deletes the place and its reviews inside a transaction and returns the image URLs
     * so the caller can purge them from Cloudinary after commit.
     */
    //dlt a place also clears cloudinary things
    @Transactional
    public Set<String> deletePlaceFromDbAndReturnImages(
            Long placeId,
            com.prince.unispot.review.infrastructure.persistence.ReviewRepository reviewRepository) {

        Place place = placeRepository.findWithImagesById(placeId)
                .orElseThrow(() -> new ResourceNotFoundException("Place not found with id: " + placeId));

        authorizationHelper.requireOwnerOrAdmin(place.getCreatedBy(),
                "You do not have permission to delete this place.");

        Set<String> urlsToDelete = Set.copyOf(place.getImageUrls());
        reviewRepository.deleteByPlaceId(placeId);
        placeRepository.delete(place);
        return urlsToDelete;
    }
}