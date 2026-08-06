package com.prince.unispot.core.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;
//handles jwt parsing
//it does cryptographic validation and extraction
//jwt header.payload(aka claim).signature(crypto hash)
@Service
public class JwtService {

    @Value("${unispot.security.jwt.secret}") //pulls from application.prop/yml or env vars at runtime
    private String secretKey;

    // We store the User ID as the "Subject" of the JWT
    public String extractUserId(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    public boolean isTokenValid(String token) {
        return !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    //using generic, allows to extract one specific piece of data (a claim) from a JWT.
    //It decrypts, verifies and opens the token , then passes the contents to functional helper to pull out what i exactly asked 
    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = Jwts.parser() //sets up jjwt parser engine
                .verifyWith(getSignInKey()) //calculates hmac, sha hash
                .build()
                .parseSignedClaims(token) //validates expiration, unpack jwt and convert in java obj
                .getPayload(); //grabs the body (claims) containing user data
        return claimsResolver.apply(claims); //execute a custom function to get what wantd
    }

    private SecretKey getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey); //decodes our app.yml secret key(sud be atleast 256 bit, 32 bytes) back
        return Keys.hmacShaKeyFor(keyBytes); //converts raw bytes into cryptographic secret key( a symmetric way)
    }
}