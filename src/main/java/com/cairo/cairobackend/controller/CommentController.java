package com.cairo.cairobackend.controller;

import com.cairo.cairobackend.dto.request.AddCommentRequest;
import com.cairo.cairobackend.dto.response.CommentResponse;
import com.cairo.cairobackend.entity.User;
import com.cairo.cairobackend.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @GetMapping("/issues/{issueId}/comments")
    public ResponseEntity<List<CommentResponse>> getComments(
            @PathVariable Long issueId) {

        return ResponseEntity.ok(
                commentService.getComments(issueId)
                        .stream()
                        .map(CommentResponse::from)
                        .collect(Collectors.toList()));
    }

    @PostMapping("/issues/{issueId}/comments")
    public ResponseEntity<CommentResponse> addComment(
            @PathVariable Long issueId,
            @Valid @RequestBody AddCommentRequest request,
            @AuthenticationPrincipal User currentUser) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CommentResponse.from(
                        commentService.addComment(
                                issueId,
                                request.getBody(),
                                currentUser)));
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long commentId,
            @AuthenticationPrincipal User currentUser) {

        commentService.deleteComment(commentId, currentUser);
        return ResponseEntity.noContent().build();
    }
}