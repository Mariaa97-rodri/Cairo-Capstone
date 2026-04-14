package com.cairo.cairobackend.service;

import com.cairo.cairobackend.entity.AiInteraction;
import com.cairo.cairobackend.entity.Issue;
import com.cairo.cairobackend.entity.Sprint;
import com.cairo.cairobackend.entity.User;
import com.cairo.cairobackend.exception.BusinessException;
import com.cairo.cairobackend.exception.ResourceNotFoundException;
import com.cairo.cairobackend.repository.AiInteractionRepository;
import com.cairo.cairobackend.repository.IssueRepository;
import com.cairo.cairobackend.repository.SprintRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class AiAgentService {

    private final WebClient.Builder webClientBuilder;
    private final AiInteractionRepository aiInteractionRepository;
    private final IssueRepository issueRepository;
    private final SprintRepository sprintRepository;

    @Value("${app.claude.api-key}")
    private String claudeApiKey;

    @Value("${app.claude.model}")
    private String claudeModel;

    @Value("${app.claude.base-url}")
    private String claudeBaseUrl;

    // Sends a prompt to Claude and gets back a full issue description
    @Transactional
    public Map<String, Object> describeIssue(String prompt,
                                             Long projectId,
                                             User user) {

        String systemPrompt = """
            You are a helpful project management assistant.
            When given a short description of a problem or task,
            generate a structured issue description with:
            - Summary (one sentence)
            - Steps to reproduce (if a bug)
            - Expected behavior
            - Actual behavior
            - Also suggest the issue type (BUG, STORY, TASK, or EPIC)
              and priority (LOW, MEDIUM, HIGH, or CRITICAL).
            Keep responses concise and professional.
            """;

        String fullPrompt = "Write an issue description for: " + prompt;

        String response = callClaude(systemPrompt, fullPrompt);

        // Log the interaction for audit trail
        AiInteraction interaction = AiInteraction.builder()
                .user(user)
                .feature(AiInteraction.AiFeature.DESCRIBE_ISSUE)
                .promptText(fullPrompt)
                .responseText(response)
                .tokensUsed(estimateTokens(fullPrompt + response))
                .wasAccepted(false)
                .build();

        AiInteraction saved = aiInteractionRepository.save(interaction);

        Map<String, Object> result = new HashMap<>();
        result.put("description", response);
        result.put("interactionId", saved.getId());
        return result;
    }

    // Suggests type and priority based on title and description
    @Transactional
    public Map<String, Object> suggestFields(String title,
                                             String description,
                                             User user) {

        String systemPrompt = """
            You are a project management assistant.
            Given an issue title and description, respond with
            ONLY a JSON object in this exact format:
            {
              "type": "BUG|STORY|TASK|EPIC",
              "priority": "LOW|MEDIUM|HIGH|CRITICAL",
              "reasoning": "brief explanation"
            }
            No other text, only the JSON.
            """;

        String fullPrompt = "Title: " + title + "\nDescription: "
                + description;
        String response = callClaude(systemPrompt, fullPrompt);

        AiInteraction interaction = AiInteraction.builder()
                .user(user)
                .feature(AiInteraction.AiFeature.SUGGEST_FIELDS)
                .promptText(fullPrompt)
                .responseText(response)
                .tokensUsed(estimateTokens(fullPrompt + response))
                .wasAccepted(false)
                .build();

        AiInteraction saved = aiInteractionRepository.save(interaction);

        Map<String, Object> result = new HashMap<>();
        result.put("suggestion", response);
        result.put("interactionId", saved.getId());
        return result;
    }

    // Recommends an assignee based on workload
    @Transactional
    public Map<String, Object> recommendAssignee(Long projectId,
                                                 String issueTitle,
                                                 User user) {

        // Get open issue counts per assignee from the database
        List<Object[]> workloadData = issueRepository
                .countOpenIssuesByAssigneeInProject(projectId);

        StringBuilder workloadSummary = new StringBuilder(
                "Current team workload (open issues per person):\n");
        for (Object[] row : workloadData) {
            workloadSummary.append("User ID ")
                    .append(row[0]).append(": ")
                    .append(row[1]).append(" open issues\n");
        }

        String systemPrompt = """
            You are a project management assistant.
            Based on team workload data, recommend who should
            be assigned a new issue. Recommend the person with
            the lowest current workload. Be brief and direct.
            """;

        String fullPrompt = workloadSummary
                + "\nNew issue: " + issueTitle
                + "\nWho should be assigned?";

        String response = callClaude(systemPrompt, fullPrompt);

        AiInteraction interaction = AiInteraction.builder()
                .user(user)
                .feature(AiInteraction.AiFeature.RECOMMEND_ASSIGNEE)
                .promptText(fullPrompt)
                .responseText(response)
                .tokensUsed(estimateTokens(fullPrompt + response))
                .wasAccepted(false)
                .build();

        AiInteraction saved = aiInteractionRepository.save(interaction);

        Map<String, Object> result = new HashMap<>();
        result.put("recommendation", response);
        result.put("interactionId", saved.getId());
        return result;
    }

    // Generates a sprint progress summary
    @Transactional
    public Map<String, Object> sprintSummary(Long sprintId, User user) {

        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Sprint", sprintId));

        List<Issue> issues = issueRepository
                .findBySprintIdOrderByCreatedAtAsc(sprintId);

        long done       = issues.stream().filter(i ->
                i.getStatus() == Issue.IssueStatus.DONE).count();
        long inProgress = issues.stream().filter(i ->
                i.getStatus() == Issue.IssueStatus.IN_PROGRESS).count();
        long inReview   = issues.stream().filter(i ->
                i.getStatus() == Issue.IssueStatus.IN_REVIEW).count();
        long todo       = issues.stream().filter(i ->
                i.getStatus() == Issue.IssueStatus.TODO).count();

        String sprintData = String.format("""
            Sprint: %s
            Status: %s
            Total issues: %d
            Done: %d | In Review: %d | In Progress: %d | To Do: %d
            """,
                sprint.getName(), sprint.getStatus(),
                issues.size(), done, inReview, inProgress, todo);

        String systemPrompt = """
            You are a project management assistant.
            Generate a concise sprint progress summary suitable
            for a standup meeting. Highlight completion percentage,
            any concerns, and a recommendation for the team.
            Keep it under 100 words.
            """;

        String response = callClaude(systemPrompt, sprintData);

        AiInteraction interaction = AiInteraction.builder()
                .user(user)
                .sprint(sprint)
                .feature(AiInteraction.AiFeature.SPRINT_SUMMARY)
                .promptText(sprintData)
                .responseText(response)
                .tokensUsed(estimateTokens(sprintData + response))
                .wasAccepted(false)
                .build();

        AiInteraction saved = aiInteractionRepository.save(interaction);

        Map<String, Object> result = new HashMap<>();
        result.put("summary", response);
        result.put("stats", Map.of(
                "total", issues.size(),
                "done", done,
                "inReview", inReview,
                "inProgress", inProgress,
                "todo", todo
        ));
        result.put("interactionId", saved.getId());
        return result;
    }

    // Mark an AI suggestion as accepted by the user
    @Transactional
    public void markAccepted(Long interactionId) {
        AiInteraction interaction = aiInteractionRepository
                .findById(interactionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "AiInteraction", interactionId));
        interaction.setWasAccepted(true);
        aiInteractionRepository.save(interaction);
    }

    // Makes the actual HTTP call to the Claude API
    private String callClaude(String systemPrompt, String userMessage) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", claudeModel);
            requestBody.put("max_tokens", 1024);
            requestBody.put("system", systemPrompt);
            requestBody.put("messages", List.of(
                    Map.of("role", "user", "content", userMessage)
            ));

            Map response = webClientBuilder.build()
                    .post()
                    .uri(claudeBaseUrl + "/v1/messages")
                    .header("x-api-key", claudeApiKey)
                    .header("anthropic-version", "2023-06-01")
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && response.containsKey("content")) {
                List<Map<String, Object>> content =
                        (List<Map<String, Object>>) response.get("content");
                if (!content.isEmpty()) {
                    return (String) content.get(0).get("text");
                }
            }

            throw new BusinessException("Empty response from Claude API");

        } catch (Exception e) {
            log.error("Claude API call failed: {}", e.getMessage());
            throw new BusinessException(
                    "AI service unavailable. Please try again.");
        }
    }

    // Rough token estimate — actual count comes from the API response
    private int estimateTokens(String text) {
        return text.length() / 4;
    }
}