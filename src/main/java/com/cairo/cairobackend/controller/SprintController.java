package com.cairo.cairobackend.controller;

import com.cairo.cairobackend.entity.Issue;
import com.cairo.cairobackend.entity.Sprint;
import com.cairo.cairobackend.service.SprintService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class SprintController {

    private final SprintService sprintService;

    // GET /api/v1/projects/{id}/sprints
    @GetMapping("/projects/{projectId}/sprints")
    public ResponseEntity<List<Sprint>> getSprints(
            @PathVariable Long projectId) {

        return ResponseEntity.ok(
                sprintService.getSprintsForProject(projectId));
    }

    // POST /api/v1/projects/{id}/sprints
    @PostMapping("/projects/{projectId}/sprints")
    public ResponseEntity<Sprint> createSprint(
            @PathVariable Long projectId,
            @Valid @RequestBody CreateSprintRequest request) {

        Sprint created = sprintService.createSprint(
                projectId,
                request.getName(),
                request.getStartDate(),
                request.getEndDate()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(created);
    }

    // PATCH /api/v1/sprints/{id}/start
    @PatchMapping("/sprints/{sprintId}/start")
    public ResponseEntity<Sprint> startSprint(
            @PathVariable Long sprintId) {

        return ResponseEntity.ok(
                sprintService.startSprint(sprintId));
    }

    // PATCH /api/v1/sprints/{id}/complete
    @PatchMapping("/sprints/{sprintId}/complete")
    public ResponseEntity<Sprint> completeSprint(
            @PathVariable Long sprintId) {

        return ResponseEntity.ok(
                sprintService.completeSprint(sprintId));
    }

    // POST /api/v1/sprints/{id}/issues
    @PostMapping("/sprints/{sprintId}/issues")
    public ResponseEntity<Map<String, String>> addIssueToSprint(
            @PathVariable Long sprintId,
            @RequestBody Map<String, Long> body) {

        sprintService.addIssueToSprint(
                sprintId, body.get("issueId"));
        return ResponseEntity.ok(
                Map.of("message", "Issue added to sprint"));
    }

    // GET /api/v1/projects/{id}/board
    @GetMapping("/projects/{projectId}/board")
    public ResponseEntity<Map<Issue.IssueStatus, List<Issue>>>
    getBoard(@PathVariable Long projectId) {

        return ResponseEntity.ok(
                sprintService.getBoardData(projectId));
    }

    // ── Request DTO ──────────────────────────────────────────

    @Getter @Setter
    public static class CreateSprintRequest {

        @NotBlank(message = "Sprint name is required")
        private String name;

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate startDate;

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate endDate;
    }
}