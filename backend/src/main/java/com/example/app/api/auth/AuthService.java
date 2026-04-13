package com.example.app.api.auth;

import com.example.app.common.exception.BusinessException;
import com.example.app.domain.auth.RefreshToken;
import com.example.app.domain.auth.RefreshTokenRepository;
import com.example.app.infrastructure.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.expiration-ms}")
    private long expirationMs;

    @Value("${jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    @Transactional
    public TokenResponse login(String username, String password) {
        Authentication authentication = authenticate(username, password);

        String role = authentication.getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .orElse("ROLE_USER");

        String accessToken = jwtTokenProvider.generateAccessToken(username, role);
        String refreshToken = jwtTokenProvider.generateRefreshToken(username);

        // 기존 토큰 교체 (1인 1토큰)
        refreshTokenRepository.findByUsername(username)
                .ifPresentOrElse(
                        existing -> refreshTokenRepository.save(existing.rotate(refreshToken, refreshExpirationMs)),
                        () -> refreshTokenRepository.save(RefreshToken.of(username, refreshToken, refreshExpirationMs))
                );

        return TokenResponse.of(accessToken, refreshToken, expirationMs / 1000);
    }

    @Transactional
    public TokenResponse refresh(String refreshTokenValue) {
        if (!jwtTokenProvider.validateToken(refreshTokenValue)
                || !jwtTokenProvider.isRefreshToken(refreshTokenValue)) {
            throw BusinessException.unauthorized("유효하지 않은 리프레시 토큰입니다.");
        }

        RefreshToken stored = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> BusinessException.unauthorized("리프레시 토큰을 찾을 수 없습니다."));

        if (stored.isExpired()) {
            refreshTokenRepository.delete(stored);
            throw BusinessException.unauthorized("리프레시 토큰이 만료되었습니다. 다시 로그인해주세요.");
        }

        String username = jwtTokenProvider.extractUsername(refreshTokenValue);
        // DB에서 사용자 정보를 다시 조회해 최신 role 반영은 UserDetailsService 통해 처리
        String newAccessToken = jwtTokenProvider.generateAccessToken(username, "ROLE_USER");
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(username);

        refreshTokenRepository.save(stored.rotate(newRefreshToken, refreshExpirationMs));

        return TokenResponse.of(newAccessToken, newRefreshToken, expirationMs / 1000);
    }

    @Transactional
    public void logout(String refreshTokenValue) {
        refreshTokenRepository.findByToken(refreshTokenValue)
                .ifPresent(refreshTokenRepository::delete);
    }

    private Authentication authenticate(String username, String password) {
        try {
            return authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password));
        } catch (BadCredentialsException e) {
            throw BusinessException.unauthorized("아이디 또는 비밀번호가 올바르지 않습니다.");
        }
    }
}
