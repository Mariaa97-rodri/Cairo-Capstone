package com.cairo.cairobackend.dto.request;

import com.cairo.cairobackend.entity.Issue;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateIssueRequest {

    private String title;
    private String description;
    private Issue.IssueType type;
    private Issue.Priority priority;
    private Long assigneeId;
    private Long sprintId;
    private Integer storyPoints;
}