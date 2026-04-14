package com.cairo.cairobackend.controller;

import com.cairo.cairobackend.dto.request.AddCommentRequest;
import com.cairo.cairobackend.entity.Comment;
import com.cairo.cairobackend.entity.User;
import com.cairo.cairobackend.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

    @GetMapping("/issues/{issueId}/comments")
    public ResponseEntity<List<Comment>> getComments(
            @PathVariable Long issueId) {

        return ResponseEntity.ok(
                commentService.getComments(issueId));
    }

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

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long commentId,
            @AuthenticationPrincipal User currentUser) {

        commentService.deleteComment(commentId, currentUser);
        return ResponseEntity.noContent().build();
    }
}