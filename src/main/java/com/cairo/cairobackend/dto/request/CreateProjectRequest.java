package com.cairo.cairobackend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateProjectRequest {

    @NotBlank(message = "Project name is required")
    @Size(max = 150)
    private String name;

    @NotBlank(message = "Project key is required")
    @Size(max = 10, message = "Key cannot exceed 10 characters")
    @Pattern(regexp = "^[A-Za-z0-9]+$",
            message = "Key must be letters and numbers only")
    private String projectKey;

    private String description;
}