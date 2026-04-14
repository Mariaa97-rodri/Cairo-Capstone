package com.cairo.cairobackend.controller;

import com.cairo.cairobackend.dto.request.AiDescribeRequest;
import com.cairo.cairobackend.dto.request.AiSuggestRequest;
import com.cairo.cairobackend.dto.request.AiAssigneeRequest;
import com.cairo.cairobackend.dto.request.AiSprintSummaryRequest;
import com.cairo.cairobackend.entity.User;
import com.cairo.cairobackend.service.AiAgentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiAgentService aiAgentService;

    @PostMapping("/describe-issue")
    public ResponseEntity<Map<String, Object>> describeIssue(
            @Valid @RequestBody AiDescribeRequest request,
            @AuthenticationPrincipal User currentUser) {

        return ResponseEntity.ok(
                aiAgentService.describeIssue(
                        request.getPrompt(),
                        request.getProjectId(),
                        currentUser
                ));
    }

    @PostMapping("/suggest-fields")
    public ResponseEntity<Map<String, Object>> suggestFields(
            @Valid @RequestBody AiSuggestRequest request,
            @AuthenticationPrincipal User currentUser) {

        return ResponseEntity.ok(
                aiAgentService.suggestFields(
                        request.getTitle(),
                        request.getDescription(),
                        currentUser
                ));
    }

    @PostMapping("/recommend-assignee")
    public ResponseEntity<Map<String, Object>> recommendAssignee(
            @Valid @RequestBody AiAssigneeRequest request,
            @AuthenticationPrincipal User currentUser) {

        return ResponseEntity.ok(
                aiAgentService.recommendAssignee(
                        request.getProjectId(),
                        request.getIssueTitle(),
                        currentUser
                ));
    }

    @PostMapping("/sprint-summary")
    public ResponseEntity<Map<String, Object>> sprintSummary(
            @Valid @RequestBody AiSprintSummaryRequest request,
            @AuthenticationPrincipal User currentUser) {

        return ResponseEntity.ok(
                aiAgentService.sprintSummary(
                        request.getSprintId(),
                        currentUser
                ));
    }

    @PatchMapping("/interactions/{id}/accept")
    public ResponseEntity<Map<String, String>> markAccepted(
            @PathVariable Long id) {

        aiAgentService.markAccepted(id);
        return ResponseEntity.ok(
                Map.of("message",
                        "Interaction marked as accepted"));
    }
}