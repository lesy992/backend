package com.sum.backend.controller;

import com.sum.backend.dto.CreateUser;
import com.sum.backend.dto.LoginUser;
import com.sum.backend.entity.User;
import com.sum.backend.jwt.JwtUtil;
import com.sum.backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;


@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    @Operation(summary = "유저 생성", description = "유저의 계정을 생성합니다.")
    @PostMapping("/create")
    public ResponseEntity<User> createUser(@RequestBody CreateUser user) {
        return ResponseEntity.ok(userService.createUser(user));
    }

    @Operation(summary = "유저 삭제", description = "특정 ID를 가진 유저의 계정을 삭제합니다.")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteUser(
            @Parameter(description = "삭제할 유저의 고유 ID", example = "1")
            @PathVariable Long id
    ) {
        // 전달받은 ID를 서비스 계층으로 전달하여 삭제 처리
        userService.deleteUser(id);

        // 삭제 완료 후 200 OK 응답 반환
        return ResponseEntity.ok("유저 계정이 정상적으로 삭제되었습니다.");
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginUser request, HttpServletResponse response) {
        String[] tokens = userService.login(request.getLoginId(), request.getPassword());

        System.out.println(tokens[0]);

        // 1. Access Token 쿠키 (수명: 30분)
        ResponseCookie accessCookie = createCookie("access_token", tokens[0], 60 * 30);
        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());

        // 2. Refresh Token 쿠키 (수명: 14일)
        ResponseCookie refreshCookie = createCookie("refresh_token", tokens[1], 60 * 60 * 24 * 14);
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        return ResponseEntity.ok("로그인 성공");
    }

    @PostMapping("/reissue")
    public ResponseEntity<String> reissue(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = getCookieValue(request, "refresh_token");
        if (refreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Refresh Token이 없습니다.");
        }

        // 새 Access Token 발급
        String newAccessToken = userService.reissueAccessToken(refreshToken);

        // 새 Access Token 쿠키로 덮어쓰기
        ResponseCookie accessCookie = createCookie("access_token", newAccessToken, 60 * 30);
        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());

        return ResponseEntity.ok("Access Token 재발급 완료");
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletRequest request, HttpServletResponse response) {
        // 기존 필터 로직을 통해 SecurityContext에 저장된 유저 아이디 가져오기
        String loginId = SecurityContextHolder.getContext().getAuthentication().getName();

        if (!loginId.equals("anonymousUser")) {
            userService.logout(loginId); // DB에서 Refresh Token 삭제
        }

        // 쿠키 두 개 모두 수명을 0으로 만들어서 삭제 유도
        response.addHeader(HttpHeaders.SET_COOKIE, createCookie("access_token", "", 0).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, createCookie("refresh_token", "", 0).toString());

        return ResponseEntity.ok("로그아웃 완료");
    }

    // 쿠키 생성 중복 코드를 줄이기 위한 유틸 메서드
    private ResponseCookie createCookie(String name, String value, long maxAge) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(false) // 운영 환경에서는 true
                .path("/")
                .maxAge(maxAge)
                .sameSite("Strict")
                .build();
    }

    // 요청에서 특정 쿠키 값을 찾는 유틸 메서드
    private String getCookieValue(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookieName.equals(cookie.getName())) return cookie.getValue();
            }
        }
        return null;
    }

    @Operation(summary = "JWT 토큰 확인", description = "발급된 JWT 토큰을 확인합니다.")
    @PostMapping("/token/validation")
    @ResponseStatus(HttpStatus.OK)
    public void jwtValidate(@RequestParam String token) {
        if(!jwtUtil.validateToken(token)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "유저를 찾을 수 없습니다.");
        }
    }
}
