package com.sum.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "users") // 예약어 충돌 방지를 위해 테이블명 지정
@Getter
@Setter // 데이터 수정이 필요한 필드를 위해 선언 (실무에서는 비즈니스 메서드로 대체 권장)
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 스펙을 위한 기본 생성자 (접근 제어로 안전성 확보)
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String username;

    @Column(nullable = false, unique = true, length = 50)
    private String loginId;

    @Column(nullable = false, length = 100)
    private String password;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    /**
     * 최근 로그인 일시
     * 데이터 변경(Update) 시 자동 갱신되지 않도록 별도 분리하며, 로그인 서비스 성공 시점에 명시적 저장 필요
     */
    private LocalDateTime lastLoginAt;

    /**
     * 유저 생성일
     * 최초 회원가입(Insert) 시 서버의 현재 시간으로 자동 입력되며, 이후 수정 불가능
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 유저 정보 갱신일
     * 회원 정보가 변경(Update)될 때마다 서버의 현재 시간으로 자동 갱신
     */
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 최근 로그인 시간을 갱신하기 위한 비즈니스 메서드
     */
    public void updateLastLoginTime() {
        this.lastLoginAt = LocalDateTime.now();
    }
}
