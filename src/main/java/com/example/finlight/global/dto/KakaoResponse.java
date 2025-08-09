package com.example.finlight.global.dto;

import org.springframework.security.oauth2.core.OAuth2AuthenticationException;

import java.util.Map;
import java.util.UUID;

public class KakaoResponse implements OAuth2Response{

    private final Map<String, Object> attribute;

    public KakaoResponse(Map<String, Object> attribute) {
        this.attribute = attribute;
    }

    @Override
    public String getProvider() {
        return "kakao";
    }

    @Override
    public String getProviderId() {
        return String.valueOf(attribute.get("id"));   // id는 Long타입이므로 String으로 직접 캐스팅 할 수 없음. 그래서 String.valueOf()를 쓰고 이를 쓰는 이유는 내부적으로 toString()을 호출하기 때문 + nullPointerException오류도 방지한다(null이 들어오면 "null" 문자열 반환), attribute.get("id").toString(); 방식은 NullPointerException 발생 가능
    }

    @SuppressWarnings("unchecked")
    @Override
    public String getEmail() {
        Map<String, Object> kakaoAccount = (Map<String, Object>) attribute.get("kakao_account");
        String email = (String) kakaoAccount.get("email");
        if (email == null || email.isEmpty()) {
            throw new OAuth2AuthenticationException("카카오 계정에서 이메일 정보를 가져올 수 없습니다.");
        }
        return email;
    }

    @SuppressWarnings("unchecked")
    @Override
    public String getNickname() {
        Map<String, Object> kakaoAccount = (Map<String, Object>) attribute.get("kakao_account");
        Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
        String nickname = (String) profile.get("nickname");
        if (nickname == null || nickname.isEmpty()) {
            return "kakaoUser_" + UUID.randomUUID().toString().substring(0, 8);
        }
        return nickname;
    }
}


// Kakao OAuth2 응답 구조
//{
//    "id": 123456789,
//    "kakao_account": {
//        "email": "abc@kakao.com",
//        "profile":{
//            "nickname":"홍길동"
//        }
//    }
//}