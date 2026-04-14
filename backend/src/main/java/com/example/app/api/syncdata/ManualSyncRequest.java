package com.example.app.api.syncdata;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "동기화 데이터 수동 입력 요청")
public record ManualSyncRequest(

        @Schema(description = "외부 시스템 ID", example = "1")
        @NotNull(message = "외부 시스템 ID를 입력해주세요.")
        Long systemId,

        @Schema(description = "외부 참조 ID", example = "REF-2024-001")
        @NotBlank(message = "외부 참조 ID를 입력해주세요.")
        @Size(max = 200, message = "외부 참조 ID는 200자 이하여야 합니다.")
        String externalRefId,

        @Schema(description = "처리 상태", example = "COMPLETED",
                allowableValues = {"PENDING", "PROCESSING", "COMPLETED", "FAILED", "SKIPPED"})
        String status,

        @Schema(description = "원본 페이로드 (JSON 등)", example = "{\"key\":\"value\"}")
        @Size(max = 10000, message = "페이로드는 10,000자 이하여야 합니다.")
        String rawPayload,

        @Schema(description = "오류 메시지 (FAILED 상태 시)")
        @Size(max = 1000, message = "오류 메시지는 1,000자 이하여야 합니다.")
        String errorMessage
) {}
