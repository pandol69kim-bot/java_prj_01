package com.example.app.api.syncdata;

import com.example.app.common.dto.PageResponse;
import com.example.app.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Sync Data", description = "동기화 데이터 조회 API")
@RestController
@RequestMapping("/api/v1/sync-data")
@RequiredArgsConstructor
public class SyncDataController {

    private final SyncDataQueryService syncDataQueryService;

    @Operation(summary = "시스템별 동기화 데이터 목록 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<SyncDataResponse>>> list(
            @Parameter(description = "외부 시스템 ID", required = true)
            @RequestParam Long systemId,
            @Parameter(description = "페이지 번호 (0부터)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "정렬 기준 (createdAt|syncedAt)") @RequestParam(defaultValue = "createdAt") String sort,
            @Parameter(description = "정렬 방향 (desc|asc)") @RequestParam(defaultValue = "desc") String direction) {

        Sort.Direction dir = "asc".equalsIgnoreCase(direction)
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        PageRequest pageable = PageRequest.of(page, size, Sort.by(dir, sort));

        PageResponse<SyncDataResponse> result =
                PageResponse.from(syncDataQueryService.findBySystem(systemId, pageable));
        return ResponseEntity.ok(result.toApiResponse());
    }

    @Operation(summary = "동기화 데이터 단건 조회")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SyncDataResponse>> getById(
            @Parameter(description = "레코드 ID") @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(syncDataQueryService.findById(id)));
    }
}
