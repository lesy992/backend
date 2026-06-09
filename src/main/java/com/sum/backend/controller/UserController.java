package com.sum.backend.controller;

import com.sum.backend.dto.CreateUser;
import com.sum.backend.dto.LoginUser;
import com.sum.backend.dto.UserResponse;
import com.sum.backend.entity.User;
import com.sum.backend.jwt.JwtUtil;
import com.sum.backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    @Operation(summary = "유저 생성", description = "유저의 계정을 생성합니다.")
    @PostMapping
    public ResponseEntity<UserResponse> createUser(@RequestBody @Valid CreateUser user) {
        User created = userService.createUser(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(new UserResponse(created));
    }

    @Operation(summary = "유저 삭제", description = "본인 계정을 삭제합니다.")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteUser(
            @Parameter(description = "삭제할 유저의 고유 ID", example = "1")
            @PathVariable Long id
    ) {
        String loginId = SecurityContextHolder.getContext().getAuthentication().getName();
        userService.deleteUser(id, loginId);
        return ResponseEntity.ok("유저 계정이 정상적으로 삭제되었습니다.");
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody @Valid LoginUser request, HttpServletResponse response) {
        String[] tokens = userService.login(request.getLoginId(), request.getPassword());

        ResponseCookie accessCookie = createCookie("access_token", tokens[0], 60 * 30);
        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());

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

        String newAccessToken = userService.reissueAccessToken(refreshToken);

        ResponseCookie accessCookie = createCookie("access_token", newAccessToken, 60 * 30);
        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());

        return ResponseEntity.ok("Access Token 재발급 완료");
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletRequest request, HttpServletResponse response) {
        String loginId = null;

        // 유효한 Access Token이 있으면 SecurityContext에서 loginId 추출
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && !auth.getName().equals("anonymousUser")) {
            loginId = auth.getName();
        } else {
            // Access Token이 만료된 경우 Refresh Token에서 loginId 추출
            String refreshToken = getCookieValue(request, "refresh_token");
            if (refreshToken != null && jwtUtil.validateToken(refreshToken)) {
                loginId = jwtUtil.getLoginIdFromToken(refreshToken);
            }
        }

        if (loginId != null) {
            userService.logout(loginId);
        }

        response.addHeader(HttpHeaders.SET_COOKIE, createCookie("access_token", "", 0).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, createCookie("refresh_token", "", 0).toString());

        return ResponseEntity.ok("로그아웃 완료");
    }

    @Operation(summary = "JWT 토큰 확인", description = "access_token 쿠키의 유효성을 확인합니다.")
    @GetMapping("/token/validation")
    public ResponseEntity<String> jwtValidate() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || "anonymousUser".equals(auth.getName())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("유효하지 않은 토큰입니다.");
        }
        return ResponseEntity.ok("유효한 토큰입니다.");
    }

    private ResponseCookie createCookie(String name, String value, long maxAge) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(false) // 운영 환경에서는 true
                .path("/")
                .maxAge(maxAge)
                .sameSite("Strict")
                .build();
    }

    private String getCookieValue(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookieName.equals(cookie.getName())) return cookie.getValue();
            }
        }
        return null;
    }
}
