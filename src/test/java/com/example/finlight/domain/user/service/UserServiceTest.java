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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    JwtUtil jwtUtil;

    @Mock
    RefreshTokenService refreshTokenService;

    @Mock
    HttpServletResponse response;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    @InjectMocks
    UserService userService;

    UserSignupDTO userSignupDTO;

    @BeforeEach
    void setUp() {
        userSignupDTO = new UserSignupDTO("test@example.com", "testNick", "password123");
    }

    @Test
    @DisplayName("일반 회원가입 성공")
    void originalSignUp() {
        // given
        String username = "local_test@example.com";
        UUID expectedId = UUID.randomUUID();

        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());
        when(userRepository.existsByNickname("testNick")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User userToSave = invocation.getArgument(0);

            // ReflectionTestUtils로 private 필드에 ID 설정
            // void setField(Object targetObject, String name, Object value)
            // 실제 JPA에서는 save() 끝나면 id 필드를 채우지만, 단위테스트에서 Mocking 할 때는 그럴 수 없어 id가 null이 된다.
            // 따라서 ReflectionTestUtils로 엔티티의 private 필드에 접근하여 값울 설정
            ReflectionTestUtils.setField(userToSave, "id", expectedId);
            return userToSave;
        });

        // when
        UserResponseDTO result = userService.originalSignUp(userSignupDTO);

        // then
        // 반환값 검증
        assertThat(result.id()).isNotNull();
        assertThat(result.email()).isEqualTo("test@example.com");
        assertThat(result.nickname()).isEqualTo("testNick");

        verify(userRepository).save(any(User.class));
        verify(passwordEncoder).encode("password123");
    }

    @Test
    @DisplayName("회원가입 실패 - 중복 사용자명")
    void originalSignUp_Fail_DuplicateUsername() {
        // given
        String username = "local_test@example.com";
        User existingUser = mock(User.class);
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(existingUser));

        // when & then
        assertThatThrownBy(() -> userService.originalSignUp(userSignupDTO))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_USER);
    }

    @Test
    @DisplayName("회원가입 실패 - 중복 닉네임")
    void originalSignUp_Fail_DuplicateNickname() {
        // given
        String username = "local_test@example.com";
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());
        when(userRepository.existsByNickname("testNick")).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> userService.originalSignUp(userSignupDTO))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_USER);
    }

    @Test
    @DisplayName("토큰 재발급 성공")
    void reissueToken_Success() {
        // given
        String refreshToken = "validRefreshToken";
        String newRefreshToken = "newRefreshToken";
        String accessToken = "newAccessToken";
        UUID mockUserId = UUID.randomUUID();

        when(jwtUtil.isTokenValid(refreshToken)).thenReturn(true);
        when(jwtUtil.extractUserId(refreshToken)).thenReturn(mockUserId);
        when(refreshTokenService.verifyRefreshToken(mockUserId, refreshToken)).thenReturn(true);
        when(jwtUtil.createRefreshToken(mockUserId)).thenReturn(newRefreshToken);
        when(jwtUtil.createAccessToken(mockUserId)).thenReturn(accessToken);

        // when
        ResponseEntity<?> result = userService.reissueToken(refreshToken, response);

        // then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) result.getBody();

        assertThat(body).isNotNull();
        assertThat(body.get("accessToken")).isEqualTo(accessToken);
        assertThat(body.get("userId")).isEqualTo(mockUserId);

        verify(refreshTokenService).delete(mockUserId);
        verify(refreshTokenService).save(eq(mockUserId), eq(newRefreshToken), any(Duration.class));
        verify(response).addHeader(eq(HttpHeaders.SET_COOKIE), anyString());
    }

    @Test
    @DisplayName("토큰 재발급 실패 - 토큰 없음")
    void reissueToken_Fail_NoToken() {
        // when
        ResponseEntity<?> result = userService.reissueToken(null, response);

        // then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(result.getBody()).isEqualTo("No refresh token");

        verify(jwtUtil, never()).isTokenValid(anyString());
        verify(refreshTokenService, never()).verifyRefreshToken(any(), anyString());
    }

    @Test
    @DisplayName("토큰 재발급 실패 - 유효하지 않은 토큰")
    void reissueToken_Fail_InvalidToken() {
        // given
        String refreshToken = "invalidToken";
        when(jwtUtil.isTokenValid(refreshToken)).thenReturn(false);

        // when
        ResponseEntity<?> result = userService.reissueToken(refreshToken, response);

        // then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(result.getBody()).isEqualTo("Invalid refresh token");

        verify(jwtUtil, never()).extractUserId(anyString());
        verify(refreshTokenService, never()).verifyRefreshToken(any(), anyString());
    }

    @Test
    @DisplayName("토큰 재발급 실패 - 토큰 불일치")
    void reissueToken_Fail_TokenMismatch() {
        // given
        String refreshToken = "mismatchToken";
        UUID mockUserId = UUID.randomUUID();

        when(jwtUtil.isTokenValid(refreshToken)).thenReturn(true);
        when(jwtUtil.extractUserId(refreshToken)).thenReturn(mockUserId);
        when(refreshTokenService.verifyRefreshToken(mockUserId, refreshToken)).thenReturn(false);

        // when
        ResponseEntity<?> result = userService.reissueToken(refreshToken, response);

        // then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(result.getBody()).isEqualTo("Mismatch/expired refresh token");

        verify(jwtUtil, never()).createRefreshToken(any());
        verify(jwtUtil, never()).createAccessToken(any());
    }

    @Test
    @DisplayName("로그아웃 성공")
    void logout_Success() {
        // given
        UUID mockUserId = UUID.randomUUID();

        // when
        ResponseEntity<?> result = userService.logout(mockUserId, response);

        // then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(refreshTokenService).delete(mockUserId);
        verify(response).setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        verify(response).addHeader(eq(HttpHeaders.SET_COOKIE), anyString());
    }

    @Test
    @DisplayName("사용자 정보 조회 성공")
    void me_Success() {
        // given
        UUID mockUserId = UUID.randomUUID();
        User mockUser = mock(User.class);
        when(mockUser.getId()).thenReturn(mockUserId);
        when(mockUser.getEmail()).thenReturn("test@example.com");
        when(mockUser.getNickname()).thenReturn("testNick");

        when(userRepository.findById(mockUserId)).thenReturn(Optional.of(mockUser));

        // when
        ResponseEntity<?> result = userService.me(mockUserId);

        // then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) result.getBody();

        assertThat(body).isNotNull();
        assertThat(body.get("email")).isEqualTo("test@example.com");
        assertThat(body.get("nickname")).isEqualTo("testNick");
        assertThat(body.get("id")).isEqualTo(mockUserId);

        verify(userRepository).findById(mockUserId);
    }

    @Test
    @DisplayName("사용자 정보 조회 실패 - 사용자 없음")
    void me_Fail_UserNotFound() {
        // given
        UUID mockUserId = UUID.randomUUID();
        when(userRepository.findById(mockUserId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.me(mockUserId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);

        verify(userRepository).findById(mockUserId);
    }
}