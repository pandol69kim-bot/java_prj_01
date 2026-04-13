package com.example.app.api.auth;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(
        @NotBlank(message = "refreshToken을 입력해주세요.")
        String refreshToken
) {}
