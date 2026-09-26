package com.nhat.SharedBudgetManagement.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhat.SharedBudgetManagement.controller.v1.TagController;
import com.nhat.SharedBudgetManagement.dto.request.CreateTagRequest;
import com.nhat.SharedBudgetManagement.dto.request.UpdateTagRequest;
import com.nhat.SharedBudgetManagement.dto.response.TagResponse;
import com.nhat.SharedBudgetManagement.entity.Tag;
import com.nhat.SharedBudgetManagement.exception.GlobalExceptionHandler;
import com.nhat.SharedBudgetManagement.mapper.TagMapper;
import com.nhat.SharedBudgetManagement.service.TagService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests for TagController")
class TagControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private TagService tagService;

    @Mock
    private TagMapper tagMapper;

    @InjectMocks
    private TagController tagController;

    private Tag testTag;
    private TagResponse testTagResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(tagController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();

        objectMapper = new ObjectMapper();

        testTag = Tag.builder().id(1L).name("Food").build();
        testTagResponse = TagResponse.builder().id(1L).name("Food").build();
    }

    @Test
    @DisplayName("POST /api/v1/tags - Success (201 Created)")
    void createTag_success() throws Exception {
        CreateTagRequest request = new CreateTagRequest();
        request.setName("Food");

        when(tagService.createTag("Food")).thenReturn(testTag);
        when(tagMapper.toResponse(testTag)).thenReturn(testTagResponse);

        mockMvc.perform(post("/api/v1/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.name").value("Food"));
    }

    @Test
    @DisplayName("GET /api/v1/tags/{id} - Success (200 OK)")
    void getTag_success() throws Exception {
        when(tagService.getTagById(1L)).thenReturn(testTag);
        when(tagMapper.toResponse(testTag)).thenReturn(testTagResponse);

        mockMvc.perform(get("/api/v1/tags/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.id").value(1L));
    }

    @Test
    @DisplayName("GET /api/v1/tags - Success (200 OK)")
    void getAllTags_success() throws Exception {
        Page<Tag> tagPage = new PageImpl<>(List.of(testTag));
        when(tagService.getAllTags(any(Pageable.class))).thenReturn(tagPage);
        when(tagMapper.toResponse(testTag)).thenReturn(testTagResponse);

        mockMvc.perform(get("/api/v1/tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.content[0].name").value("Food"));
    }

    @Test
    @DisplayName("PUT /api/v1/tags/{id} - Success (200 OK)")
    void updateTag_success() throws Exception {
        UpdateTagRequest request = new UpdateTagRequest();
        request.setName("Groceries");

        Tag updatedTag = Tag.builder().id(1L).name("Groceries").build();
        TagResponse updatedResponse = TagResponse.builder().id(1L).name("Groceries").build();

        when(tagService.updateTag(1L, "Groceries")).thenReturn(updatedTag);
        when(tagMapper.toResponse(updatedTag)).thenReturn(updatedResponse);

        mockMvc.perform(put("/api/v1/tags/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.name").value("Groceries"));
    }

    @Test
    @DisplayName("DELETE /api/v1/tags/{id} - Success (200 OK)")
    void deleteTag_success() throws Exception {
        mockMvc.perform(delete("/api/v1/tags/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }
}
