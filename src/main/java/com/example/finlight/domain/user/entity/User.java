package com.example.finlight.domain.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users") // 테이블명 단수로 통일 -> 하려고 했으나!! PostgreSQL에 user는 키워드라 구문 오류 뜸. users로 변경.
@Getter   // 읽기 전용 getter만 제공 (캡슐화 원칙)
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 요구사항, 외부 생성 방지
public class User {
    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "email", nullable = false, length = 100)   // 이메일
    private String email;

    @Column(name = "nickname", nullable = false, unique = true, length = 50)
    private String nickname;

    @Column(name = "password", nullable = false, length = 225)
    private String password;

    @Column(name = "username", nullable = false, unique = true, length = 300)
    private String username;  // ex: "google_123456", "local_abc@gmail.com"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;     // ROLE_USER, ROLE_ADMIN

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp // Hibernate가 자동으로 현재 시간 설정
    private LocalDateTime createdAt;

    // 비즈니스 로직을 포함한 정적 팩토리 메서드 (객체지향 원칙)
    public static User createUser(String email, String nickname, String password, String username, Role role) {
        User user = new User();
        user.email = email;
        user.nickname = nickname;
        user.password = password;
        user.username = username;
        user.role = role;
        // @CreationTimestamp 사용 시 제거해도 됨
        // user.createdAt = LocalDateTime.now();
        return user;
    }

    public static User createOAuthUser(String email, String nickname, String username, String password) {
        return createUser(email, nickname, password, username, Role.USER);
    }

    public void changeNickname(String newNickname) {
        this.nickname = newNickname;
    }

    // 소셜 로그인 사용자면 비밀번호 변경 기능 숨기기 등
    public boolean isOAuthUser() {
        return !username.startsWith("local_");
    }

}