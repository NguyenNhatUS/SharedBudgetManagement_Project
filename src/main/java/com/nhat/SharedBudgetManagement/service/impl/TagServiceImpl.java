package com.nhat.SharedBudgetManagement.service.impl;

import com.nhat.SharedBudgetManagement.entity.Tag;
import com.nhat.SharedBudgetManagement.repository.TagRepository;
import com.nhat.SharedBudgetManagement.service.TagService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {

    private final TagRepository tagRepository;

    @Override
    @Transactional
    public Tag createTag(String name, String color, String icon) {
        if (tagRepository.existsByName(name)) {
            throw new IllegalArgumentException("Tag with name '" + name + "' already exists");
        }

        Tag tag = Tag.builder()
                .name(name)
                .color(color)
                .icon(icon)
                .build();
        return tagRepository.save(tag);
    }

    @Override
    @Transactional
    public Tag updateTag(Long tagId, String name, String color, String icon) {
        Tag tag = getTagById(tagId);
        tag.setName(name);
        tag.setColor(color);
        tag.setIcon(icon);
        return tagRepository.save(tag);
    }

    @Override
    @Transactional
    public void deleteTag(Long tagId) {
        Tag tag = getTagById(tagId);
        tagRepository.delete(tag);
    }

    @Override
    @Transactional(readOnly = true)
    public Tag getTagById(Long tagId) {
        return tagRepository.findById(tagId)
                .orElseThrow(() -> new EntityNotFoundException("Tag not found with id: " + tagId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Tag> getAllTags() {
        return tagRepository.findAll();
    }
}
