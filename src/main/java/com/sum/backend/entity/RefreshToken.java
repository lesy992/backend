package com.sum.backend.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String loginId;

    @Column(nullable = false, length = 512)
    private String token;

    public RefreshToken(String loginId, String token) {
        this.loginId = loginId;
        this.token = token;
    }

    public void updateToken(String token) {
        this.token = token;
    }
}
