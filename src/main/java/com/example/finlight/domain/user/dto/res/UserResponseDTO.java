package com.example.finlight.domain.user.dto.res;

import lombok.Getter;
import org.hibernate.annotations.Target;

// record : 데이터를 불변하게 담기 위한 목적
// record는 기본 생성자가 없다. 모든 필드의 값을 받는 생성자만 있다.
// record는 Setter가 없다.
public record  UserResponseDTO(java.util.UUID id, String email, String nickname) {
}
