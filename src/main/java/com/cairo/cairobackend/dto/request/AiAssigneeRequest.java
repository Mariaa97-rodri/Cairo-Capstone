package com.cairo.cairobackend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class AiAssigneeRequest {

    @NotNull(message = "Project ID is required")
    private Long projectId;

    @NotBlank(message = "Issue title is required")
    private String issueTitle;
}