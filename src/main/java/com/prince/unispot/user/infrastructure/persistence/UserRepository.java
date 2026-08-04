package com.prince.unispot.user.infrastructure.persistence;

import com.prince.unispot.user.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

// @Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}