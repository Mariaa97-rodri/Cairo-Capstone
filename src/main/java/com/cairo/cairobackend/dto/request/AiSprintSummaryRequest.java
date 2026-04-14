package com.cairo.cairobackend.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class AiSprintSummaryRequest {

    @NotNull(message = "Sprint ID is required")
    private Long sprintId;
}