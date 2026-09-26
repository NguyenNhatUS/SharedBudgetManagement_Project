package com.nhat.SharedBudgetManagement.service;

import com.nhat.SharedBudgetManagement.entity.Tag;
import com.nhat.SharedBudgetManagement.exception.ConflictException;
import com.nhat.SharedBudgetManagement.exception.ResourceNotFoundException;
import com.nhat.SharedBudgetManagement.repository.TagRepository;
import com.nhat.SharedBudgetManagement.service.impl.TagServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests for TagService")
class TagServiceTest {

    @Mock
    private TagRepository tagRepository;

    @InjectMocks
    private TagServiceImpl tagService;

    private Tag testTag;

    @BeforeEach
    void setUp() {
        testTag = Tag.builder()
                .id(1L)
                .name("Food & Dining")
                .color("#FF5733")
                .icon("utensils")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("createTag - Success: should save and return new tag")
    void createTag_success() {
        when(tagRepository.existsByName("Travel")).thenReturn(false);
        when(tagRepository.save(any(Tag.class))).thenAnswer(invocation -> {
            Tag tag = invocation.getArgument(0);
            tag.setId(2L);
            return tag;
        });

        Tag result = tagService.createTag("Travel");

        assertNotNull(result);
        assertEquals("Travel", result.getName());
        assertEquals(2L, result.getId());
        verify(tagRepository).save(any(Tag.class));
    }

    @Test
    @DisplayName("createTag - Conflict: should throw ConflictException when tag name already exists")
    void createTag_conflict() {
        when(tagRepository.existsByName("Food & Dining")).thenReturn(true);

        assertThrows(ConflictException.class, () -> tagService
                .createTag("Food & Dining"));

        verify(tagRepository, never()).save(any(Tag.class));
    }

    @Test
    @DisplayName("updateTag - Success: should update tag name and save")
    void updateTag_success() {
        when(tagRepository.findById(1L)).thenReturn(Optional.of(testTag));
        when(tagRepository.save(any(Tag.class))).thenReturn(testTag);

        Tag result = tagService.updateTag(1L, "Food & Drinks");

        assertNotNull(result);
        assertEquals("Food & Drinks", result.getName());
        verify(tagRepository).save(testTag);
    }

    @Test
    @DisplayName("updateTag - Not Found: should throw ResourceNotFoundException when tag does not exist")
    void updateTag_notFound() {
        when(tagRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> tagService.updateTag(99L, "New Name"));
        verify(tagRepository, never()).save(any(Tag.class));
    }

    @Test
    @DisplayName("deleteTag - Success: should delete existing tag")
    void deleteTag_success() {
        when(tagRepository.findById(1L)).thenReturn(Optional.of(testTag));

        tagService.deleteTag(1L);

        verify(tagRepository).delete(testTag);
    }

    @Test
    @DisplayName("deleteTag - Not Found: should throw ResourceNotFoundException when tag does not exist")
    void deleteTag_notFound() {
        when(tagRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> tagService.deleteTag(99L));
        verify(tagRepository, never()).delete(any(Tag.class));
    }

    @Test
    @DisplayName("getTagById - Success: should return tag when found")
    void getTagById_success() {
        when(tagRepository.findById(1L)).thenReturn(Optional.of(testTag));

        Tag result = tagService.getTagById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Food & Dining", result.getName());
    }

    @Test
    @DisplayName("getTagById - Not Found: should throw ResourceNotFoundException")
    void getTagById_notFound() {
        when(tagRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> tagService.getTagById(99L));
    }

    @Test
    @DisplayName("getAllTags - Success: should return list of all tags")
    void getAllTags_list_success() {
        when(tagRepository.findAll()).thenReturn(List.of(testTag));

        List<Tag> result = tagService.getAllTags();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Food & Dining", result.get(0).getName());
    }

    @Test
    @DisplayName("getAllTags (Pageable) - Success: should return paged tags")
    void getAllTags_pageable_success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Tag> page = new PageImpl<>(List.of(testTag), pageable, 1);
        when(tagRepository.findAll(pageable)).thenReturn(page);

        Page<Tag> result = tagService.getAllTags(pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
    }
}