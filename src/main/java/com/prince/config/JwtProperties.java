package com.prince.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;

@Configuration //this class define beans
@ConfigurationProperties(prefix = "security.jwt") //maps properties files to object
@Getter
@Setter
public class JwtProperties {
    private String secret;
    private long expiration;
}
