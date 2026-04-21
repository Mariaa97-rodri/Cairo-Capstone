package com.cairo.cairobackend.dto.response;

import com.cairo.cairobackend.entity.Comment;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CommentResponse {

    private Long         id;
    private String       body;
    private Long         issueId;
    private UserResponse author;

    public static CommentResponse from(Comment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .body(comment.getBody())
                .issueId(comment.getIssue().getId())
                .author(UserResponse.from(comment.getAuthor()))
                .build();
    }
}