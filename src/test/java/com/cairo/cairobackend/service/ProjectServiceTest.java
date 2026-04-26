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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class ProjectServiceTest {

    @Mock ProjectRepository       projectRepository;
    @Mock ProjectMemberRepository projectMemberRepository;
    @Mock UserRepository          userRepository;
    @Mock NotificationRepository  notificationRepository;

    @InjectMocks ProjectService projectService;

    private User    owner;
    private User    otherUser;
    private Project project;

    @BeforeEach
    void setUp() {
        owner = User.builder()
                .id(1L).name("Maria").email("maria@cairo.com")
                .role(User.Role.USER).build();

        otherUser = User.builder()
                .id(2L).name("John").email("john@cairo.com")
                .role(User.Role.USER).build();

        project = Project.builder()
                .id(1L).name("Cairo Backend")
                .projectKey("CAIROBE").owner(owner).build();
    }

    // ── createProject ─────────────────────────────────────────────

    @Test
    void createProject_success_savedAndOwnerAddedAsMember() {
        when(projectRepository.existsByProjectKey("CAIROBE")).thenReturn(false);
        when(projectRepository.save(any(Project.class))).thenReturn(project);

        Project result = projectService.createProject(
                "Cairo Backend", "CAIROBE", "desc", owner);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Cairo Backend");
        verify(projectMemberRepository).save(any(ProjectMember.class));
    }

    @Test
    void createProject_duplicateKey_throwsDuplicateResourceException() {
        when(projectRepository.existsByProjectKey("CAIROBE")).thenReturn(true);

        assertThatThrownBy(() ->
                projectService.createProject("Cairo", "CAIROBE", "desc", owner))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("CAIROBE");

        verify(projectRepository, never()).save(any());
    }

//    @Test
//    void createProject_keyIsUppercased() {
//        when(projectRepository.existsByProjectKey("CAIRO")).thenReturn(false);
//        when(projectRepository.save(any(Project.class))).thenAnswer(inv -> {
//            Project p = inv.getArgument(0);
//            assertThat(p.getProjectKey()).isEqualTo("CAIRO");
//            return project;
//        });
//        when(projectMemberRepository.save(any())).thenReturn(null);
//
//        projectService.createProject("Cairo", "cairo", "desc", owner);
//
//        verify(projectRepository).save(argThat(p -> "CAIRO".equals(p.getProjectKey())));
//    }

    // ── updateProject ─────────────────────────────────────────────

    @Test
    void updateProject_byOwner_updatesNameAndDescription() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(projectRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Project result = projectService.updateProject(1L, "New Name", "New Desc", owner);

        assertThat(result.getName()).isEqualTo("New Name");
        assertThat(result.getDescription()).isEqualTo("New Desc");
    }

    @Test
    void updateProject_byAdmin_updatesProject() {
        User admin = User.builder().id(99L).role(User.Role.ADMIN).build();
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(projectRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Project result = projectService.updateProject(1L, "Admin Update", null, admin);

        assertThat(result.getName()).isEqualTo("Admin Update");
    }

    @Test
    void updateProject_byOtherUser_throwsUnauthorizedException() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        assertThatThrownBy(() -> projectService.updateProject(1L, "Hack", "hack", otherUser))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("owner");

        verify(projectRepository, never()).save(any());
    }

    @Test
    void updateProject_nullFields_notUpdated() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(projectRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Project result = projectService.updateProject(1L, null, null, owner);

        // Original values unchanged
        assertThat(result.getName()).isEqualTo("Cairo Backend");
    }

    @Test
    void updateProject_notFound_throwsResourceNotFoundException() {
        when(projectRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.updateProject(99L, "name", "desc", owner))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── getProjectsForUser ────────────────────────────────────────

//    @Test
//    void getProjectsForUser_adminSeesAllProjects() {
//        User admin = User.builder().id(99L).role(User.Role.ADMIN).build();
//        Page<Project> page = new PageImpl<>(List.of(project));
//        when(projectRepository.findAll(any(Pageable.class)))
//                .thenReturn(page);
//
//        Page<Project> result = projectService.getProjectsForUser(99L, Pageable.unpaged());
//
//        assertThat(result).hasSize(1);
//        verify(projectRepository).findAll(any(Pageable.class));
//    }

//    @Test
//    void getProjectsForUser_regularUserSeesOwnProjects() {
//        Page<Project> page = new PageImpl<>(List.of(project));
//        when(projectRepository.findAllByMemberId(eq(1L), any(Pageable.class)))
//                .thenReturn(page);
//
//        Page<Project> result = projectService.getProjectsForUser(1L, Pageable.unpaged());
//
//        assertThat(result).hasSize(1);
//    }

    @Test
    void getProjectsForUser_userNotFound_throwsResourceNotFoundException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.getProjectsForUser(99L, Pageable.unpaged()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── getProjectById ────────────────────────────────────────────

    @Test
    void getProjectById_adminCanSeeAnyProject() {
        User admin = User.builder().id(99L).role(User.Role.ADMIN).build();
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        Project result = projectService.getProjectById(1L, admin);

        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void getProjectById_memberCanSeeProject() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(projectMemberRepository.existsByProjectIdAndUserId(1L, 1L))
                .thenReturn(true);

        Project result = projectService.getProjectById(1L, owner);

        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void getProjectById_nonMemberCannotSeeProject() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(projectMemberRepository.existsByProjectIdAndUserId(1L, 2L))
                .thenReturn(false);

        assertThatThrownBy(() -> projectService.getProjectById(1L, otherUser))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("not a member");
    }

    @Test
    void getProjectById_notFound_throwsResourceNotFoundException() {
        when(projectRepository.findById(99L)).thenReturn(Optional.empty());

        //TODO: FIX ME
        assertThatThrownBy(() -> projectService.getProjectById(99L, new User()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── addMember ─────────────────────────────────────────────────

    @Test
    void addMember_success_memberSaved() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(projectMemberRepository.existsByProjectIdAndUserId(1L, 2L))
                .thenReturn(false);
        when(userRepository.findById(2L)).thenReturn(Optional.of(otherUser));

        projectService.addMember(1L, 2L, owner);

        verify(projectMemberRepository).save(any(ProjectMember.class));
    }

    @Test
    void addMember_notOwner_throwsUnauthorizedException() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        // otherUser is NOT the owner
        assertThatThrownBy(() -> projectService.addMember(1L, 2L, otherUser))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("owner");

        verify(projectMemberRepository, never()).save(any());
    }

    @Test
    void addMember_alreadyMember_throwsBusinessException() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(projectMemberRepository.existsByProjectIdAndUserId(1L, 2L))
                .thenReturn(true);

        assertThatThrownBy(() -> projectService.addMember(1L, 2L, owner))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already a member");
    }

    @Test
    void addMember_userNotFound_throwsResourceNotFoundException() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(projectMemberRepository.existsByProjectIdAndUserId(1L, 99L))
                .thenReturn(false);
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.addMember(1L, 99L, owner))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void addMember_projectNotFound_throwsResourceNotFoundException() {
        when(projectRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.addMember(99L, 2L, owner))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── removeMember ──────────────────────────────────────────────

    @Test
    void removeMember_success_memberDeleted() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(userRepository.findById(2L)).thenReturn(Optional.of(otherUser));

        projectService.removeMember(1L, 2L, owner);

        verify(projectMemberRepository).deleteByProjectIdAndUserId(1L, 2L);
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void removeMember_notOwner_throwsUnauthorizedException() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        assertThatThrownBy(() -> projectService.removeMember(1L, 2L, otherUser))
                .isInstanceOf(UnauthorizedException.class);

        verify(projectMemberRepository, never())
                .deleteByProjectIdAndUserId(anyLong(), anyLong());
    }

    @Test
    void removeMember_cannotRemoveOwner_throwsBusinessException() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        // owner trying to remove themselves
        assertThatThrownBy(() -> projectService.removeMember(1L, owner.getId(), owner))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("owner");
    }

    // ── deleteProject ─────────────────────────────────────────────

    @Test
    void deleteProject_success_projectDeleted() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        projectService.deleteProject(1L, owner);

        verify(projectRepository).delete(project);
    }

    @Test
    void deleteProject_notOwner_throwsUnauthorizedException() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        assertThatThrownBy(() -> projectService.deleteProject(1L, otherUser))
                .isInstanceOf(UnauthorizedException.class);

        verify(projectRepository, never()).delete(any());
    }

    @Test
    void deleteProject_notFound_throwsResourceNotFoundException() {
        when(projectRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.deleteProject(99L, owner))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── getMembers ────────────────────────────────────────────────

    @Test
    void getMembers_returnsListOfProjectMembers() {
        ProjectMember member1 = ProjectMember.builder().projectId(1L).userId(1L).build();
        ProjectMember member2 = ProjectMember.builder().projectId(1L).userId(2L).build();
        
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(projectMemberRepository.findByProjectId(1L))
                .thenReturn(List.of(member1, member2));
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(userRepository.findById(2L)).thenReturn(Optional.of(otherUser));

        List<UserResponse> result = projectService.getMembers(1L);

        assertThat(result).hasSize(2);
    }

    @Test
    void getMembers_projectNotFound_throwsResourceNotFoundException() {
        when(projectRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.getMembers(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getMembers_emptyMemberList() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(projectMemberRepository.findByProjectId(1L))
                .thenReturn(List.of());

        List<UserResponse> result = projectService.getMembers(1L);

        assertThat(result).isEmpty();
    }
}