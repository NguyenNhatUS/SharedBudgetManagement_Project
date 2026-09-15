package com.nhat.SharedBudgetManagement.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.function.Function;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PageResponse<T> {

    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;
    private boolean empty;
    private String sortBy;
    private String sortDirection;


    public static <T> PageResponse<T> from(Page<T> page) {
        String sortBy = null;
        String sortDirection = null;

        if (page.getSort().isSorted()) {
            Sort.Order order = page.getSort().iterator().next();
            sortBy = order.getProperty();
            sortDirection = order.getDirection().name();
        }

        return PageResponse.<T>builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .empty(page.isEmpty())
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();
    }


    public static <T, R> PageResponse<R> from(Page<T> page, Function<T, R> mapper) {
        String sortBy = null;
        String sortDirection = null;

        if (page.getSort().isSorted()) {
            Sort.Order order = page.getSort().iterator().next();
            sortBy = order.getProperty();
            sortDirection = order.getDirection().name();
        }

        List<R> mappedContent = page.getContent().stream()
                .map(mapper)
                .toList();

        return PageResponse.<R>builder()
                .content(mappedContent)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .empty(page.isEmpty())
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();
    }
}