package com.example.app.common.dto;

import com.example.app.common.response.ApiResponse;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * 페이징 응답 공통 래퍼.
 * Spring Data Page 를 ApiResponse 형식으로 변환한다.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }

    public static <S, T> PageResponse<T> from(Page<S> page, Function<S, T> mapper) {
        return new PageResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }

    public ApiResponse<PageResponse<T>> toApiResponse() {
        ApiResponse.PageMeta meta = new ApiResponse.PageMeta(totalElements, page, size);
        return ApiResponse.ok(this, meta);
    }
}
