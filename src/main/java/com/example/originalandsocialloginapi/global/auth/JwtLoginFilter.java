package com.example.originalandsocialloginapi.global.auth;

import com.example.originalandsocialloginapi.domain.user.entity.User;
import com.example.originalandsocialloginapi.global.auth.jwt.JwtUtil;
import com.example.originalandsocialloginapi.global.dto.LoginRequestDTO;
import com.example.originalandsocialloginapi.global.redis.RefreshTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

// 일반 로그인 처리
public class JwtLoginFilter extends UsernamePasswordAuthenticationFilter {

    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

    public JwtLoginFilter(AuthenticationManager authenticationManager, JwtUtil jwtUtil, RefreshTokenService refreshTokenService) {
        this.jwtUtil = jwtUtil;
        this.refreshTokenService = refreshTokenService;
        setAuthenticationManager(authenticationManager);
    }

    // 로그인 시도: JSON 요청을 파싱해서 Authentication 객체 생성
    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            LoginRequestDTO loginRequest = objectMapper.readValue(request.getInputStream(), LoginRequestDTO.class);

            String username = "local_" + loginRequest.getEmail();
            String password = loginRequest.getPassword();

            // UsernamePasswordAuthenticationToken 생성
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(username, password);

            // AuthenticationManager에게 "이 사용자를 인증해달라"고 요청
            return getAuthenticationManager().authenticate(authToken);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // 위 과정에서 AuthenticationManager가 PrincipalDetail로 감싸진 사용자 정보를 받아, 비밀번호 검증 후 인증 성공하면 JWT 토큰 발급 및 응답
    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                            FilterChain chain, Authentication authResult) throws IOException {
        // 인증된 사용자 정보 추출
        PrincipalDetails principal = (PrincipalDetails) authResult.getPrincipal();
        User user = principal.getUser();
        UUID userId = user.getId();

        // JWT 토큰 생성
        String accessToken = jwtUtil.createAccessToken(userId);
        String refreshToken = jwtUtil.createRefreshToken(userId);
        Duration refreshTtl = jwtUtil.getRefreshTtl();

        // Redis에 Refresh Token을 저장
        refreshTokenService.save(userId, refreshToken, refreshTtl);

        // Refresh Token -> HttpOnly 쿠키로 전달
        ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(false) // HTTPS일 경우 true
                .path("/")
                .maxAge(Duration.ofDays(14))
                .sameSite("Lax") // 크로스 도메인 대응 : Strict - 같은 사이트 요청에서만 전송 / Lax - 같은 사이트, 일부 안전한 탐색에서 전송 / None : 같은 사이트, 크로스 사이트 모두 전송(Secure true 설정 필수)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());

        // 액세스 토큰은 userId와 함께 클라이언트에 JSON으로 응답
        response.setStatus(HttpServletResponse.SC_OK); // 200 ok
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");  // 토큰이 포함된 응답이므로 캐시 금지
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        response.getWriter().printf("{\"accessToken\":\"%s\",\"userId\":\"%s\"}", accessToken, userId);
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                              AuthenticationException failed) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"error\": \"로그인 실패: " + failed.getMessage() + "\"}");
    }
}
