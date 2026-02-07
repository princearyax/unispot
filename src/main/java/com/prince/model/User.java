package com.prince.model;

import java.time.LocalDateTime;
import java.util.Set;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "users")
@Data //getters, seters, toString
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) //sequence would be better as hibernate can batch, but just easy
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Enumerated(EnumType.STRING) //uses single enum field, string so that no default unsafe ordinal
    private UserStatus status = UserStatus.PENDING;

    @ElementCollection(fetch = FetchType.EAGER) //Used for collections of value types (no entity, no ID)(not any entity) ,stored in a separate table
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id")) //m:n relation
    @Enumerated(EnumType.STRING)
    private Set<Role> roles;

    @CreationTimestamp
    private LocalDateTime createdAt;
}

enum UserStatus { PENDING, ACTIVE, BANNED }
enum Role { ROLE_USER, ROLE_ADMIN, ROLE_ANONYMOUS }