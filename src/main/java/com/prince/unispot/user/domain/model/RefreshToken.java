package com.prince.unispot.user.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

//stateful refresh token, in db, so taht can revoke/logout/ban user
@Entity
@Table(name = "refresh_tokens", indexes = {
    @Index(name = "idx_refresh_token", columnList = "token", unique = true)
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "refresh_token_seq")
    @SequenceGenerator(name = "refresh_token_seq", sequenceName = "refresh_token_sequence", allocationSize = 10)
    private Long id;

    //will use cryptographically secure random UUID string
    @Column(nullable = false, unique = true, length = 36) //uuid are 36 chars
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Instant expiryDate;
}