package com.sum.backend.service;

import com.sum.backend.dto.CreateUser;
import com.sum.backend.entity.RefreshToken;
import com.sum.backend.entity.User;
import com.sum.backend.jwt.JwtUtil;
import com.sum.backend.repository.RefreshTokenRepository;
import com.sum.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Service
@RequiredArgsConstructor // final로 선언된 필드의 의존성을 자동으로 주입 (생성자 주입)
public class UserService {

    // 유저가 없을 때도 BCrypt를 실행해 응답 시간 차이로 아이디 존재 여부를 추측하지 못하게 함
    private static final String DUMMY_HASH = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder; // SecurityConfig에서 등록한 빈 주입
    private final JwtUtil jwtUtil;
    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * 신규 유저 생성 메서드
     */
    @Transactional // 메서드 실행 중 예외 발생 시 자동으로 롤백 처리
    public User createUser(CreateUser user) {

        // 1. 중복 가입 검증 (Fact Check: 실제 운영 환경에서는 필수적인 로직)
        if (userRepository.existsByLoginId(user.getLoginId())) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
        }

        // 비밀번호 평문을 BCrypt 알고리즘으로 암호화
        String encodedPassword = passwordEncoder.encode(user.getPassword());

        // 2. User 객체 생성 (엔티티에 설정해둔 @Builder 활용)
        User newUser = User.builder()
                .username(user.getUsername())
                .loginId(user.getLoginId())
                .password(encodedPassword) // 추후 Spring Security 등을 통해 암호화(Encoding) 필요
                .email(user.getEmail())
                // createdAt, updatedAt은 Hibernate 어노테이션에 의해 자동 주입됨
                // lastLoginAt은 최초 가입 시점이므로 null 처리하거나 필요시 현재 시간 삽입
                .build();

        // 3. DB에 저장 후, 저장된 엔티티 반환 (이때 id 값이 부여됨)
        return userRepository.save(newUser);
    }

    /**
     * 유저 삭제 메서드 (Hard Delete) - 본인 계정만 삭제 가능
     */
    @Transactional
    public void deleteUser(Long id, String loginId) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다."));

        if (!user.getLoginId().equals(loginId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인 계정만 삭제할 수 있습니다.");
        }

        refreshTokenRepository.deleteByLoginId(user.getLoginId());
        userRepository.deleteById(id);
    }

    /**
     * 로그인 시 배열로 [AccessToken, RefreshToken] 반환
     */
    @Transactional
    public String[] login(String loginId, String password) {
        Optional<User> userOpt = userRepository.findByLoginId(loginId);
        String storedHash = userOpt.map(User::getPassword).orElse(DUMMY_HASH);
        boolean matches = passwordEncoder.matches(password, storedHash);

        if (userOpt.isEmpty() || !matches) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다.");
        }

        User user = userOpt.get();
        user.updateLastLoginTime();

        // 두 개의 토큰 생성
        String accessToken = jwtUtil.generateAccessToken(loginId);
        String refreshToken = jwtUtil.generateRefreshToken(loginId);

        // Refresh Token을 DB에 저장 (이미 있다면 갱신, 없다면 새로 생성)
        refreshTokenRepository.findByLoginId(loginId)
                .ifPresentOrElse(
                        rt -> rt.updateToken(refreshToken),
                        () -> refreshTokenRepository.save(new RefreshToken(loginId, refreshToken))
                );

        return new String[]{accessToken, refreshToken};
    }

    /**
     * Access Token 재발급 로직
     */
    @Transactional
    public String reissueAccessToken(String refreshToken) {
        // 1. Refresh Token 자체의 유효성 검증
        if (!jwtUtil.validateToken(refreshToken)) {
            throw new IllegalArgumentException("유효하지 않거나 만료된 Refresh Token입니다. 다시 로그인해주세요.");
        }

        // 2. DB에 해당 Refresh Token이 존재하는지 확인 (해커가 임의로 만든 토큰 차단)
        RefreshToken findToken = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new IllegalArgumentException("다른 기기에서 로그인되어 인증이 만료되었습니다. 다시 로그인해주세요."));

        // 3. 새로운 Access Token 발급
        return jwtUtil.generateAccessToken(findToken.getLoginId());
    }

    /**
     * 로그아웃: DB에서 Refresh Token 삭제
     */
    @Transactional
    public void logout(String loginId) {
        refreshTokenRepository.deleteByLoginId(loginId);
    }
}