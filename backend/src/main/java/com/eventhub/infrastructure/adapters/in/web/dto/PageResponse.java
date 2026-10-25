package com.eventhub.infrastructure.adapters.in.web.dto;

import com.eventhub.domain.model.PageResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.function.Function;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PageResponse<T> {

    private List<T> content;
    private int pageNumber;
    private int pageSize;
    private long totalElements;
    private int totalPages;
    private boolean last;

    public static <D, R> PageResponse<R> fromPageResult(PageResult<D> pageResult, Function<D, R> mapper) {
        List<R> items = pageResult.getContent().stream()
                .map(mapper)
                .toList();

        return PageResponse.<R>builder()
                .content(items)
                .pageNumber(pageResult.getPageNumber())
                .pageSize(pageResult.getPageSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .last(pageResult.isLast())
                .build();
    }
}
