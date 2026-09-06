package com.prince.unispot.user.application.service;

import com.prince.unispot.core.security.AppUserDetails;
import com.prince.unispot.core.security.JwtService;
import com.prince.unispot.user.domain.model.RefreshToken;
import com.prince.unispot.user.domain.model.Role;
import com.prince.unispot.user.domain.model.User;
import com.prince.unispot.user.infrastructure.persistence.UserRepository;
import com.prince.unispot.user.presentation.dto.LoginRequest;
import com.prince.unispot.user.presentation.dto.RegisterRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
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
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public AuthResult register(RegisterRequest request) {
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
        String accessToken = jwtService.generateToken(savedUser.getId().toString(), savedUser.getRole().name());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(savedUser.getId());
        
        return new AuthResult(accessToken, refreshToken.getToken(), savedUser.getRole().name());
    }

    @Transactional //readonly only when not using modifying/creating
    public AuthResult login(LoginRequest request) {
        // delegate auth to Spring Security's Authentication Manager
        // will securely hash the provided password and compare it against the database.
        //not doing by if (passwordEncoder.matches), cuz spring will take care of timing related attacks, and will send const delay
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                request.email(),
                request.password()
            )
        );

        // get the in-memory object (no DB hits twice)
        AppUserDetails userDetails = (AppUserDetails) authentication.getPrincipal();

        String accessToken = jwtService.generateToken(userDetails.id().toString(), userDetails.role());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(userDetails.id());
    
        return new AuthResult(accessToken, refreshToken.getToken(), userDetails.role());
    }

    @Transactional
    public AuthResult refreshToken(String tokenString) {
        return refreshTokenService.findByToken(tokenString)
            .map(refreshTokenService::verifyExpiration)
            .map(RefreshToken::getUser)
            .map(user -> {
                String accessToken = jwtService.generateToken(user.getId().toString(), user.getRole().name());
                //return the same refresh token unless implementing rotation to pretect from token theft. without rotation access token remains valid for the time it was generated
                return new AuthResult(accessToken, tokenString, user.getRole().name()); 
            })
            .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));
    }

    @Transactional
    public void logout(String tokenString) {
        refreshTokenService.findByToken(tokenString)
            .ifPresent(refreshToken -> refreshTokenService.deleteByUserId(refreshToken.getUser().getId()));
    }
}