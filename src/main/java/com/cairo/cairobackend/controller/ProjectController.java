package com.cairo.cairobackend.controller;

import com.cairo.cairobackend.dto.request.CreateProjectRequest;
import com.cairo.cairobackend.dto.response.ProjectResponse;
import com.cairo.cairobackend.dto.response.UserResponse;
import com.cairo.cairobackend.entity.Project;
import com.cairo.cairobackend.entity.User;
import com.cairo.cairobackend.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping
    public ResponseEntity<Page<ProjectResponse>> getProjects(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal User currentUser) {

        Page<Project> projects = projectService.getProjectsForUser(
                currentUser.getId(),
                PageRequest.of(page, size, Sort.by("createdAt").descending())
        );
        Page<ProjectResponse> response = projects.map(ProjectResponse::from);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponse> getProject(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(
                ProjectResponse.from(projectService.getProjectById(id, currentUser)));
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<List<UserResponse>> getMembers(@PathVariable Long id) {
        return ResponseEntity.ok(projectService.getMembers(id));
    }

    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(
            @Valid @RequestBody CreateProjectRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ProjectResponse.from(
                        projectService.createProject(
                                request.getName(),
                                request.getProjectKey(),
                                request.getDescription(),
                                currentUser)));
    }

    // Edit project name and description — owner or ADMIN only
    @PutMapping("/{id}")
    public ResponseEntity<ProjectResponse> updateProject(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(ProjectResponse.from(
                projectService.updateProject(id,
                        body.get("name"),
                        body.get("description"),
                        currentUser)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        projectService.deleteProject(id, currentUser);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<Map<String, String>> addMember(
            @PathVariable Long id,
            @RequestBody Map<String, Long> body,
            @AuthenticationPrincipal User currentUser) {
        projectService.addMember(id, body.get("userId"), currentUser);
        return ResponseEntity.ok(Map.of("message", "Member added successfully"));
    }

    @DeleteMapping("/{id}/members/{userId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable Long id,
            @PathVariable Long userId,
            @AuthenticationPrincipal User currentUser) {
        projectService.removeMember(id, userId, currentUser);
        return ResponseEntity.noContent().build();
    }
}