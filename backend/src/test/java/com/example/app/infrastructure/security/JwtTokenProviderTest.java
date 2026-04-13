package com.example.app.infrastructure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private JwtTokenProvider provider;

    @BeforeEach
    void setUp() {
        // 32자 이상 비밀키 필요
        provider = new JwtTokenProvider(
                "test-secret-key-must-be-at-least-32-chars",
                3_600_000L,   // 1시간
                604_800_000L  // 7일
        );
    }

    @Test
    void 액세스_토큰을_생성하고_검증한다() {
        // Arrange & Act
        String token = provider.generateAccessToken("user1", "ROLE_USER");

        // Assert
        assertThat(provider.validateToken(token)).isTrue();
        assertThat(provider.isAccessToken(token)).isTrue();
        assertThat(provider.extractUsername(token)).isEqualTo("user1");
        assertThat(provider.extractRole(token)).isEqualTo("ROLE_USER");
    }

    @Test
    void 리프레시_토큰을_생성하고_타입을_구분한다() {
        // Arrange & Act
        String token = provider.generateRefreshToken("user1");

        // Assert
        assertThat(provider.validateToken(token)).isTrue();
        assertThat(provider.isRefreshToken(token)).isTrue();
        assertThat(provider.isAccessToken(token)).isFalse();
    }

    @Test
    void 잘못된_토큰은_검증에_실패한다() {
        assertThat(provider.validateToken("invalid.token.value")).isFalse();
    }

    @Test
    void 빈_토큰은_검증에_실패한다() {
        assertThat(provider.validateToken("")).isFalse();
    }

    @Test
    void 다른_키로_서명한_토큰은_검증에_실패한다() {
        JwtTokenProvider otherProvider = new JwtTokenProvider(
                "other-secret-key-must-be-at-least-32-chars",
                3_600_000L, 604_800_000L
        );
        String token = otherProvider.generateAccessToken("user1", "ROLE_USER");

        assertThat(provider.validateToken(token)).isFalse();
    }
}
