package com.example.finlight.domain.user.service;

import com.example.finlight.domain.user.dto.req.UserSignupDTO;
import com.example.finlight.domain.user.dto.res.UserResponseDTO;
import com.example.finlight.domain.user.entity.Role;
import com.example.finlight.domain.user.entity.User;
import com.example.finlight.domain.user.repository.UserRepository;
import com.example.finlight.global.dto.LoginRequestDTO;
import com.example.finlight.global.redis.RefreshTokenService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)   // 매 테스트 메서드 시작 전 스프링 컨텍스트, H2 스키마가 초기화 됨
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = "classpath:application-test.properties")
@DisplayName("사용자 통합테스트")
public class UserIntegrationTest {
    // @Mock이 아닌 진짜 의존성 주입
    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @LocalServerPort
    private int port;

    // 테스트용 사용자 데이터
    private UserSignupDTO userSignupDTO;
    private LoginRequestDTO loginRequestDTO;

    @BeforeEach
    void setUp() {
        // 각 테스트 전 데이터 정리
        userRepository.deleteAll();

        // 테스트 데이터 준비
        userSignupDTO = new UserSignupDTO("test@example.com", "테스터", "password123");

        loginRequestDTO = new LoginRequestDTO("test@example.com", "password123");
    }

    @Test
    @DisplayName("1. 회원가입 성공")
    void 회원가입_성공_테스트() {
        // Given: 회원가입 요청 데이터 준비
        // 요청 헤더 객체 생성
        HttpHeaders headers = new HttpHeaders();
        // 이 요청은 JSON 형식이라는 Content-Type 지정
        headers.setContentType(MediaType.APPLICATION_JSON);
        // 헤더와 바디를 묶어 최종 요청 객체 만들기
        HttpEntity<UserSignupDTO> request = new HttpEntity<>(userSignupDTO, headers);

        // When: 실제 HTTP POST 요청으로 회원가입
        ResponseEntity<UserResponseDTO> response = restTemplate.postForEntity(
                createURL("/api/users"),
                request,
                UserResponseDTO.class   // 서버가 돌려준 JSON 응답 body를 UserResponseDTO 타입으로 역직렬화
        );

        // Then: HTTP 응답 검증
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // Location 헤더 검증 (자원 위치 정보)
        String locationHeader = response.getHeaders().getFirst("Location");
        assertThat(locationHeader).contains("/api/users/");

        // 응답 데이터 검증
        UserResponseDTO responseBody = response.getBody();
        assertThat(responseBody).isNotNull();
        assertThat(responseBody.email()).isEqualTo("test@example.com");
        assertThat(responseBody.nickname()).isEqualTo("테스터");
        assertThat(responseBody.id()).isNotNull(); // UUID 자동 생성됨

        // 데이터베이스 저장 검증
        Optional<User> savedUser = userRepository.findByUsername("local_test@example.com");
        assertThat(savedUser).isPresent();

        User user = savedUser.get();
        assertThat(user.getEmail()).isEqualTo("test@example.com");
        assertThat(user.getNickname()).isEqualTo("테스터");
        assertThat(user.getUsername()).isEqualTo("local_test@example.com"); // local_ 접두사 확인
        assertThat(user.getRole()).isEqualTo(Role.USER);
        assertThat(user.isOAuthUser()).isFalse(); // 일반 회원가입이므로 OAuth 아님

        // 비밀번호 암호화 검증
        assertThat(passwordEncoder.matches("password123", user.getPassword())).isTrue();

        System.out.println("✅ 회원가입 성공 - 사용자 ID: " + user.getId());
    }

    @Test
    @DisplayName("2. 로그인 성공 - JWT 토큰 발급 및 쿠키 설정")
    void 로그인_성공_JWT_발급_테스트() {
        // Given: 먼저 사용자 회원가입
        User testUser = createTestUser();

        // 로그인 요청 준비
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<LoginRequestDTO> loginEntity = new HttpEntity<>(loginRequestDTO, headers);

        // When: 실제 로그인 API 호출
        ResponseEntity<String> response = restTemplate.postForEntity(
                createURL("/login"),
                loginEntity,
                String.class
        );

        // Then: 로그인 성공 검증
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Set-Cookie 헤더 검증 (Refresh Token)
        List<String> cookies = response.getHeaders().get("Set-Cookie");
        assertThat(cookies).isNotNull();
        String refreshTokenCookie = cookies.get(0);

        assertThat(refreshTokenCookie).contains("refreshToken=");
        assertThat(refreshTokenCookie).contains("HttpOnly");
        assertThat(refreshTokenCookie).contains("Path=/");
        assertThat(refreshTokenCookie).contains("Max-Age=1209600"); // 14일
        assertThat(refreshTokenCookie).contains("SameSite=Lax");

        // Refresh Token 값만 파싱 후 Redis에 저장된 토큰과 비교 - Redis에 토큰이 잘 저장되었는지 확인하기 위함
        String refreshToken = extractCookieValue(refreshTokenCookie, "refreshToken");
        UUID uid = testUser.getId();
        assertThat(refreshTokenService.verifyRefreshToken(uid, refreshToken)).isTrue();

        // 응답 본문 검증
        String responseBody = response.getBody();
        assertThat(responseBody).contains("accessToken");
        assertThat(responseBody).contains("userId");

        // JSON 파싱해서 토큰 추출
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonResponse;

        try {
            jsonResponse = mapper.readTree(responseBody);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new RuntimeException("응답 JSON 파싱 실패: " + responseBody, e);
        }

        String accessToken = jsonResponse.get("accessToken").asText();
        assertThat(accessToken).isNotBlank();

        System.out.println("✅ 로그인 성공 - Access Token : " + accessToken);
        System.out.println("✅ 사용자 ID : " + uid);
    }

    @Test
    @DisplayName("3. JWT 토큰으로 인증된 API 접근 - 내 정보 조회")
    void JWT_인증_내정보_조회_테스트() {
        // Given: 사용자 생성 및 로그인으로 토큰 획득
        User testUser = createTestUser();
        String accessToken = loginAndGetAccessToken();

        // Authorization 헤더에 JWT 토큰 설정
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        HttpEntity<?> request = new HttpEntity<>(headers);

        // When: 인증이 필요한 API 호출
        // getForEntity : 헤더를 못 넣음. exchange : 헤더를 넣으면서 GET요청 가능
        ResponseEntity<String> response = restTemplate.exchange(
                createURL("/api/users/me"),
                HttpMethod.GET,
                request,
                String.class
        );

        // Then: 인증된 접근 성공 검증
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        // 내 정보 응답 데이터 검증
        String responseBody = response.getBody();
        assertThat(responseBody).contains("test@example.com");
        assertThat(responseBody).contains("테스터");
        assertThat(responseBody).contains(testUser.getId().toString());

        // JSON 상세 검증
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonResponse;

        try {
            jsonResponse = mapper.readTree(responseBody);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new RuntimeException("응답 JSON 파싱 실패: " + responseBody, e);
        }

        assertThat(jsonResponse.get("id").asText()).isEqualTo(testUser.getId().toString());
        assertThat(jsonResponse.get("email").asText()).isEqualTo("test@example.com");
        assertThat(jsonResponse.get("nickname").asText()).isEqualTo("테스터");

        System.out.println("✅ JWT 인증 성공 - 내 정보 조회 완료");
    }

    @Test
    @DisplayName("4. Refresh Token으로 Access Token 재발급")
    void 토큰_재발급_성공_테스트() {
        // Given: 로그인해서 Refresh Token 쿠키 획득
        createTestUser();
        String refreshTokenCookie = loginAndGetRefreshTokenCookie();

        // 쿠키 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.set("Cookie", refreshTokenCookie);
        HttpEntity<?> request = new HttpEntity<>(headers);

        // When: 토큰 재발급 API 호출
        ResponseEntity<String> response = restTemplate.postForEntity(
                createURL("/api/users/refresh"),
                request,
                String.class
        );

        // Then: 재발급 성공 검증
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        // 새로운 Access Token 발급 확인
        String responseBody = response.getBody();
        assertThat(responseBody).contains("accessToken");
        assertThat(responseBody).contains("userId");

        // 새로운 Refresh Token 쿠키 설정 확인 (로테이션)
        List<String> newCookies = response.getHeaders().get("Set-Cookie");
        assertThat(newCookies).isNotNull();
        assertThat(newCookies.get(0)).contains("refreshToken=");
    }

    @Test
    @DisplayName("5. 로그아웃 - 쿠키 삭제 및 Refresh Token 무효화")
    void 로그아웃_성공_테스트() {
        // Given: 로그인 상태의 사용자
        User testUser = createTestUser();

        // 로그인해서 토큰 확보
        String accessToken = loginAndGetAccessToken();
        String refreshCookie = loginAndGetRefreshTokenCookie();
        String refreshToken  = extractCookieValue(refreshCookie, "refreshToken");

        // JWT 토큰으로 인증된 로그아웃 요청
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        HttpEntity<?> request = new HttpEntity<>(headers);

        // When: 로그아웃 API 호출
        ResponseEntity<Void> response = restTemplate.postForEntity(
                createURL("/api/users/logout"),
                request,
                Void.class
        );

        // Then: 로그아웃 성공 검증
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        // 쿠키 삭제 확인 (Max-Age=0)
        List<String> cookies = response.getHeaders().get("Set-Cookie");
        assertThat(cookies).isNotNull();

        String deleteCookie = cookies.get(0);
        assertThat(deleteCookie).contains("refreshToken="); // 빈 값으로 설정
        assertThat(deleteCookie).contains("Max-Age=0"); // 즉시 만료

        // Redis에 RT 무효화 확인
        assertThat(refreshTokenService.verifyRefreshToken(testUser.getId(), refreshToken)).isFalse();

        System.out.println("✅ 로그아웃 성공 - Refresh Token 삭제됨");
    }

    @Test
    @DisplayName("6. 중복 이메일 회원가입 실패")
    void 중복_이메일_회원가입_실패_테스트() {
        // Given: 이미 가입된 사용자
        createTestUser();

        // 동일한 이메일로 재가입 시도
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<UserSignupDTO> request = new HttpEntity<>(userSignupDTO, headers);

        // When: 중복 이메일로 회원가입 시도
        ResponseEntity<String> response = restTemplate.postForEntity(
                createURL("/api/users"),
                request,
                String.class
        );

        // Then: 회원가입 실패 (400)
        assertThat(response.getStatusCode()).isIn(HttpStatus.BAD_REQUEST, HttpStatus.CONFLICT);

        // DB에 중복 사용자가 생성되지 않았는지 확인
        List<User> users = userRepository.findAll();
        assertThat(users).hasSize(1); // 기존 1명만 있어야 함

        System.out.println("✅ 중복 이메일 회원가입 차단됨");
    }

    /** ======================= 헬퍼 메서드들 ======================= */
    /**
     * 테스트용 URL 생성
     */
    private String createURL(String path) {
        return "http://localhost:" + port + path;
    }

    /**
     * 테스트용 사용자 생성 (일반 로그인)
     */
    private User createTestUser() {
        User user = User.createUser(
                "test@example.com",
                "테스터",
                passwordEncoder.encode("password123"),
                "local_test@example.com",
                Role.USER
        );

        return userRepository.save(user);
    }

    /**
     * 로그인해서 Access Token 반환
     */
    private String loginAndGetAccessToken() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<LoginRequestDTO> request = new HttpEntity<>(loginRequestDTO, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    createURL("/login"),
                    request,
                    String.class
            );

            ObjectMapper mapper = new ObjectMapper();   // Json -> 객체로 변환 (ObjectMapper : Jackson에서 제공하는 JSON 파서)
            JsonNode jsonResponse = mapper.readTree(response.getBody());   // .readTree(String) : 문자열(JSON 텍스트)을 트리 구조(JsonNode) 로 파싱해 줌
            return jsonResponse.get("accessToken").asText();   // .get("accessToken") : JSON에서 "accessToken" 키에 해당하는 값 찾음. .asText() : JsonNode -> 문자열로 변환

        } catch (Exception e) {
            throw new RuntimeException("로그인 실패", e);
        }
    }

    /**
     * 로그인해서 Refresh Token 쿠키 반환
     */
    private String loginAndGetRefreshTokenCookie() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<LoginRequestDTO> request = new HttpEntity<>(loginRequestDTO, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                createURL("/login"),
                request,
                String.class
        );

        List<String> cookies = response.getHeaders().get("Set-Cookie");
        return cookies.get(0); // refreshToken 쿠키 반환
    }

    /**
     * 쿠키에서 Refresh Token 값만 파싱
     */
    private String extractCookieValue(String setCookie, String name) {
        if (setCookie == null) return null;
        for (String part : setCookie.split(";")) {
            String t = part.trim();
            if (t.startsWith(name + "=")) return t.substring((name + "=").length());
        }
        return null;
    }

    @AfterEach
    void tearDown() {
        System.out.println("🧹 테스트 완료 - 데이터 롤백됨");
    }
}