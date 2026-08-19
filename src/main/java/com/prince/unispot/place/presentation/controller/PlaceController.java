package com.prince.unispot.place.presentation.controller;

import com.prince.unispot.place.application.service.PlaceService;
import com.prince.unispot.place.domain.model.Category;
import com.prince.unispot.place.presentation.dto.PlaceRequest;
import com.prince.unispot.place.presentation.dto.PlaceSummaryProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/places")
@RequiredArgsConstructor
public class PlaceController {

    private final PlaceService placeService;

    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Void> createPlace(@RequestBody PlaceRequest request) {
        placeService.createPlace(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping
    public ResponseEntity<Slice<PlaceSummaryProjection>> getPlaces(
            @RequestParam Category category, 
            Pageable pageable) {
        return ResponseEntity.ok(placeService.getPlacesByCategory(category, pageable));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Void> deletePlace(@PathVariable Long id) {
        placeService.deletePlace(id);
        return ResponseEntity.noContent().build();
    }
}