package com.prince.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import com.prince.config.JwtProperties;

import io.jsonwebtoken.Claims;
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

    //validate token
    public boolean validateToken(String token, String userEmail){
        final String extractedEmail = extractUsername(token);
        return (extractedEmail.equals(userEmail) && !isTokenExpired(token));
    }

    //extract username
    //claims -> claims.getSubject() aka Claims::getSubject
    public String extractUsername(String token){
        return extractClaim(token, Claims::getSubject);
    }

    //helping methods
    //one generic method that can pull out any claim
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver){
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    } 

    private Claims extractAllClaims(String token){
        return Jwts.parser()
                .verifyWith(key)                //.getSigningKey()
                .build()
                .parseSignedClaims(token)       //.parseClaimsJws()
                .getPayload();                  //.getBody()
    }

    private boolean isTokenExpired(String token){
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token){
        return extractClaim(token, Claims::getExpiration);
    }

}
