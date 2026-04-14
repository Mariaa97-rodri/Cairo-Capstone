package com.cairo.cairobackend.controller;

import com.cairo.cairobackend.entity.Project;
import com.cairo.cairobackend.entity.User;
import com.cairo.cairobackend.service.ProjectService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    // GET /api/v1/projects
    @GetMapping
    public ResponseEntity<Page<Project>> getProjects(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal User currentUser) {

        Page<Project> projects = projectService.getProjectsForUser(
                currentUser.getId(),
                PageRequest.of(page, size,
                        Sort.by("createdAt").descending())
        );
        return ResponseEntity.ok(projects);
    }

    // GET /api/v1/projects/{id}
    @GetMapping("/{id}")
    public ResponseEntity<Project> getProject(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                projectService.getProjectById(id));
    }

    // POST /api/v1/projects
    @PostMapping
    public ResponseEntity<Project> createProject(
            @Valid @RequestBody CreateProjectRequest request,
            @AuthenticationPrincipal User currentUser) {

        Project created = projectService.createProject(
                request.getName(),
                request.getProjectKey(),
                request.getDescription(),
                currentUser
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(created);
    }

    // DELETE /api/v1/projects/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {

        projectService.deleteProject(id, currentUser);
        return ResponseEntity.noContent().build();
    }

    // POST /api/v1/projects/{id}/members
    @PostMapping("/{id}/members")
    public ResponseEntity<Map<String, String>> addMember(
            @PathVariable Long id,
            @RequestBody Map<String, Long> body,
            @AuthenticationPrincipal User currentUser) {

        projectService.addMember(id, body.get("userId"), currentUser);
        return ResponseEntity.ok(
                Map.of("message", "Member added successfully"));
    }

    // DELETE /api/v1/projects/{id}/members/{userId}
    @DeleteMapping("/{id}/members/{userId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable Long id,
            @PathVariable Long userId,
            @AuthenticationPrincipal User currentUser) {

        projectService.removeMember(id, userId, currentUser);
        return ResponseEntity.noContent().build();
    }

    // ── Request DTOs ─────────────────────────────────────────

    @Getter @Setter
    public static class CreateProjectRequest {

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
}