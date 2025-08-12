package com.example.originalandsocialloginapi.global.auth.oauth;

import com.example.originalandsocialloginapi.domain.user.entity.User;
import com.example.originalandsocialloginapi.domain.user.repository.UserRepository;
import com.example.originalandsocialloginapi.global.auth.PrincipalDetails;
import com.example.originalandsocialloginapi.global.dto.oauth.GoogleResponse;
import com.example.originalandsocialloginapi.global.dto.oauth.KakaoResponse;
import com.example.originalandsocialloginapi.global.dto.oauth.OAuth2Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

// OAuth2 사용자 정보 로딩 -> 최종적으로 OAuth2User를 반환해야 함 -> OAuth2User로 username을 생성하고 username을 통해 User를 DB에서 조회 -> 없다면 새로 유저 등록, 있다면 PrincipalDetails로 감싸서 반환
@Slf4j
@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public CustomOAuth2UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    protected OAuth2User loadUserFromProvider(OAuth2UserRequest request) {
        return super.loadUser(request);
    }

    // 소셜 로그인이 인증되면 스프링이 내부적으로 호출하는 메소드
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // 어떤 소셜 로그인인지 (ex. kakao, google 등)
        String provider = userRequest.getClientRegistration().getRegistrationId();
        log.debug("[OAUTH] start loadUser: regId={}", provider);

        // 기본 OAuth2UserService를 통해 사용자 정보 받아오기
        OAuth2User oAuth2User = loadUserFromProvider(userRequest);

        OAuth2Response oAuth2Response = null;
        
        // provider 별로 응답 형태가 다르므로 통일된 인터페이스로 반환
        if (provider.equals("google")) {
            oAuth2Response = new GoogleResponse(oAuth2User.getAttributes());
        }
        else if (provider.equals("kakao")) {
            oAuth2Response = new KakaoResponse(oAuth2User.getAttributes());
        }
        else {
            throw new OAuth2AuthenticationException("지원하지 않는 소셜 로그인입니다.");
        }

        String username = provider + "_" + oAuth2Response.getProviderId();

        // 기존 유저 찾기
        User user = userRepository.findByUsername(username).orElse(null);
        
        // 없으면 새로 저장
        if (user == null) {
            user = User.createOAuthUser(
                    oAuth2Response.getEmail(),
                    oAuth2Response.getNickname(),
                    username,
                    passwordEncoder.encode("oauth2")  // 더미 PW
            );
            System.out.println("[OAUTH] before save username=" + user.getUsername());
            userRepository.save(user);
            System.out.println("[OAUTH] saved id=" + user.getId() + ", saved username=" + user.getUsername());
        }

        log.debug("[OAUTH] end loadUser");
        // 7. 반환: 소셜 사용자 → PrincipalDetails로 감싸서 반환 -> OAuth2LoginAuthenticationFilter로 돌아감
        return new PrincipalDetails(user, oAuth2User.getAttributes());

    }

}
