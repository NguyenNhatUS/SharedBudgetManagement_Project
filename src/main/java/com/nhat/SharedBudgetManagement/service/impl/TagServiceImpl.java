package com.nhat.SharedBudgetManagement.service.impl;

import com.nhat.SharedBudgetManagement.config.RedisConfig;
import com.nhat.SharedBudgetManagement.entity.Tag;
import com.nhat.SharedBudgetManagement.exception.ConflictException;
import com.nhat.SharedBudgetManagement.exception.ErrorCode;
import com.nhat.SharedBudgetManagement.exception.ResourceNotFoundException;
import com.nhat.SharedBudgetManagement.repository.TagRepository;
import com.nhat.SharedBudgetManagement.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {

    private final TagRepository tagRepository;

    @Override
    @Transactional
    @CacheEvict(value = {RedisConfig.CACHE_TAGS, RedisConfig.CACHE_TAG}, allEntries = true)
    public Tag createTag(String name) {
        if (tagRepository.existsByName(name)) {
            throw new ConflictException(ErrorCode.TAG_ALREADY_EXISTS, "Tag with name '" + name + "' already exists");
        }

        Tag tag = Tag.builder()
                .name(name)
                .build();
        return tagRepository.save(tag);
    }

    @Override
    @Transactional
    @CacheEvict(value = {RedisConfig.CACHE_TAGS, RedisConfig.CACHE_TAG}, allEntries = true)
    public Tag updateTag(Long tagId, String name) {
        Tag tag = getTagById(tagId);
        tag.setName(name);

        return tagRepository.save(tag);
    }

    @Override
    @Transactional
    @CacheEvict(value = {RedisConfig.CACHE_TAGS, RedisConfig.CACHE_TAG}, allEntries = true)
    public void deleteTag(Long tagId) {
        Tag tag = getTagById(tagId);
        tagRepository.delete(tag);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = RedisConfig.CACHE_TAG, key = "#tagId")
    public Tag getTagById(Long tagId) {
        return tagRepository.findById(tagId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.TAG_NOT_FOUND, "Tag not found with id: " + tagId));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = RedisConfig.CACHE_TAGS, key = "'all'")
    public List<Tag> getAllTags() {
        return tagRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Tag> getAllTags(Pageable pageable) {
        return tagRepository.findAll(pageable);
    }
}
