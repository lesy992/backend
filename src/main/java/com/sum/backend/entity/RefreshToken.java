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
    private String loginId; // 어떤 유저의 교환권인지 식별

    @Column(nullable = false, length = 512)
    private String token; // Refresh Token 값

    public RefreshToken(String loginId, String token) {
        this.loginId = loginId;
        this.token = token;
    }

    // 토큰 갱신 메서드 (기존 로그인 유저가 새로 로그인했을 때 교체용)
    public void updateToken(String token) {
        this.token = token;
    }
}
