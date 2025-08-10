package com.example.finlight.domain.user.service;

import com.example.finlight.domain.user.dto.req.UserSignupDTO;
import com.example.finlight.domain.user.dto.res.UserResponseDTO;
import com.example.finlight.domain.user.entity.Role;
import com.example.finlight.domain.user.entity.User;
import com.example.finlight.domain.user.repository.UserRepository;
import com.example.finlight.global.exception.CustomException;
import com.example.finlight.global.exception.ErrorCode;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponseDTO originalSignUp(UserSignupDTO req) {
        String email = req.getEmail();
        String username = "local_" + email;

        if (userRepository.findByUsername(username).isPresent())
            throw new CustomException(ErrorCode.DUPLICATE_USER);
        if (userRepository.existsByNickname(req.getNickname()))
            throw new CustomException(ErrorCode.USER_NOT_FOUND);

        User user = User.createUser(
                email,
                req.getNickname(),
                passwordEncoder.encode(req.getPassword()),
                username,
                Role.USER
        );

        userRepository.save(user);
        return new UserResponseDTO(user.getId(), user.getEmail(), user.getNickname());

    }
}
