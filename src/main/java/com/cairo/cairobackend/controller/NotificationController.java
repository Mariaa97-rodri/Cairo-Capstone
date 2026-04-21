package com.cairo.cairobackend.controller;

import com.cairo.cairobackend.dto.response.NotificationResponse;
import com.cairo.cairobackend.entity.User;
import com.cairo.cairobackend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    // GET /api/v1/notifications
    // Returns all unread notifications for the logged-in user
    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getUnread(
            @AuthenticationPrincipal User currentUser) {

        return ResponseEntity.ok(
                notificationService.getUnread(currentUser.getId())
                        .stream()
                        .map(NotificationResponse::from)
                        .collect(Collectors.toList()));
    }

    // GET /api/v1/notifications/count
    // Returns just the unread count — used for the bell badge in the UI
    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> countUnread(
            @AuthenticationPrincipal User currentUser) {

        return ResponseEntity.ok(Map.of(
                "unread",
                notificationService.countUnread(currentUser.getId())));
    }

    // PATCH /api/v1/notifications/read-all
    // Marks all unread notifications as read
    @PatchMapping("/read-all")
    public ResponseEntity<Map<String, String>> markAllRead(
            @AuthenticationPrincipal User currentUser) {

        notificationService.markAllRead(currentUser.getId());
        return ResponseEntity.ok(
                Map.of("message", "All notifications marked as read"));
    }
}