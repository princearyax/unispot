package com.prince.unispot.place.presentation.controller;

import com.prince.unispot.place.application.service.PlaceService;
import com.prince.unispot.place.domain.model.Category;
import com.prince.unispot.place.presentation.dto.CursorPageResponse;
import com.prince.unispot.place.presentation.dto.PlaceRequest;
import com.prince.unispot.place.presentation.dto.PlaceResponse;
import com.prince.unispot.place.presentation.dto.PlaceSummaryDto;
import com.prince.unispot.place.presentation.dto.PlaceUpdateRequest;

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
public class PlaceController {

    private final PlaceService placeService;

    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
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
    public ResponseEntity<PlaceResponse> getPlace(@PathVariable Long id) {
        return ResponseEntity.ok(placeService.getPlace(id));
    }

    //optimise pagination by cursor
    @GetMapping("/cursor")
    public ResponseEntity<CursorPageResponse<PlaceSummaryDto>> getPlacesByCursor(
            @RequestParam Category category,
            @RequestParam(required = false) Long lastId,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(placeService.getPlacesByCursor(category, lastId, size));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<PlaceResponse> updatePlace(@PathVariable Long id, @Valid @RequestBody PlaceUpdateRequest request) {
        return ResponseEntity.ok(placeService.updatePlace(id, request));
    }

    //uploading uses multipart file
    @PostMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<PlaceResponse> uploadImage(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(placeService.uploadImage(id, file));
    }

    @DeleteMapping("/{id}/images")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<PlaceResponse> deleteImage(
            @PathVariable Long id,
            @RequestParam("imageUrl") String imageUrl) {
        return ResponseEntity.ok(placeService.deleteImage(id, imageUrl));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Void> deletePlace(@PathVariable Long id) {
        placeService.deletePlace(id);
        return ResponseEntity.noContent().build();
    }
}