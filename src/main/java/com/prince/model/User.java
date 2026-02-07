package com.prince.model;

import java.time.LocalDateTime;
import java.util.Set;

import org.hibernate.annotations.CreationTimestamp;

import com.prince.model.enums.Role;
import com.prince.model.enums.UserStatus;

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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter //not usin @data as gives diff , .equals() and hashcode
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder //Helps to create objects easily: User.builder().email(...).build()
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) //sequence would be better as hibernate can batch, but just easy
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING) //uses single enum field, string so that no default unsafe ordinal
    @Column(nullable = false)
    @Builder.Default
    private UserStatus status = UserStatus.PENDING;

    @ElementCollection(fetch = FetchType.EAGER) //Used for collections of value types (no entity, no ID)(not any entity) ,stored in a separate table
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id")) //m:n relation
    @Enumerated(EnumType.STRING)
    private Set<Role> roles;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
