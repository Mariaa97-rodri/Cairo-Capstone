package com.cairo.cairobackend.service;

import com.cairo.cairobackend.entity.Project;
import com.cairo.cairobackend.entity.ProjectMember;
import com.cairo.cairobackend.entity.ProjectMemberId;
import com.cairo.cairobackend.entity.User;
import com.cairo.cairobackend.exception.BusinessException;
import com.cairo.cairobackend.exception.DuplicateResourceException;
import com.cairo.cairobackend.exception.ResourceNotFoundException;
import com.cairo.cairobackend.exception.UnauthorizedException;
import com.cairo.cairobackend.repository.ProjectMemberRepository;
import com.cairo.cairobackend.repository.ProjectRepository;
import com.cairo.cairobackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;

    // ── Helper: check if user is ADMIN or the project owner
    private boolean isAdminOrOwner(User user, Project project) {
        return user.getRole() == User.Role.ADMIN ||
                project.getOwner().getId().equals(user.getId());
    }

    @Transactional
    public Project createProject(String name, String projectKey,
                                 String description, User owner) {

        if (projectRepository.existsByProjectKey(projectKey)) {
            throw new DuplicateResourceException(
                    "Project key '" + projectKey + "' is already taken.");
        }

        Project project = Project.builder()
                .name(name)
                .projectKey(projectKey.toUpperCase())
                .description(description)
                .owner(owner)
                .build();

        Project saved = projectRepository.save(project);

        // Automatically add the owner as a member of their own project
        ProjectMember ownerMember = ProjectMember.builder()
                .projectId(saved.getId())
                .userId(owner.getId())
                .build();
        projectMemberRepository.save(ownerMember);

        log.info("Project '{}' created by {}", name, owner.getEmail());
        return saved;
    }

    @Transactional(readOnly = true)
    public Page<Project> getProjectsForUser(Long userId, Pageable pageable) {
        return projectRepository.findAllByMemberId(userId, pageable);
    }

    // ADMIN can see any project, not just ones they belong to
    @Transactional(readOnly = true)
    public Page<Project> getAllProjects(Pageable pageable) {
        return projectRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Project getProjectById(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project", projectId));
    }

    // ADMIN or project owner can add members
    @Transactional
    public void addMember(Long projectId, Long userId, User requestingUser) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project", projectId));

        if (!isAdminOrOwner(requestingUser, project)) {
            throw new UnauthorizedException(
                    "Only the project owner or an admin can add members.");
        }

        if (projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)) {
            throw new BusinessException("User is already a member.");
        }

        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        projectMemberRepository.save(
                ProjectMember.builder()
                        .projectId(projectId)
                        .userId(userId)
                        .build()
        );
        log.info("User {} added to project {}", userId, projectId);
    }

    // ADMIN or project owner can remove members
    @Transactional
    public void removeMember(Long projectId, Long userId, User requestingUser) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project", projectId));

        if (!isAdminOrOwner(requestingUser, project)) {
            throw new UnauthorizedException(
                    "Only the project owner or an admin can remove members.");
        }

        // Owner cannot be removed from their own project
        if (project.getOwner().getId().equals(userId)) {
            throw new BusinessException(
                    "Cannot remove the project owner from the project.");
        }

        projectMemberRepository.deleteByProjectIdAndUserId(projectId, userId);
        log.info("User {} removed from project {}", userId, projectId);
    }

    // ADMIN or project owner can delete a project
    @Transactional
    public void deleteProject(Long projectId, User requestingUser) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project", projectId));

        if (!isAdminOrOwner(requestingUser, project)) {
            throw new UnauthorizedException(
                    "Only the project owner or an admin can delete the project.");
        }

        projectRepository.delete(project);
        log.info("Project {} deleted by {}", projectId, requestingUser.getEmail());
    }
}