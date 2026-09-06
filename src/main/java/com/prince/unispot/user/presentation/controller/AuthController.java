package com.prince.unispot.user.presentation.controller;

import com.prince.unispot.user.application.service.AuthResult;
import com.prince.unispot.user.application.service.AuthService;
import com.prince.unispot.user.presentation.dto.AuthResponse;
import com.prince.unispot.user.presentation.dto.LoginRequest;
import com.prince.unispot.user.presentation.dto.RegisterRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final long REFRESH_TOKEN_EXPIRATION_SECONDS = 7 * 24 * 60 * 60;//7 days

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResult result = authService.register(request);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, createCookie(result.refreshToken(), REFRESH_TOKEN_EXPIRATION_SECONDS).toString())
                .body(new AuthResponse(result.accessToken(), result.role()));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResult result = authService.login(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, createCookie(result.refreshToken(), REFRESH_TOKEN_EXPIRATION_SECONDS).toString())
                .body(new AuthResponse(result.accessToken(), result.role()));
    }

    //@CookieValue automatically extracts the token from the HTTP request header
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@CookieValue(name = "refreshToken") String refreshToken) {
        AuthResult result = authService.refreshToken(refreshToken);
        return ResponseEntity.ok(new AuthResponse(result.accessToken(), result.role()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(name = "refreshToken", required = false) String refreshToken) {
        if (refreshToken != null) {
            authService.logout(refreshToken);
        }
        // Overwrite the cookie with a 0 max-age to delete it from the browser
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, createCookie("", 0).toString())
                .build();
    }

    private ResponseCookie createCookie(String token, long maxAge) {
        return ResponseCookie.from("refreshToken", token)
                .httpOnly(true) // blocks js XSS
                .secure(true) // Requires HTTPS 
                .path("/api/v1/auth") // browser sends this cookie to /auth endpoints, saving bandwidth on other
                .sameSite("Strict") // CSRF protection
                .maxAge(maxAge)
                .build();
    }
}

//@Valid doesn't do itself it asks hibernate validator, if any fail spring throw MethodArgumentNotValidException before mthod runs