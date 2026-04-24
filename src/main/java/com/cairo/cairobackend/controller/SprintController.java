package com.cairo.cairobackend.controller;

import com.cairo.cairobackend.dto.request.CreateSprintRequest;
import com.cairo.cairobackend.dto.response.IssueResponse;
import com.cairo.cairobackend.dto.response.SprintResponse;
import com.cairo.cairobackend.entity.Issue;
import com.cairo.cairobackend.service.SprintService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class SprintController {

    private final SprintService sprintService;

    @GetMapping("/projects/{projectId}/sprints")
    public ResponseEntity<List<SprintResponse>> getSprints(
            @PathVariable Long projectId) {
        return ResponseEntity.ok(
                sprintService.getSprintsForProject(projectId)
                        .stream().map(SprintResponse::from)
                        .collect(Collectors.toList()));
    }

    @PostMapping("/projects/{projectId}/sprints")
    public ResponseEntity<SprintResponse> createSprint(
            @PathVariable Long projectId,
            @Valid @RequestBody CreateSprintRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SprintResponse.from(
                        sprintService.createSprint(projectId, request.getName(),
                                request.getStartDate(), request.getEndDate())));
    }

    // Edit a PENDING sprint — name and dates only
    @PutMapping("/sprints/{sprintId}")
    public ResponseEntity<SprintResponse> updateSprint(
            @PathVariable Long sprintId,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(SprintResponse.from(
                sprintService.updateSprint(sprintId,
                        body.get("name"),
                        body.get("startDate"),
                        body.get("endDate"))));
    }

    @PatchMapping("/sprints/{sprintId}/start")
    public ResponseEntity<SprintResponse> startSprint(@PathVariable Long sprintId) {
        return ResponseEntity.ok(SprintResponse.from(sprintService.startSprint(sprintId)));
    }

    @PatchMapping("/sprints/{sprintId}/complete")
    public ResponseEntity<SprintResponse> completeSprint(@PathVariable Long sprintId) {
        return ResponseEntity.ok(SprintResponse.from(sprintService.completeSprint(sprintId)));
    }

    @PostMapping("/sprints/{sprintId}/issues")
    public ResponseEntity<Map<String, String>> addIssueToSprint(
            @PathVariable Long sprintId,
            @RequestBody Map<String, Long> body) {
        sprintService.addIssueToSprint(sprintId, body.get("issueId"));
        return ResponseEntity.ok(Map.of("message", "Issue added to sprint"));
    }

    @GetMapping("/projects/{projectId}/board")
    public ResponseEntity<Map<Issue.IssueStatus, List<IssueResponse>>> getBoard(
            @PathVariable Long projectId) {
        Map<Issue.IssueStatus, List<IssueResponse>> board =
                sprintService.getBoardData(projectId)
                        .entrySet().stream()
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                e -> e.getValue().stream()
                                        .map(IssueResponse::from)
                                        .collect(Collectors.toList())
                        ));
        return ResponseEntity.ok(board);
    }
}