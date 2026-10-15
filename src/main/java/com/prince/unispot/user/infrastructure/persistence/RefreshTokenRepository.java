package com.prince.unispot.user.infrastructure.persistence;

import com.prince.unispot.user.domain.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);

    @Modifying //for bulk delete/update
    @Query("DELETE FROM RefreshToken rt WHERE rt.user.id = :userId") //explicit delete needed
    void deleteByUserId(@Param("userId") Long userId);
}