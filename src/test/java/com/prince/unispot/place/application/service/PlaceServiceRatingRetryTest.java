package com.prince.unispot.place.application.service;

import com.prince.unispot.core.security.AuthorizationHelper;
import com.prince.unispot.place.infrastructure.persistence.PlaceRepository;
import com.prince.unispot.review.infrastructure.persistence.ReviewRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

//for retry logic test, when update/create review for place the aggregate value, changing retry logic
@ExtendWith(MockitoExtension.class)
class PlaceServiceRatingRetryTest {

    //these are faked
    @Mock private PlaceRatingUpdater placeRatingUpdater;
    @Mock private PlaceRepository placeRepository;
    @Mock private ReviewRepository reviewRepository;
    @Mock private AuthorizationHelper authorizationHelper;

    //real obj we testin
    @InjectMocks
    private PlaceService placeService;

    //std flow: Arrange(set up mock), Act (call the method ), and Assert(verify)
    @Test
    void succeedsImmediatelyWhenNoConflict() {
        doNothing().when(placeRatingUpdater).applyLatestAggregate(1L);

        assertDoesNotThrow(() -> placeService.recalculateRating(1L));

        verify(placeRatingUpdater, times(1)).applyLatestAggregate(1L);
    }

    @Test
    void retriesOnConflictThenSucceeds() {
        doThrow(new ObjectOptimisticLockingFailureException("Place", 1L))
                .doThrow(new ObjectOptimisticLockingFailureException("Place", 1L))
                .doNothing()
                .when(placeRatingUpdater).applyLatestAggregate(1L);

        assertDoesNotThrow(() -> placeService.recalculateRating(1L));

        verify(placeRatingUpdater, times(3)).applyLatestAggregate(1L);
    }

    @Test
    void givesUpAfterMaxAttemptsAndRethrows() {
        doThrow(new ObjectOptimisticLockingFailureException("Place", 1L))
                .when(placeRatingUpdater).applyLatestAggregate(1L);

        assertThrows(ObjectOptimisticLockingFailureException.class,
                () -> placeService.recalculateRating(1L));

        verify(placeRatingUpdater, times(3)).applyLatestAggregate(1L);
    }
}