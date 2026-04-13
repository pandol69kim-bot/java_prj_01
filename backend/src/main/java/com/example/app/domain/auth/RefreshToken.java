package com.example.app.domain.auth;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Refresh Token 저장 엔티티.
 * 1인 1토큰 정책 적용 — username 기준으로 최신 토큰만 유지한다.
 */
@Entity
@Table(name = "refresh_tokens", indexes = {
    @Index(name = "idx_refresh_tokens_username", columnList = "username", unique = true),
    @Index(name = "idx_refresh_tokens_token", columnList = "token", unique = true)
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 1000)
    private String token;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static RefreshToken of(String username, String token, long expirationMs) {
        RefreshToken rt = new RefreshToken();
        rt.username = username;
        rt.token = token;
        rt.createdAt = LocalDateTime.now();
        rt.expiresAt = rt.createdAt.plusSeconds(expirationMs / 1000);
        return rt;
    }

    public RefreshToken rotate(String newToken, long expirationMs) {
        this.token = newToken;
        this.expiresAt = LocalDateTime.now().plusSeconds(expirationMs / 1000);
        return this;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}
