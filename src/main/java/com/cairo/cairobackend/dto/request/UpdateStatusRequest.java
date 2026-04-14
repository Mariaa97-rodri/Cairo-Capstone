package com.cairo.cairobackend.dto.request;

import com.cairo.cairobackend.entity.Issue;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateStatusRequest {

    @NotNull(message = "Status is required")
    private Issue.IssueStatus status;
}