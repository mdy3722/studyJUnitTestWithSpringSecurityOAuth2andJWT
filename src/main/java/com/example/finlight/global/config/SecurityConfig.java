package com.example.finlight.global.config;

import com.example.finlight.global.auth.*;
import com.example.finlight.global.auth.jwt.JwtAuthenticationFilter;
import com.example.finlight.global.auth.jwt.JwtUtil;
import com.example.finlight.global.auth.oauth.CustomOAuth2UserService;
import com.example.finlight.global.auth.oauth.OAuth2SuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;


@Configuration     // 이 클래스는 설정 파일이다.
@RequiredArgsConstructor    // final 필드들을 자동으로 생성자 주입
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final JwtUtil jwtUtil;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        AuthenticationManager authenticationManager = http.getSharedObject(AuthenticationManager.class);

        JwtLoginFilter jwtLoginFilter = new JwtLoginFilter(authenticationManager, jwtUtil); // DI 아닌 직접 생성 (JwtLoginFilter는 Bean 등록을 안하므로)
        jwtLoginFilter.setFilterProcessesUrl("/api/auth/login");    // 설정 안하면 기본 uri는 /login

        return http
                // JWT + OAuth2 인증 방식이므로
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                // 세션 사용 X - 매 요청마다 토큰으로 인증
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/auth/**", "/oauth2/**").permitAll()
                        .anyRequest().authenticated()     // 그 외 요청은 인증 필요
                )

                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))    // 유저 정보 가져오기
                        .successHandler(oAuth2SuccessHandler)    // 로그인 성공 이후 처리
                )

                // JWT 인증 필터: 매 요청마다 JWT 헤더를 검증하여 인증 처리 -> 토큰 없으면 null인 상태로 다음 필터로 넘김
                .addFilterBefore(jwtAuthenticationFilter, JwtLoginFilter.class)
                // 일반 로그인 필터: ID/PW 로그인 요청 (/api/auth/login) 처리, JWT 발급
                .addFilterBefore(jwtLoginFilter, UsernamePasswordAuthenticationFilter.class)

                .build();
    }

    // AuthenticationManager Bean 등록 - JwtloginFilter에서 사용되므로
    // 일반 로그인 시 인증 처리 - 내부적으로 UserDetailsService와 PasswordEncoder 등을 활용하여 사용자 인증을 수행한다.
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}
