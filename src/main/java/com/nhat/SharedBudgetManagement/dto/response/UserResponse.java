package com.nhat.SharedBudgetManagement.dto.response;

import com.nhat.SharedBudgetManagement.entity.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {

    private Long id;
    private String email;
    private String fullName;
    private String avatarUrl;
    private UserRole role;
    private LocalDateTime createdAt;
}
