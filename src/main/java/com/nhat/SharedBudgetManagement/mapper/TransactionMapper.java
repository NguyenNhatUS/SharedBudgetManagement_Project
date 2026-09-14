package com.nhat.SharedBudgetManagement.mapper;

import com.nhat.SharedBudgetManagement.dto.response.TagResponse;
import com.nhat.SharedBudgetManagement.dto.response.TransactionResponse;
import com.nhat.SharedBudgetManagement.entity.Transaction;
import com.nhat.SharedBudgetManagement.entity.TransactionTag;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Collections;
import java.util.List;

@Mapper(componentModel = "spring", uses = {TagMapper.class})
public interface TransactionMapper {

    @Mapping(target = "createdByUserName", source = "createdBy.user.fullName")
    @Mapping(target = "createdByUserId", source = "createdBy.user.id")
    @Mapping(target = "tags", source = "transactionTags", qualifiedByName = "transactionTagsToTagResponses")
    TransactionResponse toResponse(Transaction transaction);

    @Named("transactionTagsToTagResponses")
    default List<TagResponse> transactionTagsToTagResponses(List<TransactionTag> transactionTags) {
        if (transactionTags == null) {
            return Collections.emptyList();
        }
        return transactionTags.stream()
                .map(tt -> TagResponse.builder()
                        .id(tt.getTag().getId())
                        .name(tt.getTag().getName())
                        .build())
                .toList();
    }
}
