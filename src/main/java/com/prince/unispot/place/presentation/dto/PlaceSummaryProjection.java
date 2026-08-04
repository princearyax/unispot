package com.prince.unispot.place.presentation.dto;

import com.prince.unispot.place.domain.model.Category;

public interface PlaceSummaryProjection {
    Long getId();
    String getName();
    Category getCategory();
    // Excluding description, coords and imageUrls to save network bandwidth
}