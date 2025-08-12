package com.example.finlight.global.auth.oauth;

import com.example.finlight.global.auth.jwt.JwtUtil;
import com.example.finlight.global.auth.PrincipalDetails;
import com.example.finlight.global.redis.RefreshTokenService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

// OAuth2LoginAuthenticationFilter에서 인증이 성공, 즉 로그인 성공 후 JWT 발급 및 응답
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws ServletException, IOException {
        PrincipalDetails principal = (PrincipalDetails) authentication.getPrincipal();   // PrincipalDetails에서 사용자 정보를 꺼냄. PrincipalDetails - 인증된 사용자 정보를 담아 SecurityContext에 저장되는 객체
        String accessToken = jwtUtil.createAccessToken(principal.getUser().getId());
        String refreshToken = jwtUtil.createRefreshToken(principal.getUser().getId());
        Duration refreshTtl = jwtUtil.getRefreshTtl();

        // Redis에 Refresh Token을 저장
        refreshTokenService.save(principal.getUser().getId(), refreshToken, refreshTtl);

        // Refresh Token -> HttpOnly 쿠키로 전달
        ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(false) // HTTPS일 경우 true
                .path("/")
                .maxAge(Duration.ofDays(14))
                .sameSite("Lax") // 크로스 도메인 대응
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());


        // 액세스 토큰은 userId와 함께 클라이언트에 JSON으로 응답
        UUID userId = principal.getUser().getId();

        response.setStatus(HttpServletResponse.SC_OK); // 200 ok
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");  // 토큰이 포함된 응답이므로 캐시 금지
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        response.getWriter().printf("{\"accessToken\":\"%s\",\"userId\":\"%s\"}", accessToken, userId);

    }
}
