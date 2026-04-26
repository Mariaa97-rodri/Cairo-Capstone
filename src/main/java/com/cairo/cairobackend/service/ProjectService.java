package com.cairo.cairobackend.service;

import com.cairo.cairobackend.dto.response.UserResponse;
import com.cairo.cairobackend.entity.Notification;
import com.cairo.cairobackend.entity.Project;
import com.cairo.cairobackend.entity.ProjectMember;
import com.cairo.cairobackend.entity.User;
import com.cairo.cairobackend.exception.BusinessException;
import com.cairo.cairobackend.exception.DuplicateResourceException;
import com.cairo.cairobackend.exception.ResourceNotFoundException;
import com.cairo.cairobackend.exception.UnauthorizedException;
import com.cairo.cairobackend.repository.NotificationRepository;
import com.cairo.cairobackend.repository.ProjectMemberRepository;
import com.cairo.cairobackend.repository.ProjectRepository;
import com.cairo.cairobackend.repository.UserRepository;
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
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;

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
                .name(name).projectKey(projectKey.toUpperCase())
                .description(description).owner(owner).build();
        Project saved = projectRepository.save(project);
        projectMemberRepository.save(
                ProjectMember.builder().projectId(saved.getId()).userId(owner.getId()).build());
        log.info("Project '{}' created by {}", name, owner.getEmail());
        return saved;
    }

    // Edit project name and description — owner or ADMIN only
    @Transactional
    public Project updateProject(Long projectId, String name,
                                 String description, User requestingUser) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));
        if (!isAdminOrOwner(requestingUser, project)) {
            throw new UnauthorizedException(
                    "Only the project owner or an admin can edit the project.");
        }
        if (name != null && !name.isBlank()) project.setName(name);
        if (description != null) project.setDescription(description);
        log.info("Project {} updated by {}", projectId, requestingUser.getEmail());
        return projectRepository.save(project);
    }

    @Transactional(readOnly = true)
    public Page<Project> getProjectsForUser(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        // Admin sees ALL projects
        if (user.getRole() == User.Role.ADMIN) {
            return projectRepository.findAll(pageable);
        }

        // Regular users only see projects they are members of
        return projectRepository.findAllByMemberId(userId, pageable);
    }

    @Transactional(readOnly = true)
    public Project getProjectById(Long projectId, User requestingUser) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));

        // Admin can see any project
        if (requestingUser.getRole() == User.Role.ADMIN) return project;

        // Regular users must be a member
        boolean isMember = projectMemberRepository
                .existsByProjectIdAndUserId(projectId, requestingUser.getId());
        if (!isMember) {
            throw new UnauthorizedException("You are not a member of this project.");
        }
        return project;
    }

    @Transactional
    public void addMember(Long projectId, Long userId, User requestingUser) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));
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
                ProjectMember.builder().projectId(projectId).userId(userId).build());
        log.info("User {} added to project {}", userId, projectId);
    }

    @Transactional
    public void removeMember(Long projectId, Long userId, User requestingUser) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));
        if (!isAdminOrOwner(requestingUser, project)) {
            throw new UnauthorizedException(
                    "Only the project owner or an admin can remove members.");
        }
        if (project.getOwner().getId().equals(userId)) {
            throw new BusinessException("Cannot remove the project owner from the project.");
        }

        User removedUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        projectMemberRepository.deleteByProjectIdAndUserId(projectId, userId);

        // Notify the removed user
        notificationRepository.save(Notification.builder()
                .user(removedUser)
                .issue(null)
                .type(Notification.NotificationType.ASSIGNED)
                .message("You have been removed from project: " + project.getName())
                .build());

        log.info("User {} removed from project {}", userId, projectId);
    }

    @Transactional
    public void deleteProject(Long projectId, User requestingUser) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));
        if (!isAdminOrOwner(requestingUser, project)) {
            throw new UnauthorizedException(
                    "Only the project owner or an admin can delete the project.");
        }
        projectRepository.delete(project);
        log.info("Project {} deleted by {}", projectId, requestingUser.getEmail());
    }
    @Transactional(readOnly = true)
    public List<UserResponse> getMembers(Long projectId) {
        projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));
        return projectMemberRepository.findByProjectId(projectId)
                .stream()
                .map(pm -> userRepository.findById(pm.getUserId()).orElse(null))
                .filter(u -> u != null)
                .map(UserResponse::from)
                .collect(java.util.stream.Collectors.toList());
    }
}