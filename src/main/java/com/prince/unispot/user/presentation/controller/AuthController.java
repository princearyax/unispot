package com.prince.unispot.user.presentation.controller;

import com.prince.unispot.core.exception.InvalidTokenException;
import com.prince.unispot.user.application.service.AuthResult;
import com.prince.unispot.user.application.service.AuthService;
import com.prince.unispot.user.presentation.dto.AuthResponse;
import com.prince.unispot.user.presentation.dto.LoginRequest;
import com.prince.unispot.user.presentation.dto.RegisterRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "1. Authentication", description = "Stateless JWT access tokens + rotating HttpOnly refresh token cookies") //class level , says about the group of api to swagger
@SecurityRequirements() // we using empty as :we don't want lock icons on /login, /register, public GET... protected will inherit lock icons by "OpenApiConfig" in swagger context
public class AuthController {

    private final AuthService authService;
    private final long REFRESH_TOKEN_EXPIRATION_SECONDS = 7 * 24 * 60 * 60;//7 days

    @Value ("${unispot.security.jwt.refresh-cookie.same-site}")
    private String refreshCookieSameSite;

    @Value("${unispot.security.jwt.refresh-cookie.secure}")
    private boolean refreshCookieSecure;

    @PostMapping("/register")
    @Operation(summary = "Register a new student account and set HttpOnly refresh cookie") //method level tells in short what it does
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResult result = authService.register(request);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, createCookie(result.refreshToken(), REFRESH_TOKEN_EXPIRATION_SECONDS).toString())
                .body(new AuthResponse(result.accessToken(), result.role()));
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate credentials, return JWT access token, and set HttpOnly refresh cookie")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResult result = authService.login(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, createCookie(result.refreshToken(), REFRESH_TOKEN_EXPIRATION_SECONDS).toString())
                .body(new AuthResponse(result.accessToken(), result.role()));
    }

    //@CookieValue automatically extracts the token from the HTTP request header
    @PostMapping("/refresh")
    @Operation(summary = "Rotate refresh token cookie and issue a fresh JWT access token")
    //setting required as false, so if no cookie, then will return 401 fires
    public ResponseEntity<AuthResponse> refresh(
        @Parameter(hidden = true) @CookieValue(name = "refreshToken", required = false) String refreshToken
    ) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new InvalidTokenException("Refresh token cookie missing.");
        }
        AuthResult result = authService.refreshToken(refreshToken);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, createCookie(result.refreshToken(), REFRESH_TOKEN_EXPIRATION_SECONDS).toString())
                .body(new AuthResponse(result.accessToken(), result.role()));
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke refresh token in DB and clear the HttpOnly cookie")
    public ResponseEntity<Void> logout(
        @Parameter(hidden = true) @CookieValue(name = "refreshToken", required = false) String refreshToken
    ) {
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
                .secure(refreshCookieSecure) // if HTTPS  required
                .path("/api/v1/auth") // browser sends this cookie to /auth endpoints, saving bandwidth on other
                .sameSite(refreshCookieSameSite) // CSRF protection
                .maxAge(maxAge)
                .build();
    }
}

//@Valid doesn't do itself it asks hibernate validator, if any fail spring throw MethodArgumentNotValidException before mthod runs

// @Parameter(hidden = true) prevents Swagger UI from trying to inject a manual header or requiring as input and lets the browser send the stored HttpOnly cookie natively. lets Swagger UI fire the POST /api/v1/auth/refresh request directly.