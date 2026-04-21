package com.cairo.cairobackend.dto.response;

import com.cairo.cairobackend.entity.Issue;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class IssueResponse {

    private Long         id;
    private String       title;
    private String       description;
    private String       type;
    private String       priority;
    private String       status;
    private Integer      storyPoints;
    private Long         projectId;
    private Long         sprintId;
    private UserResponse reporter;
    private UserResponse assignee;   // null if unassigned

    public static IssueResponse from(Issue issue) {
        return IssueResponse.builder()
                .id(issue.getId())
                .title(issue.getTitle())
                .description(issue.getDescription())
                .type(issue.getType().name())
                .priority(issue.getPriority().name())
                .status(issue.getStatus().name())
                .storyPoints(issue.getStoryPoints())
                .projectId(issue.getProject().getId())
                .sprintId(issue.getSprint() != null
                        ? issue.getSprint().getId() : null)
                .reporter(UserResponse.from(issue.getReporter()))
                .assignee(UserResponse.from(issue.getAssignee()))
                .build();
    }
}