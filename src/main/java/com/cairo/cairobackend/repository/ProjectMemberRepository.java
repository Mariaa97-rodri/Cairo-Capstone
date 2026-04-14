package com.cairo.cairobackend.repository;

import com.cairo.cairobackend.entity.ProjectMember;
import com.cairo.cairobackend.entity.ProjectMemberId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectMemberRepository
        extends JpaRepository<ProjectMember, ProjectMemberId> {

    // Used in ProjectService to check if a user
    // is already a member before adding them
    boolean existsByProjectIdAndUserId(Long projectId, Long userId);

    // Used in ProjectService to remove a member
    void deleteByProjectIdAndUserId(Long projectId, Long userId);
}