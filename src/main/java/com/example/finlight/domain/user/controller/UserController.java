package com.example.finlight.domain.user.controller;

import com.example.finlight.domain.user.dto.req.UserSignupDTO;
import com.example.finlight.domain.user.dto.res.UserResponseDTO;
import com.example.finlight.domain.user.service.UserService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController    // @Controller + @ResponseBody - 뷰 렌더링이 아닌 HTTP 본문(JSON/문자열 등) 을 바로 반환하는 API용 컨트롤러
@RequestMapping("/api/users")
@AllArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<UserResponseDTO> signUp(@Valid @RequestBody UserSignupDTO req) {
        UserResponseDTO res = userService.originalSignUp(req);
        return ResponseEntity
                .created(URI.create("/api/users/" + res.id()))  // 자원의 위치를 알려주는 Location 헤더
                .body(res);
    }
}
