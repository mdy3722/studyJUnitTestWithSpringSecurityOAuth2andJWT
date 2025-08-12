package com.example.originalandsocialloginapi.global.auth;

import com.example.originalandsocialloginapi.domain.user.entity.User;
import com.example.originalandsocialloginapi.domain.user.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

// 사용자 정보 조회
@Service
public class PrincipalDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public PrincipalDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // JwtLoginFilter에서 넘어온 "local_email" 형태의 username 받음

        // 1. local_ 접두사가 있는지 확인 (일반 로그인 사용자인지)
        if (!username.startsWith("local_")) {
            throw new UsernameNotFoundException("올바르지 않은 사용자 이름입니다.");
        }

        // 2. DB에서 해당 username으로 User 찾기
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("해당 사용자를 찾을 수 없습니다."));

        // 3. User를 PrincipalDetails로 감싸서 반환
        //    → 이게 AuthenticationManager로 다시 넘어감
        return new PrincipalDetails(user);
    }
}
