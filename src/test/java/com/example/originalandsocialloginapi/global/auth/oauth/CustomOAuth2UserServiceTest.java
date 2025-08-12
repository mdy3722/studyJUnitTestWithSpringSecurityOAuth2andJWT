package com.example.originalandsocialloginapi.global.auth.oauth;

import com.example.originalandsocialloginapi.domain.user.entity.User;
import com.example.originalandsocialloginapi.domain.user.repository.UserRepository;
import com.example.originalandsocialloginapi.global.auth.PrincipalDetails;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// CustomOAuth2UserService 단위 테스트
// 실제 구글/카카오 API 호출 없이 CustomOAuth2UserService의 로직만을 검증
@ExtendWith(MockitoExtension.class) // Mockito를 사용하기 위한 확장 설정
class CustomOAuth2UserServiceTest {

    // @Mock : 가짜 객체(Mock)를 만들어 줌. 실제 DB나 외부 의존성 없이 테스트 가능
    @Mock
    private UserRepository userRepository;     // 실제 DB와 상호작용 하지 않음

    @Mock
    private PasswordEncoder passwordEncoder;   // 실제 인코딩 로직을 실행하지 않음

    // @InjectMocks: 위의 Mock 객체들을 자동으로 주입해서 실제 테스트할 서비스 객체를 생성
    @InjectMocks
    private CustomOAuth2UserService customOAuth2UserService;

    // [테스트 시나리오 1: 신규 사용자가 구글로 로그인할 때]
    @Test
    @DisplayName("구글로 신규 회원가입 시, 새로운 User 객체를 생성하여 저장하고 반환한다.")
    void signUpWithGoogleSuccess() throws OAuth2AuthenticationException {
        // [Given - 테스트 데이터/환경을 준비]

        // 필요한 Mock 객체를 생성
        OAuth2UserRequest oAuth2UserRequest = mock(OAuth2UserRequest.class);    // 소셜 로그인 요청 정보를 담고 있는 객체
        OAuth2User oAuth2User = mock(OAuth2User.class);

        // Mock 객체의 동작을 정의
        when(oAuth2UserRequest.getClientRegistration()).thenReturn(mock(ClientRegistration.class));
        when(oAuth2UserRequest.getClientRegistration().getRegistrationId()).thenReturn("google");   // google을 반환하도록 설정. provider에 google 저장

        CustomOAuth2UserService spyService = spy(customOAuth2UserService);
        doReturn(oAuth2User).when(spyService).loadUserFromProvider(oAuth2UserRequest);

        // oAuth2User.getAttributes()가 반환할 가짜 사용자 속성(attributes)을 설정
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("sub", "123456789");
        attributes.put("email", "test@gmail.com");
        attributes.put("name", "홍길동");
        when(oAuth2User.getAttributes()).thenReturn(attributes);

        // UserRepository가 기존 유저를 찾지 못하도록 설정
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());

        // PasswordEncoder가 어떤 문자열을 인코딩해도 "encoded_password"를 반환하도록 설정
        when(passwordEncoder.encode(anyString())).thenReturn("encoded_password");

        // [When - 테스트 대상 메소드 실행]
        OAuth2User result = spyService.loadUser(oAuth2UserRequest);

        // [Then - 결과 검증]
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        // userRepository.findByUsername() 메소드가 "google_123456789"를 인자로 한 번 호출되었는지 확인
        verify(userRepository, times(1)).findByUsername(eq("google_123456789"));

        // userRepository의 save()메소드가 한 번 호출, 이때 전달된 User 객체를 userCaptor에 담기
        verify(userRepository, times(1)).save(userCaptor.capture());

        // capturedUser - userRepository.save()에 전달된 User 객체
        User capturedUser = userCaptor.getValue();

        // Repository에 제대로 값이 전달되었는가
        assertThat(capturedUser.getUsername()).isEqualTo("google_123456789");
        assertThat(capturedUser.getEmail()).isEqualTo("test@gmail.com");
        assertThat(capturedUser.getNickname()).isEqualTo("홍길동");
        assertThat(capturedUser.getPassword()).isEqualTo("encoded_password");

        // 최종 반환된 PrincipalDetails 객체의 username 필드 확인
        assertThat(result).isInstanceOf(PrincipalDetails.class);
        assertThat(((PrincipalDetails) result).getUsername()).isEqualTo("google_123456789");
    }

    @Test
    @DisplayName("카카오로 기존 회원이 로그인하면 새로운 유저를 저장하지 않고 반환한다.")
    void loginWithKakaoSuccess() throws OAuth2AuthorizationException {
        // Given
        OAuth2UserRequest oAuth2UserRequest = mock(OAuth2UserRequest.class);
        OAuth2User oAuth2User = mock(OAuth2User.class);

        User existedUser = User.createOAuthUser("test@naver.com", "홍길동", "kakao_123456", "encoded_password");

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", 123456);
        attributes.put("kakao_account", Map.of("email", "test@naver.com", "profile", Map.of("nickname", "홍길동")));

        when(oAuth2UserRequest.getClientRegistration()).thenReturn(mock(ClientRegistration.class));
        when(oAuth2UserRequest.getClientRegistration().getRegistrationId()).thenReturn("kakao");

        when(userRepository.findByUsername(eq("kakao_123456"))).thenReturn(Optional.of(existedUser));

        CustomOAuth2UserService spyService = spy(customOAuth2UserService);
        doReturn(oAuth2User).when(spyService).loadUserFromProvider(oAuth2UserRequest);

        when(oAuth2User.getAttributes()).thenReturn(attributes);

        // When
        OAuth2User result = spyService.loadUser(oAuth2UserRequest);

        // Then
        verify(userRepository, times(1)).findByUsername(eq("kakao_123456"));
        verify(userRepository, never()).save(any(User.class));
        assertThat(((PrincipalDetails) result).getUsername()).isEqualTo("kakao_123456");

    }

    @Test
    @DisplayName("지원하지 않는 소셜 로그인이면 예외를 던진다.")
    void unsupportedLoginProviderThrowsException() {
        // Given
        OAuth2UserRequest oAuth2UserRequest = mock(OAuth2UserRequest.class);
        OAuth2User oAuth2User = mock(OAuth2User.class);

        when(oAuth2UserRequest.getClientRegistration()).thenReturn(mock(ClientRegistration.class));
        when(oAuth2UserRequest.getClientRegistration().getRegistrationId()).thenReturn("kbBank");   // provider에 "kakao", "google"이 아닌 값이 들어감

        CustomOAuth2UserService spyService = spy(customOAuth2UserService);
        doReturn(oAuth2User).when(spyService).loadUserFromProvider(oAuth2UserRequest);

        // When + Then
        // loadUser 메소드 실행 시 OAuth2AuthenticationException이 발생하는지 검증
        OAuth2AuthenticationException exception = assertThrows(OAuth2AuthenticationException.class, () -> {
            spyService.loadUser(oAuth2UserRequest);
        });

        // 발생한 예외의 메시지가 예상과 일치하는지 확인
        assertEquals("지원하지 않는 소셜 로그인입니다.", exception.getError().getErrorCode());

    }

}