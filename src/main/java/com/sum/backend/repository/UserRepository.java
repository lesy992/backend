package com.sum.backend.repository;

import com.sum.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // 회원가입 시 중복 아이디/이메일 검증을 위해 Spring Data JPA가 자동으로 쿼리를 생성해주는 메서드
    boolean existsByLoginId(String loginId);
    boolean existsByEmail(String email);
    Optional<User> findByLoginId(String loginId);
}
