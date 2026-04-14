package com.cairo.cairobackend.controller;

import com.cairo.cairobackend.entity.Issue;
import com.cairo.cairobackend.entity.User;
import com.cairo.cairobackend.service.IssueService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class IssueController {

    private final IssueService issueService;

    // GET /api/v1/projects/{id}/issues
    @GetMapping("/projects/{projectId}/issues")
    public ResponseEntity<Page<Issue>> getIssues(
            @PathVariable Long projectId,
            @RequestParam(required = false) Issue.IssueType type,
            @RequestParam(required = false) Issue.Priority priority,
            @RequestParam(required = false) Issue.IssueStatus status,
            @RequestParam(required = false) Long assigneeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<Issue> issues = issueService.getProjectIssues(
                projectId,
                type,
                priority,
                status,
                assigneeId,
                PageRequest.of(page, size,
                        Sort.by("createdAt").descending())
        );
        return ResponseEntity.ok(issues);
    }

    // GET /api/v1/issues/{id}
    @GetMapping("/issues/{issueId}")
    public ResponseEntity<Issue> getIssue(
            @PathVariable Long issueId) {

        return ResponseEntity.ok(
                issueService.getIssueById(issueId));
    }

    // POST /api/v1/projects/{id}/issues
    @PostMapping("/projects/{projectId}/issues")
    public ResponseEntity<Issue> createIssue(
            @PathVariable Long projectId,
            @Valid @RequestBody CreateIssueRequest request,
            @AuthenticationPrincipal User currentUser) {

        Issue created = issueService.createIssue(
                projectId,
                currentUser.getId(),
                request.getTitle(),
                request.getDescription(),
                request.getType(),
                request.getPriority(),
                request.getAssigneeId(),
                request.getSprintId(),
                request.getStoryPoints()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(created);
    }

    // PUT /api/v1/issues/{id}
    @PutMapping("/issues/{issueId}")
    public ResponseEntity<Issue> updateIssue(
            @PathVariable Long issueId,
            @Valid @RequestBody UpdateIssueRequest request,
            @AuthenticationPrincipal User currentUser) {

        Issue updated = issueService.updateIssue(
                issueId,
                request.getTitle(),
                request.getDescription(),
                request.getType(),
                request.getPriority(),
                request.getAssigneeId(),
                request.getSprintId(),
                request.getStoryPoints(),
                currentUser
        );
        return ResponseEntity.ok(updated);
    }

    // PATCH /api/v1/issues/{id}/status
    @PatchMapping("/issues/{issueId}/status")
    public ResponseEntity<Issue> updateStatus(
            @PathVariable Long issueId,
            @Valid @RequestBody UpdateStatusRequest request,
            @AuthenticationPrincipal User currentUser) {

        Issue updated = issueService.updateStatus(
                issueId,
                request.getStatus(),
                currentUser
        );
        return ResponseEntity.ok(updated);
    }

    // DELETE /api/v1/issues/{id}
    @DeleteMapping("/issues/{issueId}")
    public ResponseEntity<Void> deleteIssue(
            @PathVariable Long issueId) {

        issueService.deleteIssue(issueId);
        return ResponseEntity.noContent().build();
    }

    // ── Request DTOs ─────────────────────────────────────────

    @Getter @Setter
    public static class CreateIssueRequest {

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

    @Getter @Setter
    public static class UpdateIssueRequest {

        private String title;
        private String description;
        private Issue.IssueType type;
        private Issue.Priority priority;
        private Long assigneeId;
        private Long sprintId;
        private Integer storyPoints;
    }

    @Getter @Setter
    public static class UpdateStatusRequest {

        @NotNull(message = "Status is required")
        private Issue.IssueStatus status;
    }
}