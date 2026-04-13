package com.example.app.api.externalsystem;

import com.example.app.domain.externalsystem.ExternalSystem;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

public record ExternalSystemRequest(

        @Schema(description = "시스템 고유 코드 (영문 대문자/숫자/언더스코어)", example = "ERP_SYSTEM")
        @NotBlank(message = "시스템 코드를 입력해주세요.")
        @Pattern(regexp = "^[A-Z0-9_]{2,50}$",
                message = "시스템 코드는 영문 대문자, 숫자, 언더스코어 2~50자여야 합니다.")
        String systemCode,

        @Schema(description = "시스템 이름", example = "ERP 시스템")
        @NotBlank(message = "시스템 이름을 입력해주세요.")
        @Size(max = 100, message = "시스템 이름은 100자 이하여야 합니다.")
        String name,

        @Schema(description = "기본 URL", example = "https://api.erp.example.com")
        @NotBlank(message = "기본 URL을 입력해주세요.")
        @Pattern(regexp = "^https?://.*", message = "유효한 URL 형식이어야 합니다.")
        String baseUrl,

        @Schema(description = "인증 방식", example = "API_KEY")
        @NotNull(message = "인증 방식을 선택해주세요.")
        ExternalSystem.AuthType authType,

        @Schema(description = "API 키 또는 토큰")
        @Size(max = 500, message = "API 키는 500자 이하여야 합니다.")
        String apiKey,

        @Schema(description = "타임아웃(초)", example = "30")
        @Min(value = 1, message = "타임아웃은 1초 이상이어야 합니다.")
        @Max(value = 300, message = "타임아웃은 300초 이하여야 합니다.")
        Integer timeoutSeconds,

        @Schema(description = "최대 재시도 횟수", example = "3")
        @Min(value = 0, message = "재시도 횟수는 0 이상이어야 합니다.")
        @Max(value = 10, message = "재시도 횟수는 10 이하여야 합니다.")
        Integer retryMaxAttempts,

        @Schema(description = "시스템 설명")
        @Size(max = 500, message = "설명은 500자 이하여야 합니다.")
        String description
) {}
