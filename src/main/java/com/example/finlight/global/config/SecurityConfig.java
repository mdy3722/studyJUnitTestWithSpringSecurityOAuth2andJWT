package com.example.finlight.global.config;

import com.example.finlight.global.auth.*;
import com.example.finlight.global.auth.jwt.JwtAuthenticationFilter;
import com.example.finlight.global.auth.jwt.JwtUtil;
import com.example.finlight.global.auth.oauth.CustomOAuth2UserService;
import com.example.finlight.global.auth.oauth.OAuth2SuccessHandler;
import com.example.finlight.global.redis.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;


@Configuration     // 이 클래스는 설정 파일이다.
@RequiredArgsConstructor    // final 필드들을 자동으로 생성자 주입
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, AuthenticationManager authenticationManager, RefreshTokenService refreshTokenService) throws Exception {

        JwtLoginFilter jwtLoginFilter = new JwtLoginFilter(authenticationManager, jwtUtil, refreshTokenService); // DI 아닌 직접 생성 (JwtLoginFilter는 Bean 등록을 안하므로)

        return http
                // JWT + OAuth2 인증 방식이므로
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                // 세션 사용 X - 매 요청마다 토큰으로 인증
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/swagger-resources/**").permitAll()
                        .requestMatchers("/", "/api/users", "/oauth2/**", "/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/users", "/api/users/refresh").permitAll()   // 일반 회원가입, 토큰 재발행
                        .anyRequest().authenticated()     // 그 외 요청은 인증 필요
                )

                // /api/** 에는 302 대신 401/403을 주도록 강제
                .exceptionHandling(ex -> ex
                        .defaultAuthenticationEntryPointFor(
                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),           // 인증 없음 → 401
                                new AntPathRequestMatcher("/api/**")
                        )
                        .accessDeniedHandler(new AccessDeniedHandlerImpl())        // 인가 실패 → 403
                )

                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))    // 유저 정보 가져오기
                        .successHandler(oAuth2SuccessHandler)    // 로그인 성공 이후 처리
                )

                // JWT 인증 필터: 매 요청의 Authorization 헤더에 담긴 JWT를 검증하고,
                // 유효하면 SecurityContext에 인증 객체를 저장한다.
                // UsernamePasswordAuthenticationFilter 실행 전에 동작해야,
                // 로그인 요청을 제외한 나머지 요청에서 JWT 기반 인증이 먼저 처리됨.
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                // 로그인 처리 필터: 기본 UsernamePasswordAuthenticationFilter를 교체하여
                // /login 요청의 JSON 바디(email, password)를 읽고 인증을 수행한 뒤 JWT를 발급한다.
                // 기존 HTML Form 로그인 방식 대신 REST API 방식으로 로그인 흐름을 완전히 대체한다.
                .addFilterAt(jwtLoginFilter, UsernamePasswordAuthenticationFilter.class)

                .build();
    }

    // AuthenticationManager Bean 등록 - JwtloginFilter에서 사용되므로
    // 일반 로그인 시 인증 처리 - 내부적으로 UserDetailsService와 PasswordEncoder 등을 활용하여 사용자 인증을 수행한다.
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

}
