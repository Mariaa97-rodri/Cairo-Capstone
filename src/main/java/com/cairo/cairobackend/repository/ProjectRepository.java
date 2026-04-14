package com.cairo.cairobackend.repository;

import com.cairo.cairobackend.entity.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    // Finds all projects a user belongs to by looking at
    // the project_members join table.
    // JPQL uses entity class names and field names —
    // not table names and column names.
    @Query("""
        SELECT p FROM Project p
        JOIN ProjectMember pm ON pm.project.id = p.id
        WHERE pm.userId = :userId
        ORDER BY p.createdAt DESC
        """)
    Page<Project> findAllByMemberId(
            @Param("userId") Long userId,
            Pageable pageable);

    // Used when creating a project to prevent duplicate keys
    boolean existsByProjectKey(String projectKey);
}