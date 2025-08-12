package com.example.originalandsocialloginapi.domain.user.dto.req;

import jakarta.validation.constraints.*;
import lombok.Getter;

@Getter
public class UserSignupDTO {
    @Email(message = "올바른 이메일 형식이 아닙니다")
    @NotBlank(message = "이메일은 필수입니다")
    @Size(max = 100)
    private String email;

    @NotBlank(message = "닉네임은 필수입니다")
    @Size(min = 1, max = 50)
    @Pattern(regexp = "^[가-힣a-zA-Z0-9_]+$", message = "특수문자 사용 불가")
    private String nickname;

    @NotBlank(message = "비밀번호는 필수입니다")
    @Size(min = 8, max = 225, message = "비밀번호는 8자 이상")
    private String password;
}
