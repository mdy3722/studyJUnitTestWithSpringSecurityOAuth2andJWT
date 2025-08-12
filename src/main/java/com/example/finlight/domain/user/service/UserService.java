package com.example.finlight.domain.user.service;

import com.example.finlight.domain.user.dto.req.UserSignupDTO;
import com.example.finlight.domain.user.dto.res.UserResponseDTO;
import com.example.finlight.domain.user.entity.Role;
import com.example.finlight.domain.user.entity.User;
import com.example.finlight.domain.user.repository.UserRepository;
import com.example.finlight.global.auth.jwt.JwtUtil;
import com.example.finlight.global.exception.CustomException;
import com.example.finlight.global.exception.ErrorCode;
import com.example.finlight.global.redis.RefreshTokenService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public UserResponseDTO originalSignUp(UserSignupDTO req) {
        String email = req.getEmail();
        String username = "local_" + email;

        if (userRepository.findByUsername(username).isPresent())
            throw new CustomException(ErrorCode.DUPLICATE_USER);
        if (userRepository.existsByNickname(req.getNickname()))
            throw new CustomException(ErrorCode.USER_NOT_FOUND);

        User user = User.createUser(
                email,
                req.getNickname(),
                passwordEncoder.encode(req.getPassword()),
                username,
                Role.USER
        );

        userRepository.save(user);
        return new UserResponseDTO(user.getId(), user.getEmail(), user.getNickname());

    }

    public ResponseEntity<?> reissueToken(String refreshToken, HttpServletResponse response) {
        if (refreshToken == null)
            return ResponseEntity.status(401).body("No refresh token");

        if (!jwtUtil.isTokenValid(refreshToken))
            return ResponseEntity.status(401).body("Invalid refresh token");

        UUID userId = jwtUtil.extractUserId(refreshToken);
        if (!refreshTokenService.verifyRefreshToken(userId, refreshToken))
            return ResponseEntity.status(401).body("Mismatch/expired refresh token");

        // 로테이션
        refreshTokenService.delete(userId);
        String newRefresh = jwtUtil.createRefreshToken(userId);
        refreshTokenService.save(userId, newRefresh, Duration.ofDays(14));

        String accessToken = jwtUtil.createAccessToken(userId);

        // 쿠키 세팅
        ResponseCookie cookie = ResponseCookie.from("refreshToken", newRefresh)
                .httpOnly(true)
                .path("/")
                .maxAge(Duration.ofDays(14))
                .secure(false) // 로컬 개발 시
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        // userId와 accessToken을 함께 반환
        return ResponseEntity.ok(Map.of(
                "accessToken", accessToken,
                "userId", userId
        ));
    }

    public ResponseEntity<?> logout(UUID userId, HttpServletResponse response) {
            refreshTokenService.delete(userId); // RT 제거

        // 캐시 금지
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");

        // 쿠키 제거 (maxAge=0)
        ResponseCookie deleteCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, deleteCookie.toString());
        return ResponseEntity.ok().build();
    }

    public ResponseEntity<?> me(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "nickname", user.getNickname()
        ));
    }
}
