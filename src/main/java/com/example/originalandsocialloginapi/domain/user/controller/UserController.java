package com.example.originalandsocialloginapi.domain.user.controller;

import com.example.originalandsocialloginapi.domain.user.dto.req.UserSignupDTO;
import com.example.originalandsocialloginapi.domain.user.dto.res.UserResponseDTO;
import com.example.originalandsocialloginapi.domain.user.service.UserService;
import com.example.originalandsocialloginapi.global.auth.PrincipalDetails;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController    // @Controller + @ResponseBody - 뷰 렌더링이 아닌 HTTP 본문(JSON/문자열 등) 을 바로 반환하는 API용 컨트롤러
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<UserResponseDTO> signUp(@Valid @RequestBody UserSignupDTO req) {
        UserResponseDTO res = userService.originalSignUp(req);
        return ResponseEntity
                .created(URI.create("/api/users/" + res.id()))  // 자원의 위치를 알려주는 Location 헤더
                .body(res);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> reissueToken(@CookieValue(name = "refreshToken", required = false) String refreshToken, HttpServletResponse response) {
        return userService.reissueToken(refreshToken, response);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@AuthenticationPrincipal UUID userId,
                                    HttpServletResponse response) {
        return userService.logout(userId, response);
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(@AuthenticationPrincipal UUID userId) {
        return userService.me(userId);
    }

}
