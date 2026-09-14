package com.nhat.SharedBudgetManagement.mapper;

import com.nhat.SharedBudgetManagement.dto.response.TagResponse;
import com.nhat.SharedBudgetManagement.entity.Tag;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TagMapper {
    TagResponse toResponse(Tag tag);
}
