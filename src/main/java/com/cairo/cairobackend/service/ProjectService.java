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

    @Transactional(readOnly = true)
    public Project getProjectById(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project", projectId));
    }

    @Transactional
    public void addMember(Long projectId, Long userId, User requestingUser) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project", projectId));

        // Only the project owner can add members
        if (!project.getOwner().getId().equals(requestingUser.getId())) {
            throw new UnauthorizedException(
                    "Only the project owner can add members.");
        }

        if (projectMemberRepository.existsByProjectIdAndUserId(
                projectId, userId)) {
            throw new BusinessException("User is already a member.");
        }

        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User", userId));

        ProjectMember member = ProjectMember.builder()
                .projectId(projectId)
                .userId(userId)
                .build();

        projectMemberRepository.save(member);
        log.info("User {} added to project {}", userId, projectId);
    }

    @Transactional
    public void removeMember(Long projectId, Long userId,
                             User requestingUser) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project", projectId));

        if (!project.getOwner().getId().equals(requestingUser.getId())) {
            throw new UnauthorizedException(
                    "Only the project owner can remove members.");
        }

        // Owner cannot remove themselves
        if (project.getOwner().getId().equals(userId)) {
            throw new BusinessException(
                    "Cannot remove the project owner from the project.");
        }

        projectMemberRepository.deleteByProjectIdAndUserId(
                projectId, userId);
        log.info("User {} removed from project {}", userId, projectId);
    }

    @Transactional
    public void deleteProject(Long projectId, User requestingUser) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project", projectId));

        if (!project.getOwner().getId().equals(requestingUser.getId())) {
            throw new UnauthorizedException(
                    "Only the project owner can delete the project.");
        }

        projectRepository.delete(project);
        log.info("Project {} deleted by {}", projectId,
                requestingUser.getEmail());
    }
}