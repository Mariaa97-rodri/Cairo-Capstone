package com.cairo.cairobackend.dto.response;

import com.cairo.cairobackend.entity.Sprint;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class SprintResponse {

    private Long       id;
    private String     name;
    private String     status;
    private LocalDate  startDate;
    private LocalDate  endDate;
    private Long       projectId;

    public static SprintResponse from(Sprint sprint) {
        return SprintResponse.builder()
                .id(sprint.getId())
                .name(sprint.getName())
                .status(sprint.getStatus().name())
                .startDate(sprint.getStartDate())
                .endDate(sprint.getEndDate())
                .projectId(sprint.getProject().getId())
                .build();
    }
}