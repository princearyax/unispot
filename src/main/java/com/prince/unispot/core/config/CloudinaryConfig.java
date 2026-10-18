package com.prince.unispot.core.config;

import com.cloudinary.Cloudinary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class CloudinaryConfig {

    private static final Logger log = LoggerFactory.getLogger(CloudinaryConfig.class);

    @Value("${unispot.cloudinary.url:}")
    private String cloudinaryUrl;

    @Bean
    public Cloudinary cloudinary() {
        if (cloudinaryUrl == null || cloudinaryUrl.isBlank()) {
            log.warn("CLOUDINARY_URL is not configured. will use placeholder config (uploads will fail until set)");
            return new Cloudinary(Map.of(
                    "cloud_name", "unconfigured",
                    "api_key", "unconfigured",
                    "api_secret", "unconfigured",
                    "secure", true
            ));
        }
        Cloudinary cloudinary = new Cloudinary(cloudinaryUrl);
        cloudinary.config.secure = true; // sud enforce HTTPS URLs
        return cloudinary;
    }
}