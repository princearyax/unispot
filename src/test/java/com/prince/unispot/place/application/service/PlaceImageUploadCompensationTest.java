package com.prince.unispot.place.application.service;

import com.prince.unispot.core.application.storage.ImageStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.multipart.MultipartFile;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

//testing that ddeleting orphaned cloudinary img

@ExtendWith(MockitoExtension.class)
class PlaceImageUploadCompensationTest {

    @Mock private PlaceImageTransactionHelper placeImageTxHelper;
    @Mock private ImageStorageService imageStorageService;
    @Mock private MultipartFile multipartFile;

    @InjectMocks
    private PlaceService placeService;

    @Test
    void deletesOrphanedCloudImageWhenDatabaseAttachmentFails() {
        Long placeId = 42L;
        String uploadedUrl = "https://res.cloudinary.com/demo/image/upload/v1/unispot/places/42/abc.jpg";

        doNothing().when(placeImageTxHelper).verifyCanUploadImage(placeId);
        when(imageStorageService.uploadPlaceImage(placeId, multipartFile)).thenReturn(uploadedUrl);
        when(placeImageTxHelper.attachUploadedImageUrl(placeId, uploadedUrl))
                .thenThrow(new ObjectOptimisticLockingFailureException("Place", placeId));

        assertThrows(ObjectOptimisticLockingFailureException.class,
                () -> placeService.uploadImage(placeId, multipartFile));

        // Verify compensating action cleaned up the orphaned Cloudinary asset
        verify(imageStorageService, times(1)).deleteImageByUrl(uploadedUrl);
    }
}