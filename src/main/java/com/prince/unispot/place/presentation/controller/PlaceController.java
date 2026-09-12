package com.prince.unispot.place.presentation.controller;

import com.prince.unispot.place.application.service.PlaceService;
import com.prince.unispot.place.domain.model.Category;
import com.prince.unispot.place.presentation.dto.PlaceRequest;
import com.prince.unispot.place.presentation.dto.PlaceSummaryDto;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.data.domain.Pageable;
// import org.springframework.data.domain.Slice;
// import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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
    public ResponseEntity<Void> createPlace(@Valid @RequestBody PlaceRequest request) {
        placeService.createPlace(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // //spring automatically creates pageable from query params(size, sort, page)
    // @GetMapping
    // public ResponseEntity<Slice<PlaceSummaryProjection>> getPlaces(
    //         @RequestParam Category category, 
    //         @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
    //     //if no size is provided, it defaults to 20, and sorts by latest.
    //     return ResponseEntity.ok(placeService.getPlacesByCategory(category, pageable));
    // }

    //optimised pagination by cursor
    @GetMapping("/cursor")
    public ResponseEntity<List<PlaceSummaryDto>> getPlacesByCursor(
            @RequestParam Category category,
            @RequestParam(required = false) Long lastId,
            @PageableDefault(size = 20) Pageable pageable) {
        
        //if is null (first page), pass Long.MAX_VALUE to get the absolute latest
        Long cursor = (lastId != null) ? lastId : Long.MAX_VALUE;
        
        return ResponseEntity.ok(placeService.getPlacesByCursor(category, cursor, pageable));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Void> deletePlace(@PathVariable Long id) {
        placeService.deletePlace(id);
        return ResponseEntity.noContent().build();
    }
}