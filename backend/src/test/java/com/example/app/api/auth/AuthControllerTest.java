package com.example.app.api.auth;

import com.example.app.common.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@ActiveProfiles("test")
@Import(com.example.app.infrastructure.security.SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private com.example.app.infrastructure.security.JwtTokenProvider jwtTokenProvider;

    @MockBean
    private com.example.app.infrastructure.security.CustomUserDetailsService customUserDetailsService;

    @MockBean
    private org.springframework.security.authentication.AuthenticationManager authenticationManager;

    @Test
    void 올바른_자격증명으로_로그인하면_토큰을_반환한다() throws Exception {
        // Arrange
        TokenResponse expected = TokenResponse.of("access-token", "refresh-token", 3600L);
        given(authService.login("user1", "pass")).willReturn(expected);

        LoginRequest request = new LoginRequest("user1", "pass");

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"));
    }

    @Test
    void username이_빈_값이면_400을_반환한다() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest("", "pass");

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 잘못된_자격증명이면_401을_반환한다() throws Exception {
        // Arrange
        given(authService.login(anyString(), anyString()))
                .willThrow(BusinessException.unauthorized("아이디 또는 비밀번호가 올바르지 않습니다."));

        LoginRequest request = new LoginRequest("user1", "wrong");

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void 유효한_리프레시_토큰으로_토큰을_갱신한다() throws Exception {
        // Arrange
        TokenResponse expected = TokenResponse.of("new-access", "new-refresh", 3600L);
        given(authService.refresh("valid-refresh")).willReturn(expected);

        RefreshRequest request = new RefreshRequest("valid-refresh");

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("new-access"));
    }

    @Test
    void 로그아웃하면_200을_반환한다() throws Exception {
        // Arrange
        willDoNothing().given(authService).logout("some-refresh-token");

        RefreshRequest request = new RefreshRequest("some-refresh-token");

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
