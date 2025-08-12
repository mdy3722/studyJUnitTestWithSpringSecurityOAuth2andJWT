package com.example.originalandsocialloginapi.global.exception;

import lombok.Getter;

@Getter
public class ErrorResponse {
    
    private final int status;
    private final String message;
    
    public ErrorResponse(int status, String message) {
        this.status = status;
        this.message = message;
    }

}

// 위와 같이 작성해도 되고
// public record ErrorResponse(int status, String message){}로 작성해도 됨
