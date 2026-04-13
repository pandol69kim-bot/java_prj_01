package com.example.app.api.externalsystem;

import com.example.app.domain.externalsystem.ExternalSystem;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "외부 시스템 응답")
public record ExternalSystemResponse(

        @Schema(description = "시스템 ID")
        Long id,

        @Schema(description = "시스템 코드")
        String systemCode,

        @Schema(description = "시스템 이름")
        String name,

        @Schema(description = "기본 URL")
        String baseUrl,

        @Schema(description = "인증 방식")
        String authType,

        @Schema(description = "타임아웃(초)")
        int timeoutSeconds,

        @Schema(description = "최대 재시도 횟수")
        int retryMaxAttempts,

        @Schema(description = "활성 여부")
        boolean active,

        @Schema(description = "설명")
        String description,

        @Schema(description = "생성일시")
        LocalDateTime createdAt,

        @Schema(description = "수정일시")
        LocalDateTime updatedAt
) {
    public static ExternalSystemResponse from(ExternalSystem es) {
        return new ExternalSystemResponse(
                es.getId(),
                es.getSystemCode(),
                es.getName(),
                es.getBaseUrl(),
                es.getAuthType().name(),
                es.getTimeoutSeconds(),
                es.getRetryMaxAttempts(),
                es.isActive(),
                es.getDescription(),
                es.getCreatedAt(),
                es.getUpdatedAt()
        );
    }
}
