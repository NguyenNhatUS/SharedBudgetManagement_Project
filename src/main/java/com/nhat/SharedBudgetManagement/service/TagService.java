package com.nhat.SharedBudgetManagement.service;

import com.nhat.SharedBudgetManagement.entity.Tag;

import java.util.List;

public interface TagService {

    Tag createTag(String name);

    Tag updateTag(Long tagId, String name);

    void deleteTag(Long tagId);

    Tag getTagById(Long tagId);

    List<Tag> getAllTags();
}
