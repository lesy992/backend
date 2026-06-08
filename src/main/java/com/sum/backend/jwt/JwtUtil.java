package com.sum.backend.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    private final SecretKey secretKey;

    // 1. 단일 수명 대신 Access/Refresh 용으로 수명 변수 분리
    private final long accessExpirationTime;
    private final long refreshExpirationTime;

    // 2. application.yml의 분리된 설정값을 가져오도록 수정
    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-expiration-time}") long accessExpirationTime,
            @Value("${jwt.refresh-expiration-time}") long refreshExpirationTime) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpirationTime = accessExpirationTime;
        this.refreshExpirationTime = refreshExpirationTime;
    }

    /**
     * Access Token 생성 메서드 (수명이 짧음)
     */
    public String generateAccessToken(String loginId) {
        return createToken(loginId, accessExpirationTime);
    }

    /**
     * Refresh Token 생성 메서드 (수명이 긺)
     */
    public String generateRefreshToken(String loginId) {
        return createToken(loginId, refreshExpirationTime);
    }

    /**
     * 토큰 생성 중복 코드를 줄이기 위한 내부(private) 공통 메서드
     */
    private String createToken(String loginId, long expirationTime) {
        return Jwts.builder()
                .subject(loginId) // 토큰의 주체 (유저 아이디)
                .issuedAt(new Date(System.currentTimeMillis())) // 발행 시간
                .expiration(new Date(System.currentTimeMillis() + expirationTime)) // 각 토큰에 맞는 만료 시간 부여
                .signWith(secretKey) // 암호화 알고리즘 및 키 적용
                .compact();
    }

    /**
     * 1. 토큰의 유효성을 검증하는 메서드 (기존 코드 유지)
     * 서명 위변조, 만료 여부 등을 확인하여 정상적인 토큰인지 반환합니다.
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey) // 발급할 때 사용한 동일한 비밀키로 서명 검증
                    .build()
                    .parseSignedClaims(token); // 파싱 과정에서 자동으로 검증 수행
            return true;
        } catch (Exception e) {
            // 만료(ExpiredJwtException), 위조(SignatureException), 잘못된 형식(MalformedJwtException) 등 발생
            return false;
        }
    }

    /**
     * 2. 토큰에서 유저 아이디(loginId)를 추출하는 메서드 (기존 코드 유지)
     * 반드시 validateToken()으로 검증이 완료된 후 호출해야 안전합니다.
     */
    public String getLoginIdFromToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject(); // createToken()에서 subject에 넣었던 loginId 반환
    }
}