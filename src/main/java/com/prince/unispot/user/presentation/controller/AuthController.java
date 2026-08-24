package com.prince.unispot.user.presentation.controller;

import com.prince.unispot.user.application.service.AuthService;
import com.prince.unispot.user.presentation.dto.AuthResponse;
import com.prince.unispot.user.presentation.dto.LoginRequest;
import com.prince.unispot.user.presentation.dto.RegisterRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}

//@Valid doesn't do itself it asks hibernate validator, if any fail spring throw MethodArgumentNotValidException before mthod runs