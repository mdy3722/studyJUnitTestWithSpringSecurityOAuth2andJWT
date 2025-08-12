package com.example.originalandsocialloginapi.global.redis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
public class RefreshTokenService {
    private final RedisTemplate<String, Object> redisTemplate;
    private static final String KEY_PREFIX = "auth:rt:";

    @Autowired
    public RefreshTokenService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private String keyOf(UUID userId) {
        return KEY_PREFIX + userId;
    }

    // 저장 (TTL은 refresh 토큰 만료와 동일하게)
    public void save(UUID userId, String refreshToken, Duration ttl) {
        String key = keyOf(userId);
        redisTemplate.opsForValue().set(key, refreshToken, ttl);
    }

    // 일치 여부 확인 (재발급 시 검증)
    public boolean verifyRefreshToken(UUID userId, String providedToken) {
        Object saved = redisTemplate.opsForValue().get(keyOf(userId));
        return saved != null && providedToken.equals(saved.toString());
    }

    // 로그아웃/재발급 시 기존 토큰 제거
    public void delete(UUID userId) {
        redisTemplate.delete(keyOf(userId));
    }
}
