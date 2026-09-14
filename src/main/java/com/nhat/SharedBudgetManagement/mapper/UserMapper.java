package com.nhat.SharedBudgetManagement.mapper;

import com.nhat.SharedBudgetManagement.dto.response.UserResponse;
import com.nhat.SharedBudgetManagement.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserResponse toResponse(User user);
}
