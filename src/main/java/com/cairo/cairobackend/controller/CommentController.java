package com.cairo.cairobackend.controller;

import com.cairo.cairobackend.entity.Comment;
import com.cairo.cairobackend.entity.User;
import com.cairo.cairobackend.service.CommentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    // GET /api/v1/issues/{id}/comments
    @GetMapping("/issues/{issueId}/comments")
    public ResponseEntity<List<Comment>> getComments(
            @PathVariable Long issueId) {

        return ResponseEntity.ok(
                commentService.getComments(issueId));
    }

    // POST /api/v1/issues/{id}/comments
    @PostMapping("/issues/{issueId}/comments")
    public ResponseEntity<Comment> addComment(
            @PathVariable Long issueId,
            @Valid @RequestBody AddCommentRequest request,
            @AuthenticationPrincipal User currentUser) {

        Comment created = commentService.addComment(
                issueId,
                request.getBody(),
                currentUser
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(created);
    }

    // DELETE /api/v1/comments/{id}
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long commentId,
            @AuthenticationPrincipal User currentUser) {

        commentService.deleteComment(commentId, currentUser);
        return ResponseEntity.noContent().build();
    }

    // ── Request DTO ──────────────────────────────────────────

    @Getter @Setter
    public static class AddCommentRequest {

        @NotBlank(message = "Comment body cannot be empty")
        private String body;
    }
}