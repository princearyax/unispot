package com.prince.unispot.user.application.service;

import com.prince.unispot.core.security.JwtService;
import com.prince.unispot.user.domain.model.Role;
import com.prince.unispot.user.domain.model.User;
import com.prince.unispot.user.infrastructure.persistence.UserRepository;
import com.prince.unispot.user.presentation.dto.AuthResponse;
import com.prince.unispot.user.presentation.dto.LoginRequest;
import com.prince.unispot.user.presentation.dto.RegisterRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email is already in use.");
        }

        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(Role.USER) // Default role
                .build();

        User savedUser = userRepository.save(user);

        //we generate a JWT specific to our library's implementation
        //here we rely on our jwtService containing the subject (getId,, we stored userId not email.. for audit things.) and a "role" claim
        String jwtToken = jwtService.generateToken(savedUser.getId().toString(), savedUser.getRole().name());
        
        return new AuthResponse(jwtToken, savedUser.getRole().name());
    }

    @Transactional(readOnly = true) //tells hibernate no ned to maintain dirty checking on entties loaded here,,
    public AuthResponse login(LoginRequest request) {
        // delegate auth to Spring Security's Authentication Manager
        // will securely hash the provided password and compare it against the database.
        //not doing by if (passwordEncoder.matches), cuz spring will take care of timing related attacks, and will send const delay
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );

        //here, authentication succeeded.
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(); // Safe to call get() 

        String jwtToken = jwtService.generateToken(user.getId().toString(), user.getRole().name());
        
        return new AuthResponse(jwtToken, user.getRole().name());
    }
}