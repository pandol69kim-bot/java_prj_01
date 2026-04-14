package com.example.app.api.syncdata;

import com.example.app.domain.sync.ExternalDataSync;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "동기화 데이터 응답")
public record SyncDataResponse(

        @Schema(description = "레코드 ID")
        Long id,

        @Schema(description = "외부 시스템 코드")
        String systemCode,

        @Schema(description = "외부 참조 ID")
        String externalRefId,

        @Schema(description = "처리 상태")
        String status,

        @Schema(description = "원본 페이로드 (외부 시스템 수신 원문)")
        String rawPayload,

        @Schema(description = "처리된 페이로드")
        String processedPayload,

        @Schema(description = "오류 메시지")
        String errorMessage,

        @Schema(description = "재시도 횟수")
        int retryCount,

        @Schema(description = "동기화 완료 일시")
        LocalDateTime syncedAt,

        @Schema(description = "생성 일시")
        LocalDateTime createdAt
) {
    public static SyncDataResponse from(ExternalDataSync sync) {
        return new SyncDataResponse(
                sync.getId(),
                sync.getExternalSystem().getSystemCode(),
                sync.getExternalRefId(),
                sync.getStatus().name(),
                sync.getRawPayload(),
                sync.getProcessedPayload(),
                sync.getErrorMessage(),
                sync.getRetryCount(),
                sync.getSyncedAt(),
                sync.getCreatedAt()
        );
    }
}
