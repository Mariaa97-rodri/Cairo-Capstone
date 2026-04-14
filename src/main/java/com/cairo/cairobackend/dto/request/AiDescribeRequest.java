package com.cairo.cairobackend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class AiDescribeRequest {

    @NotBlank(message = "Prompt is required")
    private String prompt;

    @NotNull(message = "Project ID is required")
    private Long projectId;
}