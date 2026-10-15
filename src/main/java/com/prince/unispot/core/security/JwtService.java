package com.prince.unispot.core.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
//creates and verifies access token (HS256)
//key is built once at startup so bad secret will fail

//handles jwt parsing
//it does cryptographic validation and extraction
//jwt header.payload(aka claim).signature(crypto hash)
@Service
public class JwtService {

    @Value("${unispot.security.jwt.secret}") //pulls from application.prop/yml or env vars at runtime
    private String secretKeyRaw;

    @Value("${unispot.security.jwt.expiration}")
    private long jwtExpiration;

    private SecretKey signInKey; //cache it

    @PostConstruct 
    void initKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKeyRaw);//decodes our app.yml secret key(sud be atleast 256 bit, 32 bytes) back
        this.signInKey = Keys.hmacShaKeyFor(keyBytes);//converts raw bytes into cryptographic secret key( a symmetric way)
    }
    
    //for generatibg token, userId is stored as subject
    public String generateToken(String userId, String role) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .claim("role", role) // Custom claim for RBAC
                .subject(userId) // Standard claim for principal identity
                .issuedAt(new Date(now))
                .expiration(new Date(now + jwtExpiration))
                .signWith(signInKey)
                .compact();
    }

    //pareses verfies and check expiration, one pass
    //throws JwtException : ExpiredJwtException, SignatureException, etc
    public Claims parseAndValidateClaims(String token) {
        return Jwts.parser()
                .verifyWith(signInKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

}