package com.example.finlight.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

// 도메인(USER, POST 등)별로 에러를 구분하고, 각 에러에 맞는 HTTP 상태 코드와 메시지를 Enum으로 한번에 관리
@Getter
public enum ErrorCode {
    // 예상치 못한 에러
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),

    // === User 도메인 에러 ===
    DUPLICATE_USER(HttpStatus.CONFLICT, "이미 가입된 회원입니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}
