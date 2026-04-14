package com.cairo.cairobackend.dto.request;

import com.cairo.cairobackend.entity.Issue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateIssueRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotNull(message = "Type is required")
    private Issue.IssueType type;

    @NotNull(message = "Priority is required")
    private Issue.Priority priority;

    private Long assigneeId;
    private Long sprintId;
    private Integer storyPoints;
}