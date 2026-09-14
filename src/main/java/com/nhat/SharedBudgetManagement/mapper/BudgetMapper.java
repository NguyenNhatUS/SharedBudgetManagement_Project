package com.nhat.SharedBudgetManagement.mapper;

import com.nhat.SharedBudgetManagement.dto.response.BudgetResponse;
import com.nhat.SharedBudgetManagement.entity.Budget;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {UserMapper.class})
public interface BudgetMapper {

    @Mapping(target = "memberCount", expression = "java(budget.getMembers() != null ? budget.getMembers().size() : 0)")
    BudgetResponse toResponse(Budget budget);
}
