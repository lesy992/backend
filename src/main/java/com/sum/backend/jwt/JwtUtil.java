package com.sum.backend.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
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
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.accessExpirationTime = accessExpirationTime;
        this.refreshExpirationTime = refreshExpirationTime;
    }

    public String generateAccessToken(String loginId) {
        return createToken(loginId, accessExpirationTime, "access");
    }

    public String generateRefreshToken(String loginId) {
        return createToken(loginId, refreshExpirationTime, "refresh");
    }

    private String createToken(String loginId, long expirationTime, String type) {
        return Jwts.builder()
                .subject(loginId)
                .claim("typ", type)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(secretKey)
                .compact();
    }

    public boolean isAccessToken(String token) {
        try {
            String type = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .get("typ", String.class);
            return "access".equals(type);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 1. 토큰의 유효성을 검증하는 메서드 (기존 코드 유지)
     * 서명 위변조, 만료 여부 등을 확인하여 정상적인 토큰인지 반환
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