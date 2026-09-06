package com.prince.unispot.user.application.service;

import com.prince.unispot.user.domain.model.RefreshToken;
import com.prince.unispot.user.infrastructure.persistence.RefreshTokenRepository;
import com.prince.unispot.user.infrastructure.persistence.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    @Value("${unispot.security.jwt.refresh-expiration}") //like 604800000 for 7 days
    private Long refreshTokenDurationMs;

    
    @Transactional
    public RefreshToken createRefreshToken(Long userId) {
        RefreshToken refreshToken = RefreshToken.builder()
                .user(userRepository.getReferenceById(userId)) //proxy again, no extra SELECT
                .token(UUID.randomUUID().toString()) // Opaque token, random meaningless
                .expiryDate(Instant.now().plusMillis(refreshTokenDurationMs))
                .build();

        return refreshTokenRepository.save(refreshToken); //Insert query
    }

    //called during refresh flow, throws if invalid
    @Transactional
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().compareTo(Instant.now()) < 0) {
            refreshTokenRepository.delete(token);
            throw new IllegalArgumentException("Refresh token was expired. Please make a new signin request");
        }
        return token;
    }

    @Transactional
    public void deleteByUserId(Long userId) {
        refreshTokenRepository.deleteByUser(userRepository.getReferenceById(userId));
    }

    @Transactional(readOnly = true) //as no modifying just read
    public Optional<RefreshToken> findByToken(String token) {
    return refreshTokenRepository.findByToken(token);
}
}