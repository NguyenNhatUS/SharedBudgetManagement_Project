package com.nhat.SharedBudgetManagement.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class PageResponseTest {

    @Test
    @DisplayName("Should convert paginated and sorted Page to PageResponse successfully")
    void testFromPageWithSorting() {
        List<String> items = List.of("Budget A", "Budget B");
        PageRequest pageRequest = PageRequest.of(0, 10, Sort.by("createdAt").descending());
        Page<String> page = new PageImpl<>(items, pageRequest, 25);

        PageResponse<String> response = PageResponse.from(page);

        assertNotNull(response);
        assertEquals(2, response.getContent().size());
        assertEquals(0, response.getPage());
        assertEquals(10, response.getSize());
        assertEquals(25, response.getTotalElements());
        assertEquals(3, response.getTotalPages());
        assertTrue(response.isFirst());
        assertFalse(response.isLast());
        assertFalse(response.isEmpty());
        assertEquals("createdAt", response.getSortBy());
        assertEquals("DESC", response.getSortDirection());
    }

    @Test
    @DisplayName("Should convert Page with mapper function to PageResponse successfully")
    void testFromPageWithMapper() {
        List<Integer> ids = List.of(1, 2, 3);
        PageRequest pageRequest = PageRequest.of(1, 3, Sort.by("name").ascending());
        Page<Integer> page = new PageImpl<>(ids, pageRequest, 6);

        PageResponse<String> response = PageResponse.from(page, id -> "ID-" + id);

        assertNotNull(response);
        assertEquals(List.of("ID-1", "ID-2", "ID-3"), response.getContent());
        assertEquals(1, response.getPage());
        assertEquals(3, response.getSize());
        assertEquals(6, response.getTotalElements());
        assertEquals(2, response.getTotalPages());
        assertFalse(response.isFirst());
        assertTrue(response.isLast());
        assertFalse(response.isEmpty());
        assertEquals("name", response.getSortBy());
        assertEquals("ASC", response.getSortDirection());
    }

    @Test
    @DisplayName("Should convert empty and unsorted Page to PageResponse successfully")
    void testFromEmptyPageUnsorted() {
        Page<String> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10, Sort.unsorted()), 0);

        PageResponse<String> response = PageResponse.from(emptyPage);

        assertNotNull(response);
        assertTrue(response.getContent().isEmpty());
        assertEquals(0, response.getPage());
        assertEquals(10, response.getSize());
        assertEquals(0, response.getTotalElements());
        assertEquals(0, response.getTotalPages());
        assertTrue(response.isFirst());
        assertTrue(response.isLast());
        assertTrue(response.isEmpty());
        assertNull(response.getSortBy());
        assertNull(response.getSortDirection());
    }
}
