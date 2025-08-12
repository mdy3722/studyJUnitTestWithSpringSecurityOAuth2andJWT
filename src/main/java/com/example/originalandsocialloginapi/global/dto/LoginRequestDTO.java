package com.example.originalandsocialloginapi.global.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 일반 로그인 DTO
@Getter
@NoArgsConstructor    // JSON -> Java 객체로 바인딩되는 역직렬화를 위해 기본 생성자 핗요
public class LoginRequestDTO  {
    @Email(message = "올바른 이메일 형식이 아닙니다")
    @NotBlank(message = "이메일은 필수입니다")
    @Size(max = 100)
    private String email;

    @NotBlank(message = "비밀번호는 필수입니다")
    private String password;
}
