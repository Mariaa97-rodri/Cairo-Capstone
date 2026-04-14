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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final IssueRepository issueRepository;
    private final NotificationRepository notificationRepository;

    @Transactional
    public Comment addComment(Long issueId, String body, User author) {

        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Issue", issueId));

        Comment comment = Comment.builder()
                .issue(issue)
                .author(author)
                .body(body)
                .build();

        Comment saved = commentRepository.save(comment);

        // Notify the assignee when someone comments on their issue
        if (issue.getAssignee() != null &&
                !issue.getAssignee().getId().equals(author.getId())) {

            Notification notification = Notification.builder()
                    .user(issue.getAssignee())
                    .issue(issue)
                    .type(Notification.NotificationType.COMMENTED)
                    .message(author.getName() + " commented on: "
                            + issue.getTitle())
                    .build();
            notificationRepository.save(notification);
        }

        log.info("Comment added to issue {} by {}", issueId,
                author.getEmail());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Comment> getComments(Long issueId) {
        return commentRepository.findByIssueIdOrderByCreatedAtAsc(issueId);
    }

    @Transactional
    public void deleteComment(Long commentId, User requestingUser) {

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Comment", commentId));

        // Only the author or an admin can delete a comment
        boolean isAuthor = comment.getAuthor().getId()
                .equals(requestingUser.getId());
        boolean isAdmin  = requestingUser.getRole() == User.Role.ADMIN;

        if (!isAuthor && !isAdmin) {
            throw new UnauthorizedException(
                    "You can only delete your own comments.");
        }

        commentRepository.delete(comment);
        log.info("Comment {} deleted", commentId);
    }
}