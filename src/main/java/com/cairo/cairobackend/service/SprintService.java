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
    public Sprint createSprint(Long projectId, String name,
                               LocalDate startDate, LocalDate endDate) {
        projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));

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

    // Edit name and dates of a PENDING sprint only
    @Transactional
    public Sprint updateSprint(Long sprintId, String name,
                               String startDate, String endDate) {
        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new ResourceNotFoundException("Sprint", sprintId));

        if (sprint.getStatus() != Sprint.SprintStatus.PENDING) {
            throw new BusinessException(
                    "Only PENDING sprints can be edited. Start or complete this sprint first.");
        }

        if (name != null && !name.isBlank()) sprint.setName(name);
        if (startDate != null && !startDate.isBlank()) sprint.setStartDate(LocalDate.parse(startDate));
        if (endDate   != null && !endDate.isBlank())   sprint.setEndDate(LocalDate.parse(endDate));

        log.info("Sprint {} updated", sprintId);
        return sprintRepository.save(sprint);
    }

    @Transactional
    public Sprint startSprint(Long sprintId) {
        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new ResourceNotFoundException("Sprint", sprintId));

        if (sprint.getStatus() != Sprint.SprintStatus.PENDING) {
            throw new BusinessException("Only a PENDING sprint can be started.");
        }

        boolean anotherActive = sprintRepository
                .existsByProjectIdAndStatus(
                        sprint.getProject().getId(), Sprint.SprintStatus.ACTIVE);

        if (anotherActive) {
            throw new BusinessException(
                    "Cannot start sprint — another sprint is already active. Complete it first.");
        }

        sprint.setStatus(Sprint.SprintStatus.ACTIVE);
        log.info("Sprint {} started", sprintId);
        return sprintRepository.save(sprint);
    }

    @Transactional
    public Sprint completeSprint(Long sprintId) {
        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new ResourceNotFoundException("Sprint", sprintId));

        if (sprint.getStatus() != Sprint.SprintStatus.ACTIVE) {
            throw new BusinessException("Only an ACTIVE sprint can be completed.");
        }

        List<Issue> unfinished = sprint.getIssues().stream()
                .filter(i -> i.getStatus() != Issue.IssueStatus.DONE)
                .collect(Collectors.toList());

        unfinished.forEach(i -> i.setSprint(null));
        issueRepository.saveAll(unfinished);

        sprint.setStatus(Sprint.SprintStatus.COMPLETED);
        log.info("Sprint {} completed. {} issues moved to backlog.", sprintId, unfinished.size());
        return sprintRepository.save(sprint);
    }

    @Transactional
    public void addIssueToSprint(Long sprintId, Long issueId) {
        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new ResourceNotFoundException("Sprint", sprintId));
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue", issueId));
        // Allow reassigning — just overwrite whatever sprint it was in
        issue.setSprint(sprint);
        issueRepository.save(issue);
        log.info("Issue {} assigned to sprint {}", issueId, sprintId);
    }

    @Transactional(readOnly = true)
    public Map<Issue.IssueStatus, List<Issue>> getBoardData(Long projectId) {
        Sprint activeSprint = sprintRepository
                .findByProjectIdAndStatus(projectId, Sprint.SprintStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(
                        "No active sprint found. Start a sprint first to use the board."));

        List<Issue> issues = issueRepository
                .findBySprintIdOrderByCreatedAtAsc(activeSprint.getId());

        Map<Issue.IssueStatus, List<Issue>> board = issues.stream()
                .collect(Collectors.groupingBy(Issue::getStatus));

        // Always return all 4 columns even if empty
        for (Issue.IssueStatus status : Issue.IssueStatus.values()) {
            board.putIfAbsent(status, List.of());
        }
        return board;
    }

    @Transactional(readOnly = true)
    public List<Sprint> getSprintsForProject(Long projectId) {
        return sprintRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
    }
}