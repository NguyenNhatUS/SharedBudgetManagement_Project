package com.nhat.SharedBudgetManagement.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateTagRequest {

    @NotBlank(message = "Tag name is required")
    @Size(max = 50, message = "Tag name must not exceed 50 characters")
    private String name;

    @Size(max = 7, message = "Color must be a valid hex code (e.g. #FF6B6B)")
    private String color;

    @Size(max = 50, message = "Icon name must not exceed 50 characters")
    private String icon;
}