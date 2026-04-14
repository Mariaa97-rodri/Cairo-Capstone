package com.cairo.cairobackend.repository;

import com.cairo.cairobackend.entity.Sprint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface SprintRepository extends JpaRepository<Sprint, Long> {

    List<Sprint> findByProjectIdOrderByCreatedAtDesc(Long projectId);

    // Used by the Kanban board — finds whichever sprint is ACTIVE
    Optional<Sprint> findByProjectIdAndStatus(Long projectId, Sprint.SprintStatus status);

    // Used before starting a sprint — ensures no other sprint is active
    boolean existsByProjectIdAndStatus(Long projectId, Sprint.SprintStatus status);
}