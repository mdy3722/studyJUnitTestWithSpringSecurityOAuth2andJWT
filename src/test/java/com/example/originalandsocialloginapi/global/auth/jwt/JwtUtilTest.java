package com.example.originalandsocialloginapi.global.auth.jwt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// junit 단위 테스트

/**
 *  개별 메소드나 클래스의 가장 작은 단위를 독립적으로 테스트 하는 것
 *  다른 의존성 없이 해당 로직만 검증 가능
 */
@ExtendWith(MockitoExtension.class)  // Mockito를 사용하기 위한 JUnit 확장
@DisplayName("JWT 유틸 클래스 테스트")    // 테스트 클래스에 대한 설명
class JwtUtilTest {

    private JwtUtil jwtUtil;

    // 테스트용 비밀키 (실제 운영에서는 더 복잡해야 함)
    // 테스트용 설정값들
    private final String TEST_SECRET_KEY = "mySecretKeyForTestingJwtTokenGenerationAndValidation12345678901234567890"; // 64바이트 이상
    private final long TEST_ACCESS_TOKEN_VALIDITY = 3600; // 1시간 (초 단위)
    private final long TEST_REFRESH_TOKEN_VALIDITY = 1209600; // 2주 (초 단위)

    @BeforeEach   // 각 테스트 메소드 실행 전에 먼저 실행
    void setUp() {
        jwtUtil = new JwtUtil(     // 테스트용 JwtUtil 객체를 생성하고 초기화
                TEST_SECRET_KEY,
                TEST_ACCESS_TOKEN_VALIDITY,
                TEST_REFRESH_TOKEN_VALIDITY
        );
    }

    @Test
    @DisplayName("사용자 ID로 액세스 토큰이 정상 생성되어야 한다.")
    void createAccessTokenTest() {
        // Given - 테스트용 데이터/환경 준비 (사용자 Id 준비)
        UUID testUserId = UUID.randomUUID();

        // When - 테스트 실행 (액세스 토큰 생성)
        String accessToken = jwtUtil.createAccessToken(testUserId);

        // Then - 테스트 검증
        // assertThat : AssertJ 라이브러리의 메소드, 실제 코드에서 기대한 결과값(ex. 테스트케이스)과 실제 테스트 결과가 같은지 등을 확인해주는 메소드
        // JUnit의 assertEquals()와 비교했을 때, 가독성/체이닝/메소드 다양성 측면에서 좋다.
        assertThat(accessToken)
                .isNotNull()      // null이 아닌지
                .isNotEmpty()     // 빈 문자열인지 아닌지
                .contains(".");    // JWT 형식 - header.payload.signature

        String[] tokenParts = accessToken.split("\\.");
        assertThat(tokenParts).hasSize(3);
    }

    @Test
    @DisplayName("리프레시 토큰이 정상 생성되어야 한다.")
    void createRefreshTokenTest() {
        // When - 리프레시 토큰 생성
        String refreshToken = jwtUtil.createRefreshToken();

        // Then - 테스트 검증
        assertThat(refreshToken)
                .isNotNull()
                .isNotEmpty()
                .contains(".");

        String[] tokenParts = refreshToken.split("\\.");
        assertThat(tokenParts).hasSize(3);
    }

    @Test
    @DisplayName("생성한 토큰에서 원래 사용자 Id를 추출할 수 있어야 한다.")
    void extractUserIdTest() {
        // Given - 테스트용 사용자 Id와 토큰 준비
        UUID originalUserId = UUID.randomUUID();
        String testToken = jwtUtil.createAccessToken(originalUserId);

        // When - 액세스 토큰으로부터 사용자 Id를 추출
        UUID extractedUserId = jwtUtil.extractUserId(testToken);

        // Then - originalUserId와 추출한 extractedUserId를 비교
        assertThat(extractedUserId).isEqualTo(originalUserId);
    }

    @Test
    @DisplayName("토큰의 유효성을 검증한다.")
    void isTokenValidTest() {
        // Given - 유효성을 검증할 토큰을 준비
        UUID userId = UUID.randomUUID();
        String testToken = jwtUtil.createAccessToken(userId);

        // When - 토큰 유효성 검증
        boolean isValid = jwtUtil.isTokenValid(testToken);

        // Then - 유효성 검증이 성공했는지를 검증
        assertThat(isValid).isTrue();
    }
}