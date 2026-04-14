package com.cairo.cairobackend.service;

import com.cairo.cairobackend.entity.Issue;
import com.cairo.cairobackend.entity.Sprint;
import com.cairo.cairobackend.exception.BusinessException;
import com.cairo.cairobackend.exception.ResourceNotFoundException;
import com.cairo.cairobackend.repository.IssueRepository;
import com.cairo.cairobackend.repository.ProjectRepository;
import com.cairo.cairobackend.repository.SprintRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class SprintService {

    private final SprintRepository sprintRepository;
    private final IssueRepository issueRepository;
    private final ProjectRepository projectRepository;

    @Transactional
    public Sprint createSprint(Long projectId,
                               String name,
                               LocalDate startDate,
                               LocalDate endDate) {

        projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project", projectId));

        Sprint sprint = Sprint.builder()
                .project(projectRepository.getReferenceById(projectId))
                .name(name)
                .startDate(startDate)
                .endDate(endDate)
                .status(Sprint.SprintStatus.PENDING)
                .build();

        Sprint saved = sprintRepository.save(sprint);
        log.info("Sprint '{}' created for project {}", name, projectId);
        return saved;
    }

    @Transactional
    public Sprint startSprint(Long sprintId) {

        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Sprint", sprintId));

        if (sprint.getStatus() != Sprint.SprintStatus.PENDING) {
            throw new BusinessException(
                    "Only a PENDING sprint can be started.");
        }

        // Business rule: only one sprint can be ACTIVE per project
        boolean anotherActive = sprintRepository
                .existsByProjectIdAndStatus(
                        sprint.getProject().getId(),
                        Sprint.SprintStatus.ACTIVE);

        if (anotherActive) {
            throw new BusinessException(
                    "Cannot start sprint — another sprint is already " +
                            "active in this project. Complete it first.");
        }

        sprint.setStatus(Sprint.SprintStatus.ACTIVE);
        log.info("Sprint {} started", sprintId);
        return sprintRepository.save(sprint);
    }

    @Transactional
    public Sprint completeSprint(Long sprintId) {

        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Sprint", sprintId));

        if (sprint.getStatus() != Sprint.SprintStatus.ACTIVE) {
            throw new BusinessException(
                    "Only an ACTIVE sprint can be completed.");
        }

        // Move all unfinished issues back to backlog
        List<Issue> unfinished = sprint.getIssues().stream()
                .filter(i -> i.getStatus() != Issue.IssueStatus.DONE)
                .collect(Collectors.toList());

        unfinished.forEach(i -> i.setSprint(null));
        issueRepository.saveAll(unfinished);

        sprint.setStatus(Sprint.SprintStatus.COMPLETED);

        log.info("Sprint {} completed. {} issues moved to backlog.",
                sprintId, unfinished.size());
        return sprintRepository.save(sprint);
    }

    @Transactional
    public void addIssueToSprint(Long sprintId, Long issueId) {

        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Sprint", sprintId));

        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Issue", issueId));

        if (issue.getSprint() != null) {
            throw new BusinessException(
                    "Issue is already assigned to a sprint.");
        }

        issue.setSprint(sprint);
        issueRepository.save(issue);
        log.info("Issue {} added to sprint {}", issueId, sprintId);
    }

    // Returns issues grouped by status for the Kanban board
    @Transactional(readOnly = true)
    public Map<Issue.IssueStatus, List<Issue>> getBoardData(
            Long projectId) {

        Sprint activeSprint = sprintRepository
                .findByProjectIdAndStatus(
                        projectId, Sprint.SprintStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(
                        "No active sprint found for this project."));

        List<Issue> issues = issueRepository
                .findBySprintIdOrderByCreatedAtAsc(activeSprint.getId());

        return issues.stream()
                .collect(Collectors.groupingBy(Issue::getStatus));
    }

    @Transactional(readOnly = true)
    public List<Sprint> getSprintsForProject(Long projectId) {
        return sprintRepository
                .findByProjectIdOrderByCreatedAtDesc(projectId);
    }
}