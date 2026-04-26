package com.cairo.cairobackend.service;

import com.cairo.cairobackend.dto.response.IssueHistoryResponse;
import com.cairo.cairobackend.entity.*;
import com.cairo.cairobackend.exception.ResourceNotFoundException;
import com.cairo.cairobackend.exception.UnauthorizedException;
import com.cairo.cairobackend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IssueServiceTest {

    @Mock IssueRepository        issueRepository;
    @Mock ProjectRepository      projectRepository;
    @Mock SprintRepository       sprintRepository;
    @Mock UserRepository         userRepository;
    @Mock IssueHistoryRepository historyRepository;
    @Mock NotificationRepository notificationRepository;

    @InjectMocks IssueService issueService;

    private User    reporter;
    private User    assignee;
    private Project project;
    private Issue   issue;

    @BeforeEach
    void setUp() {
        reporter = User.builder()
                .id(1L).name("Maria").email("maria@cairo.com")
                .role(User.Role.USER).build();

        assignee = User.builder()
                .id(2L).name("John").email("john@cairo.com")
                .role(User.Role.USER).build();

        project = Project.builder()
                .id(1L).name("Cairo Backend").build();

        issue = Issue.builder()
                .id(1L).title("Fix login bug")
                .type(Issue.IssueType.BUG)
                .priority(Issue.Priority.HIGH)
                .status(Issue.IssueStatus.TODO)
                .project(project)
                .reporter(reporter)
                .assignee(assignee)
                .storyPoints(3)
                .build();
    }

    // ── createIssue ───────────────────────────────────────────────

    @Test
    void createIssue_withAssignee_savesIssueAndSendsNotification() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(userRepository.findById(1L)).thenReturn(Optional.of(reporter));
        when(userRepository.findById(2L)).thenReturn(Optional.of(assignee));
        when(issueRepository.save(any(Issue.class))).thenReturn(issue);

        Issue result = issueService.createIssue(
                1L, 1L, "Fix login bug", null,
                Issue.IssueType.BUG, Issue.Priority.HIGH,
                2L, null, 3);

        assertThat(result.getTitle()).isEqualTo("Fix login bug");
        // assignee (id=2) ≠ reporter (id=1) → notification sent
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void createIssue_selfAssign_noNotificationSent() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(userRepository.findById(1L)).thenReturn(Optional.of(reporter));
        when(issueRepository.save(any(Issue.class))).thenReturn(issue);

        // reporter (id=1) assigns to themselves (assigneeId=1)
        issueService.createIssue(
                1L, 1L, "Title", null,
                Issue.IssueType.TASK, Issue.Priority.LOW,
                1L, null, 0);

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void createIssue_noAssignee_noNotificationSent() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(userRepository.findById(1L)).thenReturn(Optional.of(reporter));
        when(issueRepository.save(any(Issue.class))).thenReturn(issue);

        issueService.createIssue(
                1L, 1L, "Title", null,
                Issue.IssueType.TASK, Issue.Priority.LOW,
                null, null, 0);

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void createIssue_withSprint_sprintAssigned() {
        Sprint sprint = Sprint.builder().id(5L).name("Sprint 1")
                .status(Sprint.SprintStatus.ACTIVE).build();

        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(userRepository.findById(1L)).thenReturn(Optional.of(reporter));
        when(sprintRepository.findById(5L)).thenReturn(Optional.of(sprint));
        when(issueRepository.save(any(Issue.class))).thenAnswer(inv -> {
            Issue i = inv.getArgument(0);
            assertThat(i.getSprint()).isEqualTo(sprint);
            return issue;
        });

        issueService.createIssue(
                1L, 1L, "Title", null,
                Issue.IssueType.TASK, Issue.Priority.LOW,
                null, 5L, 0);
    }

    @Test
    void createIssue_projectNotFound_throwsResourceNotFoundException() {
        when(projectRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                issueService.createIssue(
                        99L, 1L, "Title", null,
                        Issue.IssueType.TASK, Issue.Priority.LOW,
                        null, null, 0))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createIssue_reporterNotFound_throwsResourceNotFoundException() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                issueService.createIssue(
                        1L, 99L, "Title", null,
                        Issue.IssueType.TASK, Issue.Priority.LOW,
                        null, null, 0))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createIssue_nullStoryPoints_defaultsToZero() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(userRepository.findById(1L)).thenReturn(Optional.of(reporter));
        when(issueRepository.save(any(Issue.class))).thenAnswer(inv -> {
            Issue i = inv.getArgument(0);
            assertThat(i.getStoryPoints()).isEqualTo(0);
            return issue;
        });

        issueService.createIssue(
                1L, 1L, "Title", null,
                Issue.IssueType.TASK, Issue.Priority.LOW,
                null, null, null);
    }

    // ── updateStatus ──────────────────────────────────────────────

    @Test
    void updateStatus_writesAuditHistoryEntry() {
        when(issueRepository.findById(1L)).thenReturn(Optional.of(issue));
        when(issueRepository.save(any())).thenReturn(issue);

        issueService.updateStatus(1L, Issue.IssueStatus.IN_PROGRESS, reporter);

        ArgumentCaptor<IssueHistory> captor = ArgumentCaptor.forClass(IssueHistory.class);
        verify(historyRepository).save(captor.capture());

        IssueHistory saved = captor.getValue();
        assertThat(saved.getFieldName()).isEqualTo("status");
        assertThat(saved.getOldValue()).isEqualTo("TODO");
        assertThat(saved.getNewValue()).isEqualTo("IN_PROGRESS");
        assertThat(saved.getChangedBy()).isEqualTo(reporter);
    }

    @Test
    void updateStatus_withAssignee_sendsNotification() {
        // issue has assignee set in setUp()
        when(issueRepository.findById(1L)).thenReturn(Optional.of(issue));
        when(issueRepository.save(any())).thenReturn(issue);

        issueService.updateStatus(1L, Issue.IssueStatus.DONE, reporter);

        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void updateStatus_noAssignee_noNotification() {
        issue.setAssignee(null);
        when(issueRepository.findById(1L)).thenReturn(Optional.of(issue));
        when(issueRepository.save(any())).thenReturn(issue);

        issueService.updateStatus(1L, Issue.IssueStatus.IN_PROGRESS, reporter);

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void updateStatus_issueNotFound_throwsResourceNotFoundException() {
        when(issueRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                issueService.updateStatus(99L, Issue.IssueStatus.DONE, reporter))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── updateIssue ───────────────────────────────────────────────

    @Test
    void updateIssue_titleAndDescription_updated() {
        when(issueRepository.findById(1L)).thenReturn(Optional.of(issue));
        when(issueRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Issue result = issueService.updateIssue(
                1L, "Updated title", "New desc",
                null, null, null, null, null, reporter);

        assertThat(result.getTitle()).isEqualTo("Updated title");
        assertThat(result.getDescription()).isEqualTo("New desc");
    }

    @Test
    void updateIssue_withAssigneeId_assigneeChanged() {
        User newAssignee = User.builder().id(3L).name("Sara").build();
        when(issueRepository.findById(1L)).thenReturn(Optional.of(issue));
        when(userRepository.findById(3L)).thenReturn(Optional.of(newAssignee));
        when(issueRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Issue result = issueService.updateIssue(
                1L, null, null, null, null,
                3L, null, null, reporter);

        assertThat(result.getAssignee()).isEqualTo(newAssignee);
    }

    @Test
    void updateIssue_withSprintId_sprintAssigned() {
        Sprint sprint = Sprint.builder().id(5L).name("Sprint 1").build();
        when(issueRepository.findById(1L)).thenReturn(Optional.of(issue));
        when(sprintRepository.findById(5L)).thenReturn(Optional.of(sprint));
        when(issueRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Issue result = issueService.updateIssue(
                1L, null, null, null, null,
                null, 5L, null, reporter);

        assertThat(result.getSprint()).isEqualTo(sprint);
    }

    @Test
    void updateIssue_nullFields_noChanges() {
        when(issueRepository.findById(1L)).thenReturn(Optional.of(issue));
        when(issueRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // All null — nothing should change
        Issue result = issueService.updateIssue(
                1L, null, null, null, null,
                null, null, null, reporter);

        assertThat(result.getTitle()).isEqualTo("Fix login bug");
        assertThat(result.getStoryPoints()).isEqualTo(3);
    }

    @Test
    void updateIssue_issueNotFound_throwsResourceNotFoundException() {
        when(issueRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                issueService.updateIssue(99L, "title", null, null,
                        null, null, null, null, reporter))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── getIssueById ──────────────────────────────────────────────

    @Test
    void getIssueById_found_returnsIssue() {
        when(issueRepository.findById(1L)).thenReturn(Optional.of(issue));

        Issue result = issueService.getIssueById(1L);

        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void getIssueById_notFound_throwsResourceNotFoundException() {
        when(issueRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> issueService.getIssueById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── getProjectIssues ──────────────────────────────────────────

    @Test
    void getProjectIssues_returnsPageFromRepository() {
        Page<Issue> page = new PageImpl<>(List.of(issue));
        when(issueRepository.findByProjectWithFilters(
                eq(1L), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        Page<Issue> result = issueService.getProjectIssues(
                1L, null, null, null, null, Pageable.unpaged());

        assertThat(result).hasSize(1);
    }

    @Test
    void getProjectIssues_withFilters_appliesAllFilters() {
        Page<Issue> page = new PageImpl<>(List.of(issue));
        when(issueRepository.findByProjectWithFilters(
                eq(1L), eq(Issue.IssueType.BUG), eq(Issue.Priority.HIGH), 
                eq(Issue.IssueStatus.TODO), eq(2L), any(Pageable.class)))
                .thenReturn(page);

        Page<Issue> result = issueService.getProjectIssues(
                1L, Issue.IssueType.BUG, Issue.Priority.HIGH, 
                Issue.IssueStatus.TODO, 2L, Pageable.unpaged());

        assertThat(result).hasSize(1);
    }

    @Test
    void getProjectIssues_emptyPage() {
        Page<Issue> page = new PageImpl<>(List.of());
        when(issueRepository.findByProjectWithFilters(
                eq(1L), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        Page<Issue> result = issueService.getProjectIssues(
                1L, null, null, null, null, Pageable.unpaged());

        assertThat(result).isEmpty();
    }

    // ── getSprintIssues ───────────────────────────────────────────

    @Test
    void getSprintIssues_returnsListFromRepository() {
        when(issueRepository.findBySprintIdOrderByCreatedAtAsc(5L))
                .thenReturn(List.of(issue));

        List<Issue> result = issueService.getSprintIssues(5L);

        assertThat(result).hasSize(1);
    }

    // ── deleteIssue ───────────────────────────────────────────────

    @Test
    void updateIssue_withPriorityAndType_updated() {
        when(issueRepository.findById(1L)).thenReturn(Optional.of(issue));
        when(issueRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Issue result = issueService.updateIssue(
                1L, null, null,
                Issue.IssueType.STORY, Issue.Priority.MEDIUM,
                null, null, null, reporter);

        assertThat(result.getType()).isEqualTo(Issue.IssueType.STORY);
        assertThat(result.getPriority()).isEqualTo(Issue.Priority.MEDIUM);
    }

    @Test
    void updateIssue_withStoryPoints_updated() {
        when(issueRepository.findById(1L)).thenReturn(Optional.of(issue));
        when(issueRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Issue result = issueService.updateIssue(
                1L, null, null, null, null,
                null, null, 8, reporter);

        assertThat(result.getStoryPoints()).isEqualTo(8);
    }

    @Test
    void updateIssue_unassigneeBySettingIdToZero() {
        when(issueRepository.findById(1L)).thenReturn(Optional.of(issue));
        when(issueRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Issue result = issueService.updateIssue(
                1L, null, null, null, null,
                0L, null, null, reporter);

        assertThat(result.getAssignee()).isNull();
    }

    @Test
    void updateIssue_moveToBacklogBySettingSprintToZero() {
        issue.setSprint(Sprint.builder().id(5L).build());
        when(issueRepository.findById(1L)).thenReturn(Optional.of(issue));
        when(issueRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Issue result = issueService.updateIssue(
                1L, null, null, null, null,
                null, 0L, null, reporter);

        assertThat(result.getSprint()).isNull();
    }

    @Test
    void updateIssue_withNewAssignee_sendsNotification() {
        User newAssignee = User.builder().id(3L).name("Sara").build();
        when(issueRepository.findById(1L)).thenReturn(Optional.of(issue));
        when(userRepository.findById(3L)).thenReturn(Optional.of(newAssignee));
        when(issueRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        issueService.updateIssue(
                1L, null, null, null, null,
                3L, null, null, reporter);

        verify(notificationRepository).save(any(Notification.class));
    }

    // ── getIssueHistory ───────────────────────────────────────────

//    @Test
//    void getIssueHistory_returnsHistorySortedByDate() {
//        IssueHistory history1 = IssueHistory.builder().id(1L).build();
//        IssueHistory history2 = IssueHistory.builder().id(2L).build();
//        issue.setHistory(List.of(history1, history2));
//
//        when(issueRepository.findById(1L)).thenReturn(Optional.of(issue));
//
//        List<IssueHistoryResponse> result = issueService.getIssueHistory(1L);
//
//        assertThat(result).isNotNull();
//    }

    @Test
    void getIssueHistory_issueNotFound_throwsResourceNotFoundException() {
        when(issueRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> issueService.getIssueHistory(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── deleteIssue ───────────────────────────────────────────────

    @Test
    void deleteIssue_byReporter_success() {
        when(issueRepository.findById(1L)).thenReturn(Optional.of(issue));

        assertThatCode(() -> issueService.deleteIssue(1L, reporter))
                .doesNotThrowAnyException();

        verify(issueRepository).delete(issue);
    }

    @Test
    void deleteIssue_byAdmin_success() {
        User admin = User.builder().id(10L).name("Admin").role(User.Role.ADMIN).build();
        when(issueRepository.findById(1L)).thenReturn(Optional.of(issue));

        assertThatCode(() -> issueService.deleteIssue(1L, admin))
                .doesNotThrowAnyException();

        verify(issueRepository).delete(issue);
    }

    @Test
    void deleteIssue_byOtherUser_throwsUnauthorizedException() {
        when(issueRepository.findById(1L)).thenReturn(Optional.of(issue));

        assertThatThrownBy(() -> issueService.deleteIssue(1L, assignee))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("reporter");

        verify(issueRepository, never()).delete(any());
    }

    @Test
    void deleteIssue_notFound_throwsResourceNotFoundException() {
        when(issueRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> issueService.deleteIssue(99L, reporter))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}