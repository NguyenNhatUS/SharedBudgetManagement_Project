package com.nhat.SharedBudgetManagement.controller.v1;

import com.nhat.SharedBudgetManagement.common.ApiResponse;
import com.nhat.SharedBudgetManagement.common.PageResponse;
import com.nhat.SharedBudgetManagement.dto.request.CreateTagRequest;
import com.nhat.SharedBudgetManagement.dto.request.UpdateTagRequest;
import com.nhat.SharedBudgetManagement.dto.response.TagResponse;
import com.nhat.SharedBudgetManagement.entity.Tag;
import com.nhat.SharedBudgetManagement.mapper.TagMapper;
import com.nhat.SharedBudgetManagement.service.TagService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;
    private final TagMapper tagMapper;

    @PostMapping
    public ResponseEntity<ApiResponse<TagResponse>> createTag(
            @Valid @RequestBody CreateTagRequest request) {
        Tag tag = tagService.createTag(request.getName());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Tag created successfully", tagMapper.toResponse(tag)));
    }


    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<TagResponse>>> getAllTags(
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<Tag> tagPage = tagService.getAllTags(pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(tagPage, tagMapper::toResponse)));
    }

    @GetMapping("/{tagId}")
    public ResponseEntity<ApiResponse<TagResponse>> getTag(@PathVariable Long tagId) {
        Tag tag = tagService.getTagById(tagId);
        return ResponseEntity.ok(ApiResponse.success(tagMapper.toResponse(tag)));
    }

    @PutMapping("/{tagId}")
    public ResponseEntity<ApiResponse<TagResponse>> updateTag(
            @PathVariable Long tagId,
            @Valid @RequestBody UpdateTagRequest request) {
        Tag tag = tagService.updateTag(tagId, request.getName());
        return ResponseEntity.ok(ApiResponse.success("Tag updated successfully", tagMapper.toResponse(tag)));
    }

    @DeleteMapping("/{tagId}")
    public ResponseEntity<ApiResponse<Void>> deleteTag(@PathVariable Long tagId) {
        tagService.deleteTag(tagId);
        return ResponseEntity.ok(ApiResponse.noContent("Tag deleted successfully"));
    }
}