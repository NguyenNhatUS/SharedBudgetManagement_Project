package com.nhat.SharedBudgetManagement.service;

import com.nhat.SharedBudgetManagement.entity.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface TagService {

    Tag createTag(String name);

    Tag updateTag(Long tagId, String name);

    void deleteTag(Long tagId);

    Tag getTagById(Long tagId);

    List<Tag> getAllTags();

    Page<Tag> getAllTags(Pageable pageable);
}
