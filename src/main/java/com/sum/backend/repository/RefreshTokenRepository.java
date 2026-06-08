package com.sum.backend.repository;

import com.sum.backend.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByLoginId(String loginId);
    Optional<RefreshToken> findByToken(String token);
    void deleteByLoginId(String loginId);
}