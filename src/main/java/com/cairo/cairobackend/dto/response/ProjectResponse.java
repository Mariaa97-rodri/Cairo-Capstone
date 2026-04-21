package com.cairo.cairobackend.dto.response;

import com.cairo.cairobackend.entity.Project;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProjectResponse {

    private Long         id;
    private String       name;
    private String       projectKey;
    private String       description;
    private UserResponse owner;

    public static ProjectResponse from(Project project) {
        return ProjectResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .projectKey(project.getProjectKey())
                .description(project.getDescription())
                .owner(UserResponse.from(project.getOwner()))
                .build();
    }
}