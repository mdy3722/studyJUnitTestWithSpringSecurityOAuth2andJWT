package com.example.finlight.global.auth;

import com.example.finlight.domain.user.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.*;

@Getter    // PrincipalDetails.getUser를 위해
public class PrincipalDetails implements UserDetails, OAuth2User {

    private final User user;
    private final Map<String, Object> attributes; // OAuth2 로그인 시 전달받은 정보

    // 일반 로그인용 생성자
    public PrincipalDetails(User user) {
        this.user = user;
        this.attributes = Collections.emptyMap();
    }

    // OAuth2 로그인용 생성자
    public PrincipalDetails(User user, Map<String, Object> attributes) {
        this.user = user;
        this.attributes = attributes;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    // 해당 User의 권한을 리턴
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return java.util.List.of(user.getRole());
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    // UserDetails 구현
    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        // 이건 언제 사용하는가?
        // 우리 사이트에서 1년 동안 회원이 로그인을 안하면 휴먼계정으로 전환하는 정책이 있다면
        // user.getLoginDate()등을 가져와서 "현재시간-로그인시간" => 1년 초과하면 return false
        return true;
    }

    // OAuth2User 구현
    @Override
    public String getName() {
        return user.getUsername();
    }

}
