package com.prince.service;

import com.prince.dto.request.LoginRequest;
import com.prince.dto.request.RegisterRequest;
import com.prince.dto.response.AuthResponse;

//interface for service, so that can use mockito w/o hitting real db
public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
}
