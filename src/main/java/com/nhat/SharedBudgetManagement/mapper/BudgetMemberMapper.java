package com.nhat.SharedBudgetManagement.mapper;

import com.nhat.SharedBudgetManagement.dto.response.BudgetMemberResponse;
import com.nhat.SharedBudgetManagement.entity.BudgetMember;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {UserMapper.class})
public interface BudgetMemberMapper {

    BudgetMemberResponse toResponse(BudgetMember budgetMember);
}
