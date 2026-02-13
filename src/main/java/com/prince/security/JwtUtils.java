package com.prince.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import com.prince.config.JwtProperties;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;


//this util class will generate, parse and validates jwt
@Component
public class JwtUtils {
    private final SecretKey key;
    private final long expiration;

    public JwtUtils(JwtProperties jwtProperties){
        this.key = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
        this.expiration = jwtProperties.getExpiration();
    }

    //generate token
    public String generateToken(String email){
        return Jwts.builder()
                    .subject(email)
                    .issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + expiration))
                    .signWith(key)
                    .compact();
    }

}
