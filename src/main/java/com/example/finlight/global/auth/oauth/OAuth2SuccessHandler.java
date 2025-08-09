package com.example.finlight.global.auth.oauth;

import com.example.finlight.global.auth.jwt.JwtUtil;
import com.example.finlight.global.auth.PrincipalDetails;
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

// OAuth2LoginAuthenticationFilter에서 인증이 성공, 즉 로그인 성공 후 JWT 발급 및 응답
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {
    private final JwtUtil jwtUtil;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws ServletException, IOException {
        PrincipalDetails principal = (PrincipalDetails) authentication.getPrincipal();   // PrincipalDetails에서 사용자 정보를 꺼냄. PrincipalDetails - 인증된 사용자 정보를 담아 SecurityContext에 저장되는 객체
        String accessToken = jwtUtil.createAccessToken(principal.getUser().getId());
        String refreshToken = jwtUtil.createRefreshToken();

        // Refresh Token -> HttpOnly 쿠키로 전달
        ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(true) // HTTPS일 경우 true
                .path("/")
                .maxAge(Duration.ofDays(14))
                .sameSite("None") // 크로스 도메인 대응
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());


        // 액세스 토큰은 클라이언트에 JSON으로 응답
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"accessToken\": \"" + accessToken + "\"}");
    }
}
