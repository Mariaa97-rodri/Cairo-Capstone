package com.cairo.cairobackend.service;

import com.cairo.cairobackend.entity.*;
import com.cairo.cairobackend.exception.*;
import com.cairo.cairobackend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class IssueService {

    private final IssueRepository issueRepository;
    private final ProjectRepository projectRepository;
    private final SprintRepository sprintRepository;
    private final UserRepository userRepository;
    private final IssueHistoryRepository historyRepository;
    private final NotificationRepository notificationRepository;

    @Transactional
    public Issue createIssue(Long projectId,
                             Long reporterId,
                             String title,
                             String description,
                             Issue.IssueType type,
                             Issue.Priority priority,
                             Long assigneeId,
                             Long sprintId,
                             Integer storyPoints) {

        log.info("Creating issue '{}' in project {}", title, projectId);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project", projectId));

        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User", reporterId));

        User assignee = null;
        if (assigneeId != null) {
            assignee = userRepository.findById(assigneeId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "User", assigneeId));
        }

        Sprint sprint = null;
        if (sprintId != null) {
            sprint = sprintRepository.findById(sprintId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Sprint", sprintId));
        }

        Issue issue = Issue.builder()
                .project(project)
                .reporter(reporter)
                .assignee(assignee)
                .sprint(sprint)
                .title(title)
                .description(description)
                .type(type)
                .priority(priority)
                .storyPoints(storyPoints != null ? storyPoints : 0)
                .build();

        Issue saved = issueRepository.save(issue);

        // Notify assignee if someone else created and assigned the issue
        if (assignee != null &&
                !assignee.getId().equals(reporterId)) {

            Notification notification = Notification.builder()
                    .user(assignee)
                    .issue(saved)
                    .type(Notification.NotificationType.ASSIGNED)
                    .message(reporter.getName()
                            + " assigned you to: " + title)
                    .build();
            notificationRepository.save(notification);
        }

        log.info("Issue created with id {}", saved.getId());
        return saved;
    }

    @Transactional
    public Issue updateStatus(Long issueId,
                              Issue.IssueStatus newStatus,
                              User changedBy) {

        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Issue", issueId));

        String oldStatus = issue.getStatus().name();

        // Write to audit trail before updating
        IssueHistory historyEntry = IssueHistory.builder()
                .issue(issue)
                .changedBy(changedBy)
                .fieldName("status")
                .oldValue(oldStatus)
                .newValue(newStatus.name())
                .build();
        historyRepository.save(historyEntry);

        issue.setStatus(newStatus);

        // Notify assignee of the status change
        if (issue.getAssignee() != null) {
            Notification notification = Notification.builder()
                    .user(issue.getAssignee())
                    .issue(issue)
                    .type(Notification.NotificationType.STATUS_CHANGED)
                    .message("Issue '" + issue.getTitle()
                            + "' moved to " + newStatus.name())
                    .build();
            notificationRepository.save(notification);
        }

        log.info("Issue {} status changed from {} to {}",
                issueId, oldStatus, newStatus);
        return issueRepository.save(issue);
    }

    @Transactional
    public Issue updateIssue(Long issueId,
                             String title,
                             String description,
                             Issue.IssueType type,
                             Issue.Priority priority,
                             Long assigneeId,
                             Long sprintId,
                             Integer storyPoints,
                             User changedBy) {

        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Issue", issueId));

        if (title != null) issue.setTitle(title);
        if (description != null) issue.setDescription(description);
        if (type != null) issue.setType(type);
        if (priority != null) issue.setPriority(priority);
        if (storyPoints != null) issue.setStoryPoints(storyPoints);

        if (assigneeId != null) {
            User assignee = userRepository.findById(assigneeId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "User", assigneeId));
            issue.setAssignee(assignee);
        }

        if (sprintId != null) {
            Sprint sprint = sprintRepository.findById(sprintId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Sprint", sprintId));
            issue.setSprint(sprint);
        }

        log.info("Issue {} updated by {}", issueId,
                changedBy.getEmail());
        return issueRepository.save(issue);
    }

    @Transactional(readOnly = true)
    public Page<Issue> getProjectIssues(Long projectId,
                                        Issue.IssueType type,
                                        Issue.Priority priority,
                                        Issue.IssueStatus status,
                                        Long assigneeId,
                                        Pageable pageable) {
        return issueRepository.findByProjectWithFilters(
                projectId, type, priority, status, assigneeId, pageable);
    }

    @Transactional(readOnly = true)
    public Issue getIssueById(Long issueId) {
        return issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Issue", issueId));
    }

    @Transactional(readOnly = true)
    public List<Issue> getSprintIssues(Long sprintId) {
        return issueRepository.findBySprintIdOrderByCreatedAtAsc(sprintId);
    }

    @Transactional
    public void deleteIssue(Long issueId) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Issue", issueId));
        issueRepository.delete(issue);
        log.info("Issue {} deleted", issueId);
    }
}