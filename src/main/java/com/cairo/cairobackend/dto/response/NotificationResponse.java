package com.cairo.cairobackend.dto.response;

import com.cairo.cairobackend.entity.Notification;
import lombok.Builder;
import lombok.Getter;

// Flat response DTO for notifications.
// Hides createdAt and avoids exposing nested User/Issue entities.
@Getter
@Builder
public class NotificationResponse {

    private Long   id;
    private String type;
    private String message;
    private Boolean isRead;

    // Just the issue ID — frontend can fetch full issue if needed.
    // Null if the issue was deleted (ON DELETE SET NULL in DB).
    private Long   issueId;

    // Just the recipient's user ID for reference
    private Long   userId;

    public static NotificationResponse from(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .type(notification.getType().name())
                .message(notification.getMessage())
                .isRead(notification.getIsRead())
                .issueId(notification.getIssue() != null
                        ? notification.getIssue().getId() : null)
                .userId(notification.getUser().getId())
                .build();
    }
}