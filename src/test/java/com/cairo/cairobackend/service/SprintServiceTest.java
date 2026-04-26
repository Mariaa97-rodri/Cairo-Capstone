package com.cairo.cairobackend.service;

import com.cairo.cairobackend.entity.Issue;
import com.cairo.cairobackend.entity.Project;
import com.cairo.cairobackend.entity.Sprint;
import com.cairo.cairobackend.exception.BusinessException;
import com.cairo.cairobackend.exception.ResourceNotFoundException;
import com.cairo.cairobackend.repository.IssueRepository;
import com.cairo.cairobackend.repository.ProjectRepository;
import com.cairo.cairobackend.repository.SprintRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SprintServiceTest {

    @Mock SprintRepository  sprintRepository;
    @Mock IssueRepository   issueRepository;
    @Mock ProjectRepository projectRepository;

    @InjectMocks SprintService sprintService;

    private Project project;
    private Sprint  pendingSprint;
    private Sprint  activeSprint;

    @BeforeEach
    void setUp() {
        project = Project.builder().id(1L).name("Cairo Backend").build();

        pendingSprint = Sprint.builder()
                .id(1L).name("Sprint 1").project(project)
                .status(Sprint.SprintStatus.PENDING)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(14))
                .build();

        activeSprint = Sprint.builder()
                .id(2L).name("Sprint Active").project(project)
                .status(Sprint.SprintStatus.ACTIVE)
                .issues(new ArrayList<>())
                .build();
    }

    // ── createSprint ──────────────────────────────────────────────

    @Test
    void createSprint_success_returnsPendingSprint() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(projectRepository.getReferenceById(1L)).thenReturn(project);
        when(sprintRepository.save(any(Sprint.class))).thenReturn(pendingSprint);

        Sprint result = sprintService.createSprint(
                1L, "Sprint 1", LocalDate.now(), LocalDate.now().plusDays(14));

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(Sprint.SprintStatus.PENDING);
        verify(sprintRepository).save(any(Sprint.class));
    }

    @Test
    void createSprint_projectNotFound_throwsResourceNotFoundException() {
        when(projectRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                sprintService.createSprint(99L, "Sprint 1", null, null))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(sprintRepository, never()).save(any());
    }

    @Test
    void createSprint_nullDates_savedSuccessfully() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(projectRepository.getReferenceById(1L)).thenReturn(project);
        when(sprintRepository.save(any(Sprint.class))).thenReturn(pendingSprint);

        Sprint result = sprintService.createSprint(1L, "No Dates Sprint", null, null);

        assertThat(result).isNotNull();
    }

    // ── updateSprint ──────────────────────────────────────────────

    @Test
    void updateSprint_nameOnly_updated() {
        when(sprintRepository.findById(1L)).thenReturn(Optional.of(pendingSprint));
        when(sprintRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Sprint result = sprintService.updateSprint(1L, "Updated Sprint Name", null, null);

        assertThat(result.getName()).isEqualTo("Updated Sprint Name");
    }

    @Test
    void updateSprint_datesUpdated() {
        LocalDate newStart = LocalDate.now().plusDays(1);
        LocalDate newEnd = LocalDate.now().plusDays(15);
        
        when(sprintRepository.findById(1L)).thenReturn(Optional.of(pendingSprint));
        when(sprintRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Sprint result = sprintService.updateSprint(
                1L, null, 
                newStart.toString(), 
                newEnd.toString());

        assertThat(result.getStartDate()).isEqualTo(newStart);
        assertThat(result.getEndDate()).isEqualTo(newEnd);
    }

    @Test
    void updateSprint_activeSprintCannotBeEdited() {
        when(sprintRepository.findById(2L)).thenReturn(Optional.of(activeSprint));

        assertThatThrownBy(() -> sprintService.updateSprint(2L, "Update", null, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("PENDING");

        verify(sprintRepository, never()).save(any());
    }

    @Test
    void updateSprint_notFound_throwsResourceNotFoundException() {
        when(sprintRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sprintService.updateSprint(99L, "Name", null, null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── startSprint ───────────────────────────────────────────────

    @Test
    void startSprint_pendingNoOtherActive_becomesActive() {
        when(sprintRepository.findById(1L)).thenReturn(Optional.of(pendingSprint));
        when(sprintRepository.existsByProjectIdAndStatus(1L, Sprint.SprintStatus.ACTIVE))
                .thenReturn(false);
        when(sprintRepository.save(any(Sprint.class))).thenAnswer(inv -> inv.getArgument(0));

        Sprint result = sprintService.startSprint(1L);

        assertThat(result.getStatus()).isEqualTo(Sprint.SprintStatus.ACTIVE);
    }

    @Test
    void startSprint_notPending_throwsBusinessException() {
        when(sprintRepository.findById(2L)).thenReturn(Optional.of(activeSprint));

        assertThatThrownBy(() -> sprintService.startSprint(2L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("PENDING");
    }

    @Test
    void startSprint_anotherAlreadyActive_throwsBusinessException() {
        when(sprintRepository.findById(1L)).thenReturn(Optional.of(pendingSprint));
        when(sprintRepository.existsByProjectIdAndStatus(1L, Sprint.SprintStatus.ACTIVE))
                .thenReturn(true);

        assertThatThrownBy(() -> sprintService.startSprint(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("active");
    }

    @Test
    void startSprint_notFound_throwsResourceNotFoundException() {
        when(sprintRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sprintService.startSprint(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── completeSprint ────────────────────────────────────────────

    @Test
    void completeSprint_movesUnfinishedIssuesToBacklog() {
        Issue doneIssue = Issue.builder().id(1L)
                .status(Issue.IssueStatus.DONE).sprint(activeSprint).build();
        Issue inProgressIssue = Issue.builder().id(2L)
                .status(Issue.IssueStatus.IN_PROGRESS).sprint(activeSprint).build();
        activeSprint.setIssues(List.of(doneIssue, inProgressIssue));

        when(sprintRepository.findById(2L)).thenReturn(Optional.of(activeSprint));
        when(sprintRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(issueRepository.saveAll(anyList())).thenReturn(List.of(inProgressIssue));

        Sprint result = sprintService.completeSprint(2L);

        assertThat(result.getStatus()).isEqualTo(Sprint.SprintStatus.COMPLETED);
        // Unfinished issue moved to backlog (sprint set null)
        assertThat(inProgressIssue.getSprint()).isNull();
        // Done issue keeps its sprint reference
        assertThat(doneIssue.getSprint()).isNotNull();
    }

    @Test
    void completeSprint_allIssuesDone_noIssuesmoved() {
        Issue doneIssue = Issue.builder().id(1L)
                .status(Issue.IssueStatus.DONE).sprint(activeSprint).build();
        activeSprint.setIssues(List.of(doneIssue));

        when(sprintRepository.findById(2L)).thenReturn(Optional.of(activeSprint));
        when(sprintRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(issueRepository.saveAll(anyList())).thenReturn(List.of());

        Sprint result = sprintService.completeSprint(2L);

        assertThat(result.getStatus()).isEqualTo(Sprint.SprintStatus.COMPLETED);
//        verify(issueRepository).saveAll(argThat(List::isEmpty));
    }

    @Test
    void completeSprint_notActive_throwsBusinessException() {
        when(sprintRepository.findById(1L)).thenReturn(Optional.of(pendingSprint));

        assertThatThrownBy(() -> sprintService.completeSprint(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ACTIVE");
    }

    @Test
    void completeSprint_notFound_throwsResourceNotFoundException() {
        when(sprintRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sprintService.completeSprint(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── addIssueToSprint ──────────────────────────────────────────

    @Test
    void addIssueToSprint_success_issueAssigned() {
        Issue unassigned = Issue.builder().id(3L).sprint(null).build();
        when(sprintRepository.findById(2L)).thenReturn(Optional.of(activeSprint));
        when(issueRepository.findById(3L)).thenReturn(Optional.of(unassigned));

        sprintService.addIssueToSprint(2L, 3L);

        assertThat(unassigned.getSprint()).isEqualTo(activeSprint);
        verify(issueRepository).save(unassigned);
    }

//    @Test
//    void addIssueToSprint_alreadyInSprint_throwsBusinessException() {
//        Issue alreadyAssigned = Issue.builder().id(3L).sprint(activeSprint).build();
//        when(sprintRepository.findById(2L)).thenReturn(Optional.of(activeSprint));
//        when(issueRepository.findById(3L)).thenReturn(Optional.of(alreadyAssigned));
//
//        assertThatThrownBy(() -> sprintService.addIssueToSprint(2L, 3L))
//                .isInstanceOf(BusinessException.class)
//                .hasMessageContaining("already assigned");
//    }

    @Test
    void addIssueToSprint_sprintNotFound_throwsResourceNotFoundException() {
        when(sprintRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sprintService.addIssueToSprint(99L, 3L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void addIssueToSprint_issueNotFound_throwsResourceNotFoundException() {
        when(sprintRepository.findById(2L)).thenReturn(Optional.of(activeSprint));
        when(issueRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sprintService.addIssueToSprint(2L, 99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── getBoardData ──────────────────────────────────────────────

    @Test
    void getBoardData_activeSprint_returnsIssuesGroupedByStatus() {
        Issue todoIssue = Issue.builder().id(1L)
                .status(Issue.IssueStatus.TODO).build();
        Issue inProgressIssue = Issue.builder().id(2L)
                .status(Issue.IssueStatus.IN_PROGRESS).build();

        when(sprintRepository.findByProjectIdAndStatus(
                1L, Sprint.SprintStatus.ACTIVE))
                .thenReturn(Optional.of(activeSprint));
        when(issueRepository.findBySprintIdOrderByCreatedAtAsc(2L))
                .thenReturn(List.of(todoIssue, inProgressIssue));

        Map<Issue.IssueStatus, List<Issue>> board = sprintService.getBoardData(1L);

        assertThat(board.get(Issue.IssueStatus.TODO)).hasSize(1);
        assertThat(board.get(Issue.IssueStatus.IN_PROGRESS)).hasSize(1);
    }

    @Test
    void getBoardData_includesAllStatusColumns_evenIfEmpty() {
        when(sprintRepository.findByProjectIdAndStatus(
                1L, Sprint.SprintStatus.ACTIVE))
                .thenReturn(Optional.of(activeSprint));
        when(issueRepository.findBySprintIdOrderByCreatedAtAsc(2L))
                .thenReturn(List.of());

        Map<Issue.IssueStatus, List<Issue>> board = sprintService.getBoardData(1L);

        // All statuses should be present
        assertThat(board).containsKeys(
                Issue.IssueStatus.TODO,
                Issue.IssueStatus.IN_PROGRESS,
                Issue.IssueStatus.IN_REVIEW,
                Issue.IssueStatus.DONE
        );
    }

    @Test
    void getBoardData_noActiveSprint_throwsBusinessException() {
        when(sprintRepository.findByProjectIdAndStatus(
                1L, Sprint.SprintStatus.ACTIVE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> sprintService.getBoardData(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("No active sprint");
    }

    // ── getSprintsForProject ──────────────────────────────────────

    @Test
    void getSprintsForProject_returnsListFromRepository() {
        when(sprintRepository.findByProjectIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(pendingSprint, activeSprint));

        List<Sprint> result = sprintService.getSprintsForProject(1L);

        assertThat(result).hasSize(2);
    }
}