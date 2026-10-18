package com.prince.unispot.core.application.storage;

import org.springframework.web.multipart.MultipartFile;

//interface for cloudinary img upload things
public interface ImageStorageService {
    String uploadPlaceImage(Long placeId, MultipartFile file);
    void deleteImageByUrl(String imageUrl); //will handle using proper directory structure, etc
}