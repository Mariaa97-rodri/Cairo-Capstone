package com.cairo.cairobackend.repository;

import com.cairo.cairobackend.entity.Issue;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface IssueRepository extends JpaRepository<Issue, Long> {

    // Backlog = issues with no sprint assigned
    Page<Issue> findByProjectIdAndSprintIsNull(Long projectId, Pageable pageable);

    // Issues for a specific sprint (Kanban board)
    List<Issue> findBySprintIdOrderByCreatedAtAsc(Long sprintId);

    // Filtered list — used by backlog filter dropdowns
    @Query("""
        SELECT i FROM Issue i
        WHERE i.project.id = :projectId
        AND (:type IS NULL OR i.type = :type)
        AND (:priority IS NULL OR i.priority = :priority)
        AND (:status IS NULL OR i.status = :status)
        AND (:assigneeId IS NULL OR i.assignee.id = :assigneeId)
        """)
    Page<Issue> findByProjectWithFilters(
            @Param("projectId")  Long projectId,
            @Param("type")       Issue.IssueType type,
            @Param("priority")   Issue.Priority priority,
            @Param("status")     Issue.IssueStatus status,
            @Param("assigneeId") Long assigneeId,
            Pageable pageable
    );

    // Used by AI smart assignee — count open issues per user in a project
    @Query("""
        SELECT i.assignee.id, COUNT(i)
        FROM Issue i
        WHERE i.project.id = :projectId
        AND i.assignee IS NOT NULL
        AND i.status != 'DONE'
        GROUP BY i.assignee.id
        """)
    List<Object[]> countOpenIssuesByAssigneeInProject(@Param("projectId") Long projectId);
}
