package com.example.finlight.global.dto.oauth;

import java.util.Map;
import java.util.UUID;

// OAuth2 provider(Google)의 응답을 파싱해서 필요한 데이터만 추출
public class GoogleResponse implements OAuth2Response {

    private final Map<String, Object> attribute;   // JSON 키-값 관리, 다양한 타입 값을 하나의 Map에 담기 위해

    public GoogleResponse(Map<String, Object> attribute) {
        this.attribute = attribute;
    }

    @Override
    public String getProvider() {
        return "google";
    }

    @Override
    public String getProviderId() {
        return (String) attribute.get("sub");   // attribute.get("sub")는 Object 타입을 반환하므로 String으로 명시적 형변환
    }

    @Override
    public String getEmail() {
        return (String) attribute.get("email");
    }

    @Override
    public String getNickname() {
        String nickname = (String) attribute.get("name");
        if (nickname == null || nickname.isEmpty()) {
            return "google_user_" + UUID.randomUUID().toString().substring(0,8);
        }
        return nickname;
    }
}


// Google OAuth2 응답 구조
//{
//    "sub": "1234567890",
//    "name": "홍길동",
//    "email": "hong@gmail.com",
//    "picture": "https://...",
//    ...
//}
