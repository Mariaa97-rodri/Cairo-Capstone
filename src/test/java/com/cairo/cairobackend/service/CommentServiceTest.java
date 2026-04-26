package com.cairo.cairobackend.service;

import com.cairo.cairobackend.entity.Comment;
import com.cairo.cairobackend.entity.Issue;
import com.cairo.cairobackend.entity.Notification;
import com.cairo.cairobackend.entity.User;
import com.cairo.cairobackend.exception.ResourceNotFoundException;
import com.cairo.cairobackend.exception.UnauthorizedException;
import com.cairo.cairobackend.repository.CommentRepository;
import com.cairo.cairobackend.repository.IssueRepository;
import com.cairo.cairobackend.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock CommentRepository      commentRepository;
    @Mock IssueRepository        issueRepository;
    @Mock NotificationRepository notificationRepository;

    @InjectMocks CommentService commentService;

    private User    author;
    private User    assignee;
    private User    admin;
    private Issue   issue;
    private Comment comment;

    @BeforeEach
    void setUp() {
        author = User.builder()
                .id(1L).name("Maria").email("maria@cairo.com")
                .role(User.Role.USER).build();

        assignee = User.builder()
                .id(2L).name("John").email("john@cairo.com")
                .role(User.Role.USER).build();

        admin = User.builder()
                .id(3L).name("Admin").email("admin@cairo.com")
                .role(User.Role.ADMIN).build();

        issue = Issue.builder()
                .id(1L).title("Fix login bug")
                .assignee(assignee).build();

        comment = Comment.builder()
                .id(1L).body("This is a comment")
                .author(author).issue(issue).build();
    }

    // ── addComment ────────────────────────────────────────────────

    @Test
    void addComment_success_commentSaved() {
        when(issueRepository.findById(1L)).thenReturn(Optional.of(issue));
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);

        Comment result = commentService.addComment(1L, "This is a comment", author);

        assertThat(result.getBody()).isEqualTo("This is a comment");
        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    void addComment_differentAuthorAndAssignee_notifiesAssignee() {
        when(issueRepository.findById(1L)).thenReturn(Optional.of(issue));
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);

        // author (id=1) comments on issue assigned to assignee (id=2)
        commentService.addComment(1L, "Hey", author);

        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void addComment_authorIsAssignee_noNotification() {
        // Make issue assigned to author themselves
        issue.setAssignee(author);
        when(issueRepository.findById(1L)).thenReturn(Optional.of(issue));
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);

        commentService.addComment(1L, "My own issue", author);

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void addComment_noAssignee_noNotification() {
        issue.setAssignee(null);
        when(issueRepository.findById(1L)).thenReturn(Optional.of(issue));
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);

        commentService.addComment(1L, "Comment", author);

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void addComment_issueNotFound_throwsResourceNotFoundException() {
        when(issueRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                commentService.addComment(99L, "body", author))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── getComments ───────────────────────────────────────────────

    @Test
    void getComments_returnsListFromRepository() {
        when(commentRepository.findByIssueIdOrderByCreatedAtAsc(1L))
                .thenReturn(List.of(comment));

        List<Comment> result = commentService.getComments(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getBody()).isEqualTo("This is a comment");
    }

    @Test
    void getComments_emptyList_returnsEmpty() {
        when(commentRepository.findByIssueIdOrderByCreatedAtAsc(1L))
                .thenReturn(List.of());

        List<Comment> result = commentService.getComments(1L);

        assertThat(result).isEmpty();
    }

    @Test
    void getComments_multipleComments_returnsSorted() {
        Comment comment2 = Comment.builder()
                .id(2L).body("Second comment")
                .author(assignee).issue(issue).build();
        
        when(commentRepository.findByIssueIdOrderByCreatedAtAsc(1L))
                .thenReturn(List.of(comment, comment2));

        List<Comment> result = commentService.getComments(1L);

        assertThat(result).hasSize(2);
    }

    // ── deleteComment ─────────────────────────────────────────────

    @Test
    void deleteComment_byAuthor_success() {
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));

        assertThatCode(() -> commentService.deleteComment(1L, author))
                .doesNotThrowAnyException();

        verify(commentRepository).delete(comment);
    }

    @Test
    void deleteComment_byAdmin_success() {
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));

        // admin (id=3) deletes comment authored by author (id=1)
        assertThatCode(() -> commentService.deleteComment(1L, admin))
                .doesNotThrowAnyException();

        verify(commentRepository).delete(comment);
    }

    @Test
    void deleteComment_byOtherUser_throwsUnauthorizedException() {
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));

        // assignee (id=2) is not the author (id=1) and not admin
        assertThatThrownBy(() -> commentService.deleteComment(1L, assignee))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("own comments");

        verify(commentRepository, never()).delete(any());
    }

    @Test
    void deleteComment_notFound_throwsResourceNotFoundException() {
        when(commentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.deleteComment(99L, author))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}