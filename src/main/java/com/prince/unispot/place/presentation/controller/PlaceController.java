package com.prince.unispot.place.presentation.controller;

import com.prince.unispot.place.application.service.PlaceService;
import com.prince.unispot.place.domain.model.Category;
import com.prince.unispot.place.presentation.dto.CursorPageResponse;
import com.prince.unispot.place.presentation.dto.PlaceRequest;
import com.prince.unispot.place.presentation.dto.PlaceResponse;
import com.prince.unispot.place.presentation.dto.PlaceSummaryDto;
import com.prince.unispot.place.presentation.dto.PlaceUpdateRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.net.URI;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/places")
@RequiredArgsConstructor
@Tag(name = "2. Campus Places", description = "CRUD, Keyset Pagination, Optimistic Locking (@Version), & Cloudinary Media")
public class PlaceController {

    private final PlaceService placeService;

    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Create a new campus place (populates createdBy via JPA Auditing)")
    public ResponseEntity<PlaceResponse> createPlace(@Valid @RequestBody PlaceRequest request) {
        PlaceResponse created = placeService.createPlace(request);
        return ResponseEntity.created(URI.create("/api/v1/places/" + created.id())).body(created);
    }

    // //spring automatically creates pageable from query params(size, sort, page)
    // @GetMapping
    // public ResponseEntity<Slice<PlaceSummaryProjection>> getPlaces(
    //         @RequestParam Category category, 
    //         @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
    //     //if no size is provided, it defaults to 20, and sorts by latest.
    //     return ResponseEntity.ok(placeService.getPlacesByCategory(category, pageable));
    // }

    @GetMapping("/{id}")
    @SecurityRequirements()
    @Operation(summary = "Get full details of a place by ID including eager-fetched image URLs")
    public ResponseEntity<PlaceResponse> getPlace(@PathVariable Long id) {
        return ResponseEntity.ok(placeService.getPlace(id));
    }

    //optimise pagination by cursor
    @GetMapping("/cursor")
    @SecurityRequirements() //empty means no lock icons needed
    @Operation(
        summary = "Fetch places by category using O(log N) Keyset (Cursor) Pagination",
        description = "Pass lastId from the previous page's nextCursor field. Omit lastId on the first page."
    )
    public ResponseEntity<CursorPageResponse<PlaceSummaryDto>> getPlacesByCursor(
            @RequestParam Category category,

            @Parameter(description = "Cursor ID from previous page (omit for first page)")
            @RequestParam(required = false) Long lastId,

            @Parameter(description = "Items per page (1 to 100)")
            @RequestParam(defaultValue = "20") int size
        ) {
        return ResponseEntity.ok(placeService.getPlacesByCursor(category, lastId, size));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(
        summary = "Update place details (Owner or ADMIN)",
        description = "Requires current entity version for Optimistic Concurrency Control. Returns 409 on version mismatch."
    )
    public ResponseEntity<PlaceResponse> updatePlace(
        @PathVariable Long id, @Valid @RequestBody PlaceUpdateRequest request
    ) {
        return ResponseEntity.ok(placeService.updatePlace(id, request));
    }

    //uploading uses multipart file
    @PostMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(
        summary = "Upload an image for a place (JPEG, PNG, WebP up to 5MB, max 5 per place)",
        description = "Streams outside @Transactional on Virtual Threads with compensating Cloudinary rollback if DB write fails."
    )
    public ResponseEntity<PlaceResponse> uploadImage(
            @PathVariable Long id,

            @Parameter(description = "Image file (JPEG, PNG, or WebP)")
            @RequestParam("file") MultipartFile file
        ) {
        return ResponseEntity.ok(placeService.uploadImage(id, file));
    }

    @DeleteMapping("/{id}/images")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Delete a specific image from a place (Owner or ADMIN)")
    public ResponseEntity<PlaceResponse> deleteImage(
            @PathVariable Long id,
            @RequestParam("imageUrl") String imageUrl
        ) {
        return ResponseEntity.ok(placeService.deleteImage(id, imageUrl));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Delete a place, bulk-delete its reviews, and purge its Cloudinary images (Owner or ADMIN)")
    public ResponseEntity<Void> deletePlace(@PathVariable Long id) {
        placeService.deletePlace(id);
        return ResponseEntity.noContent().build();
    }
}