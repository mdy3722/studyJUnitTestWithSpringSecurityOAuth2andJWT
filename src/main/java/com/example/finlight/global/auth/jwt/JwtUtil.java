package com.example.finlight.global.auth.jwt;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtil {
    private final Key secretKey;
    private final Duration accessTokenValidity;
    private final Duration refreshTokenValidity;

    @Autowired
    public JwtUtil(
            @Value("${jwt.secret}") String secretKey,
            @Value("${jwt.access-expiration}") Duration accessTokenValidity,
            @Value("${jwt.refresh-expiration}") Duration refreshTokenValidity
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        this.accessTokenValidity = accessTokenValidity;
        this.refreshTokenValidity = refreshTokenValidity;
    }

    public String createAccessToken(UUID userId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(userId.toString())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plus(accessTokenValidity)))
                .signWith(secretKey, SignatureAlgorithm.HS512)
                .compact();
    }

    public String createRefreshToken(UUID userId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(userId.toString())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plus(refreshTokenValidity)))
                .signWith(secretKey, SignatureAlgorithm.HS512)
                .compact();
    }

    public UUID extractUserId(String token) {
        return UUID.fromString(
                Jwts.parserBuilder().setSigningKey(secretKey).build()
                        .parseClaimsJws(token)
                        .getBody()
                        .getSubject()
        );
    }

    public boolean isTokenValid(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Duration getRefreshTtl() {
        return refreshTokenValidity;
    }
}
