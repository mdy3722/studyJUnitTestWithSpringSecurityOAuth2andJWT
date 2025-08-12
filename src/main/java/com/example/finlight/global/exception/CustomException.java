package com.example.finlight.global.exception;

import lombok.Getter;

// 개별 예외 클래스 대신, ErrorCode를 담을 수 있는 하나의 범용 커스텀 예외만 있어도 되는 효과
@Getter
public class CustomException extends RuntimeException{
    private final ErrorCode errorCode;

    public CustomException(ErrorCode errorCode) {
        super(errorCode.getMessage()); // 부모 클래스(RuntimeException)의 message 필드에 메시지를 설정
        this.errorCode = errorCode;
    }
}
