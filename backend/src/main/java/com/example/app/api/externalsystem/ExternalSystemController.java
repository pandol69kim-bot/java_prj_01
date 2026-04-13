package com.example.app.api.externalsystem;

import com.example.app.common.dto.PageResponse;
import com.example.app.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "External Systems", description = "외부 시스템 설정 관리 API")
@RestController
@RequestMapping("/api/v1/external-systems")
@RequiredArgsConstructor
public class ExternalSystemController {

    private final ExternalSystemService externalSystemService;

    @Operation(summary = "외부 시스템 목록 조회 (페이징)")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ExternalSystemResponse>>> list(
            @Parameter(description = "페이지 번호 (0부터)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size) {

        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        PageResponse<ExternalSystemResponse> result =
                PageResponse.from(externalSystemService.findAll(pageable));
        return ResponseEntity.ok(result.toApiResponse());
    }

    @Operation(summary = "활성 외부 시스템 전체 조회")
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<ExternalSystemResponse>>> listActive() {
        return ResponseEntity.ok(ApiResponse.ok(externalSystemService.findAllActive()));
    }

    @Operation(summary = "외부 시스템 단건 조회")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ExternalSystemResponse>> getById(
            @Parameter(description = "시스템 ID") @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(externalSystemService.findById(id)));
    }

    @Operation(summary = "외부 시스템 등록 (ADMIN)")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ExternalSystemResponse>> create(
            @Valid @RequestBody ExternalSystemRequest request) {
        ExternalSystemResponse created = externalSystemService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(created));
    }

    @Operation(summary = "API 키 갱신 (ADMIN)")
    @PatchMapping("/{id}/api-key")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ExternalSystemResponse>> updateApiKey(
            @Parameter(description = "시스템 ID") @PathVariable Long id,
            @RequestBody @NotBlank String newApiKey) {
        return ResponseEntity.ok(ApiResponse.ok(
                externalSystemService.updateApiKey(id, newApiKey)));
    }

    @Operation(summary = "외부 시스템 비활성화 (ADMIN)")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deactivate(
            @Parameter(description = "시스템 ID") @PathVariable Long id) {
        externalSystemService.deactivate(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
