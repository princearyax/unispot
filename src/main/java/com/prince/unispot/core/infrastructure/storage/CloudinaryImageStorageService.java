package com.prince.unispot.core.infrastructure.storage;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.prince.unispot.core.application.storage.ImageStorageService;
import com.prince.unispot.core.exception.FileStorageException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class CloudinaryImageStorageService implements ImageStorageService {

    private static final Logger log = LoggerFactory.getLogger(CloudinaryImageStorageService.class);

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    // Matches everything after /upload/(optional v12345/) up to the file extension
    private static final Pattern PUBLIC_ID_PATTERN =
            Pattern.compile("/upload/(?:v\\d+/)?(.+)\\.[a-zA-Z0-9]+$");

    private final Cloudinary cloudinary;

    //will upload in proper folder struc (place id related),
    //  and a UUId, so as can use the uuid/id for delete, will know 
    @Override
    public String uploadPlaceImage(Long placeId, MultipartFile file) {
        validateImageFile(file);

        String folder = "unispot/places/" + placeId;
        String publicId = UUID.randomUUID().toString();

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> options = ObjectUtils.asMap(
                    "folder", folder,
                    "public_id", publicId,
                    "resource_type", "image",
                    "overwrite", true
            ); //needed for cloudinary

            @SuppressWarnings("unchecked")
            Map<String, Object> result = cloudinary.uploader().upload(file.getBytes(), options);

            String secureUrl = (String) result.get("secure_url");
            if (secureUrl == null || secureUrl.isBlank()) {
                throw new FileStorageException("Cloudinary did not return a secure_url.");
            }
            return secureUrl;
        } catch (IOException ex) {
            throw new FileStorageException("Failed to upload image to cloud storage.", ex);
        }
    }

    @Override
    public void deleteImageByUrl(String imageUrl) {
        String publicId = extractPublicId(imageUrl);
        if (publicId == null) {
            log.warn("Could not extract Cloudinary public_id from URL: {}", imageUrl);
            return;
        }

        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", "image"));
        } catch (IOException ex) {
            // Log and swallow on delete so cleanup failure doesn't break user flow
            log.error("Failed to delete Cloudinary asset with public_id {}: {}", publicId, ex.getMessage());
        }
    }

    /**
     * Validates non-empty file, Content-Type header, AND binary magic bytes
     * to prevent disguised script/executable uploads. security things
     */
    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Image file must not be empty.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Only JPEG, PNG, and WebP images are allowed.");
        }

        try (InputStream is = file.getInputStream()) {
            byte[] header = is.readNBytes(12);
            if (!hasValidImageMagicBytes(header)) {
                throw new IllegalArgumentException("File content does not match a valid JPEG, PNG, or WebP binary signature.");
            }
        } catch (IOException ex) {
            throw new FileStorageException("Could not read uploaded file stream.", ex);
        }
    }

    private boolean hasValidImageMagicBytes(byte[] h) {
        if (h.length < 4) {
            return false;
        }
        // JPEG: FF D8 FF
        boolean isJpeg = (h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8 && (h[2] & 0xFF) == 0xFF;
        // PNG: 89 50 4E 47
        boolean isPng = (h[0] & 0xFF) == 0x89 && h[1] == 0x50 && h[2] == 0x4E && h[3] == 0x47;
        // WebP: "RIFF" (0..3) and "WEBP" (8..11)
        boolean isWebp = h.length >= 12
                && h[0] == 'R' && h[1] == 'I' && h[2] == 'F' && h[3] == 'F'
                && h[8] == 'W' && h[9] == 'E' && h[10] == 'B' && h[11] == 'P';

        return isJpeg || isPng || isWebp;
    }

    static String extractPublicId(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return null;
        }
        Matcher matcher = PUBLIC_ID_PATTERN.matcher(imageUrl);
        return matcher.find() ? matcher.group(1) : null;
    }
}