package com.sum.backend.jwt;


import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = null;

        // 1. 요청에서 모든 쿠키를 가져옵니다.
        Cookie[] cookies = request.getCookies();

        // 2. 쿠키 중 이름이 "access_token"인 쿠키를 찾습니다.
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("access_token".equals(cookie.getName())) { // 기존 "jwt"에서 이름 변경
                    token = cookie.getValue();
                    break;
                }
            }
        }

        // 3. 토큰이 존재하고 서명/만료일이 유효한지 검증합니다.
        // (DB 조회 없이 순수하게 토큰 자체가 유효한지만 빠르고 가볍게 검사합니다)
        if (token != null && jwtUtil.validateToken(token) && jwtUtil.isAccessToken(token)) {

            // 4. 유효하다면 SecurityContext에 인증 정보를 저장하여 컨트롤러 통과를 허용합니다.
            String loginId = jwtUtil.getLoginIdFromToken(token);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(loginId, null, Collections.emptyList());

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }
}
