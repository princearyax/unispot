package com.prince.unispot.core.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@MappedSuperclass//Designates a class whose mapping information is inherited by its subclasses
@EntityListeners(AuditingEntityListener.class) //intercepts database INSERT and UPDATE , automatically populates fields 
//tells this entity to listen to JPA config
@Getter
@Setter
public abstract class BaseTimeEntity {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;
}