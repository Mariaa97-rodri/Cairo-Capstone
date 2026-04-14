package com.cairo.cairobackend.controller;

import com.cairo.cairobackend.entity.User;
import com.cairo.cairobackend.service.AiAgentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiAgentService aiAgentService;

    // POST /api/v1/ai/describe-issue
    @PostMapping("/describe-issue")
    public ResponseEntity<Map<String, Object>> describeIssue(
            @Valid @RequestBody DescribeRequest request,
            @AuthenticationPrincipal User currentUser) {

        return ResponseEntity.ok(
                aiAgentService.describeIssue(
                        request.getPrompt(),
                        request.getProjectId(),
                        currentUser
                ));
    }

    // POST /api/v1/ai/suggest-fields
    @PostMapping("/suggest-fields")
    public ResponseEntity<Map<String, Object>> suggestFields(
            @Valid @RequestBody SuggestRequest request,
            @AuthenticationPrincipal User currentUser) {

        return ResponseEntity.ok(
                aiAgentService.suggestFields(
                        request.getTitle(),
                        request.getDescription(),
                        currentUser
                ));
    }

    // POST /api/v1/ai/recommend-assignee
    @PostMapping("/recommend-assignee")
    public ResponseEntity<Map<String, Object>> recommendAssignee(
            @Valid @RequestBody AssigneeRequest request,
            @AuthenticationPrincipal User currentUser) {

        return ResponseEntity.ok(
                aiAgentService.recommendAssignee(
                        request.getProjectId(),
                        request.getIssueTitle(),
                        currentUser
                ));
    }

    // POST /api/v1/ai/sprint-summary
    @PostMapping("/sprint-summary")
    public ResponseEntity<Map<String, Object>> sprintSummary(
            @Valid @RequestBody SprintSummaryRequest request,
            @AuthenticationPrincipal User currentUser) {

        return ResponseEntity.ok(
                aiAgentService.sprintSummary(
                        request.getSprintId(),
                        currentUser
                ));
    }

    // PATCH /api/v1/ai/interactions/{id}/accept
    @PatchMapping("/interactions/{id}/accept")
    public ResponseEntity<Map<String, String>> markAccepted(
            @PathVariable Long id) {

        aiAgentService.markAccepted(id);
        return ResponseEntity.ok(
                Map.of("message", "Interaction marked as accepted"));
    }

    // ── Request DTOs ─────────────────────────────────────────

    @Getter @Setter
    public static class DescribeRequest {
        @NotBlank(message = "Prompt is required")
        private String prompt;
        @NotNull(message = "Project ID is required")
        private Long projectId;
    }

    @Getter @Setter
    public static class SuggestRequest {
        @NotBlank(message = "Title is required")
        private String title;
        private String description;
    }

    @Getter @Setter
    public static class AssigneeRequest {
        @NotNull(message = "Project ID is required")
        private Long projectId;
        @NotBlank(message = "Issue title is required")
        private String issueTitle;
    }

    @Getter @Setter
    public static class SprintSummaryRequest {
        @NotNull(message = "Sprint ID is required")
        private Long sprintId;
    }
}