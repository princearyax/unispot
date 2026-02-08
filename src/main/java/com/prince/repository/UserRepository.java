package com.prince.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.prince.model.User;


public interface UserRepository extends JpaRepository<User, Long>{
    //for login
    Optional<User> findByEmail(String email);
    
    //during register
    boolean existsByEmail(String email);
} 
