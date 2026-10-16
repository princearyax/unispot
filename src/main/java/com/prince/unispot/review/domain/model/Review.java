package com.prince.unispot.review.domain.model;
//acts as child of both place and user

import com.prince.unispot.core.domain.BaseTimeEntity;
import com.prince.unispot.place.domain.model.Place;
import com.prince.unispot.user.domain.model.User;

import jakarta.persistence.*;
import lombok.*;

@Entity
//so that can get reviews belonging to a place quickly
@Table(name = "reviews", 
    indexes = {
        @Index(name = "idx_review_place_id", columnList = "place_id")
    },
    //so can check one user can review one place once only
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_review_place_user", columnNames = {"place_id", "user_id"})
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Review extends BaseTimeEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "review_seq")
    @SequenceGenerator(name = "review_seq", sequenceName = "review_id_seq", allocationSize = 13)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id", nullable = false) //owner, FK
    private Place place;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Integer rating;

    @Column(length = 1000)
    private String comment;
}
