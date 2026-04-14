package com.example.app.api.syncdata;

import com.example.app.common.dto.PageResponse;
import com.example.app.common.response.ApiResponse;
import com.example.app.domain.sync.ExternalDataSync;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Set;

@Tag(name = "Sync Data", description = "동기화 데이터 조회 API")
@RestController
@RequestMapping("/api/v1/sync-data")
@RequiredArgsConstructor
public class SyncDataController {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("createdAt", "syncedAt", "status", "externalRefId");

    private final SyncDataQueryService syncDataQueryService;
    private final ExcelExportService excelExportService;

    @Operation(summary = "시스템별 동기화 데이터 목록 조회 (페이징·정렬·필터)")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<SyncDataResponse>>> list(
            @Parameter(description = "외부 시스템 ID", required = true)
            @RequestParam Long systemId,
            @Parameter(description = "페이지 번호 (0부터)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "정렬 기준 (createdAt|syncedAt|status|externalRefId)")
            @RequestParam(defaultValue = "createdAt") String sort,
            @Parameter(description = "정렬 방향 (desc|asc)") @RequestParam(defaultValue = "desc") String direction,
            @Parameter(description = "상태 필터 (PENDING|PROCESSING|COMPLETED|FAILED|SKIPPED)")
            @RequestParam(required = false) String status,
            @Parameter(description = "외부 참조 ID 검색 (부분 일치)")
            @RequestParam(required = false) String refId) {

        String safeSort = ALLOWED_SORT_FIELDS.contains(sort) ? sort : "createdAt";
        Sort.Direction dir = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        PageRequest pageable = PageRequest.of(page, Math.min(size, 100), Sort.by(dir, safeSort));

        ExternalDataSync.SyncStatus syncStatus = parseStatus(status);
        PageResponse<SyncDataResponse> result =
                PageResponse.from(syncDataQueryService.findWithFilters(systemId, syncStatus, refId, pageable));

        return ResponseEntity.ok(result.toApiResponse());
    }

    @Operation(summary = "동기화 데이터 엑셀 다운로드 (최대 10,000건)")
    @GetMapping("/export")
    public void exportExcel(
            @Parameter(description = "외부 시스템 ID", required = true) @RequestParam Long systemId,
            @Parameter(description = "상태 필터") @RequestParam(required = false) String status,
            @Parameter(description = "외부 참조 ID 검색") @RequestParam(required = false) String refId,
            HttpServletResponse response) throws IOException {

        String filename = URLEncoder.encode("sync-data.xlsx", StandardCharsets.UTF_8);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + filename);

        ExternalDataSync.SyncStatus syncStatus = parseStatus(status);
        excelExportService.exportSyncData(systemId, syncStatus, refId, response.getOutputStream());
    }

    @Operation(summary = "동기화 데이터 수동 입력 (ADMIN, 테스트/데모용)")
    @PostMapping("/manual")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SyncDataResponse>> createManual(
            @Valid @RequestBody ManualSyncRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(syncDataQueryService.createManual(request)));
    }

    @Operation(summary = "동기화 데이터 단건 조회")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SyncDataResponse>> getById(
            @Parameter(description = "레코드 ID") @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(syncDataQueryService.findById(id)));
    }

    private ExternalDataSync.SyncStatus parseStatus(String status) {
        if (status == null || status.isBlank()) return null;
        try {
            return ExternalDataSync.SyncStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
