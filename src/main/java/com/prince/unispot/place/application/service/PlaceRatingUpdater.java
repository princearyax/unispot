package com.prince.unispot.place.application.service;

import com.prince.unispot.core.exception.ResourceNotFoundException;
import com.prince.unispot.place.domain.model.Place;
import com.prince.unispot.place.infrastructure.persistence.PlaceRepository;
import com.prince.unispot.review.infrastructure.persistence.RatingAggregate;
import com.prince.unispot.review.infrastructure.persistence.ReviewRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
  exists just to recalculateRating s retry loop gets a genuinely fresh transaction ,and EntityManager on each attempt. 
  as need to call from outside so thast proxy trigger

  cant do nested ,or requires_new propagation of Transactional inside, as it will hold outer place save so recalculate rating will see old one lss review

  Self-invocation (PlaceService calling its own @Transactional method) bypasses Spring's proxy, so REQUIRES_NEW has to live on a separate bean.
 */

//internal not a part of public api
@Component
@RequiredArgsConstructor
class PlaceRatingUpdater {

    private final PlaceRepository placeRepository;
    private final ReviewRepository reviewRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void applyLatestAggregate(Long placeId) {
        RatingAggregate aggregate = reviewRepository.calculateAggregate(placeId);

        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new ResourceNotFoundException("Place not found with id: " + placeId));

        place.setAverageRating(aggregate.average());
        place.setReviewCount(aggregate.count().intValue());

        placeRepository.saveAndFlush(place);
    }
}